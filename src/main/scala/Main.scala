package ragindexer



import ragindexer.embeddings.*
import ragindexer.ollamaclient.*
import ragindexer.content.FilesystemContentProvider



def cosineSim(a: Vector[Float], b: Vector[Float]): Float =
    val dot = a.zip(b).map(_ * _).sum
    val magA = math.sqrt(a.map(x => x * x).sum).toFloat
    val magB = math.sqrt(b.map(x => x * x).sum).toFloat
    if magA == 0.0 || magB == 0.0 then 0.0 else dot / (magA * magB)



@main def run() =
    /*
        Load embedding cache
        Walk the filesystem filtered
        for each file:
        if it does not have an up to date cache:
            add path to embedder.toLoad
        
        then
        embedder.embedAll:
        load files
        split into chunks
        once >N chunks gathered or out of files:
            batch embed
            append to embeddings list
        return embeddings
        
        then pass the embeddings to the embedding registry, have it save them to file
     */

    val contentProvider = FilesystemContentProvider()
    val ollamaClient = OllamaClient()
    val cache = EmbedRegistry(Some(CACHE_PATH), ollamaClient, contentProvider)
    val fileFilter = FileFilter(SEGMENT_BLACKLIST, EXTENSION_WHITELIST)

    println("Indexing...")
    os.walk(INDEX_ROOT, skip = p => !fileFilter.filter(p))
        .filter(os.isFile)
        .foreach(path =>
            println(s"Embedding: $path")
            cache.ensureCached(ChunkKey(path))
        )

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
