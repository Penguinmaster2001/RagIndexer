package ragindexer



import ragindexer.embeddings.*
import ragindexer.embeddings.cache.*
import ragindexer.embeddings.pipeline.*
import ragindexer.ollama.*
import ragindexer.content.*
import ragindexer.math.*



@main def run() =
    val contentProvider = FilesystemContentProvider()
    val ollamaClient = OllamaClient()
    val fileFilter = FileFilter(SEGMENT_BLACKLIST, EXTENSION_WHITELIST)

    println("Loading")
    val cache = EmbeddingCache.load(CACHE_PATH)
    println(s"Loaded ${cache.cache.size}")

    println("Searching")
    EmbeddingPipeliner.withPipeline(cache, ollamaClient, contentProvider) { p =>
        p.queueAll(
          os.walk(INDEX_ROOT, skip = p => !fileFilter.filter(p))
              .filter(os.isFile)
              .map(path => ChunkKey(path))
        )
    }

    println("Saving")
    cache.save(CACHE_PATH)

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
