package ragindexer



import ragindexer.embeddings.*
import ragindexer.ollamaclient.*
import ragindexer.content.*
import ragindexer.math.*



@main def run() =
    val contentProvider = FilesystemContentProvider()
    val ollamaClient = OllamaClient()
    val cache = EmbedRegistry(Some(CACHE_PATH), ollamaClient, contentProvider)
    val fileFilter = FileFilter(SEGMENT_BLACKLIST, EXTENSION_WHITELIST)

    println("Indexing...")
    os.walk(INDEX_ROOT, skip = p => !fileFilter.filter(p))
        .filter(os.isFile)
        .foreach(path =>
            cache.queueForEmbedding(ChunkKey(path))
        )

    cache.cacheQueuedEmbeddings()
    cache.saveCache()
    
    val embeddings = EmbeddedFileSystem(cache, ollamaClient, contentProvider, cosineSim)

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
