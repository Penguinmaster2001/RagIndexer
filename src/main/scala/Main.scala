package ragindexer



import ragindexer.embeddings.*
import ragindexer.embeddings.cache.*
import ragindexer.embeddings.pipeline.*
import ragindexer.config.*
import ragindexer.ollama.*
import ragindexer.content.*
import ragindexer.math.*



@main def run() =
    val config = AppConfig.load(os.home / ".config" / "RagIndexer" / "config.json") match
        case Right(c) => c
        case Left(e)  => throw RuntimeException(e.getLocalizedMessage(), e.getCause())

    val contentProvider = FilesystemContentProvider(config.ollama)
    val ollamaClient = OllamaClient(config.ollama)
    val fileFilter = FileFilter(config.indexing.blacklist, config.indexing.extensionWhitelist)

    println("Loading")
    val cache = EmbeddingCache.load(AppConfig.getEmbedCachePath(config))
    println(s"Loaded ${cache.cache.size}")

    println("Searching")
    EmbeddingPipeliner.withPipeline(config.ollama, cache, ollamaClient, contentProvider) { p =>
        p.queueAll(
          os.walk(config.indexing.indexRoot, skip = p => !fileFilter.filter(p))
              .filter(os.isFile)
              .map(path => ChunkKey(path))
        )
    }

    println("Saving")
    cache.save(AppConfig.getEmbedCachePath(config))

    val registry = EmbedRegistry(cache)
    val embeddings = EmbeddedFileSystem(registry, ollamaClient, contentProvider, cosineSim)

    println(s"Enter a query:")

    Iterator
        .continually(scala.io.StdIn.readLine())
        .takeWhile(_ != "quit")
        .foreach: query =>
            val topK = embeddings.topK(query, 3)

            val context = topK
                .map(r => s"[${r.chunk.key.path.last} (score: ${"%.3f".format(r.score)})]\n${r.content}")
                .mkString("\n\n---\n\n")

            val prompt = s"""Answer based on these documents:\n\n$context\n\nQuestion: $query"""
            ollamaClient.getLlmResponse(prompt): chunk =>
                print(chunk.content)
                Console.flush()

            println("\nEnter a question:")
