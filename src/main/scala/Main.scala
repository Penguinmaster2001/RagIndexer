package ragindexer



import ragindexer.embeddings.*
import ragindexer.ollamaclient.*



def cosineSim(a: Vector[Float], b: Vector[Float]): Float =
    val dot = a.zip(b).map(_ * _).sum
    val magA = math.sqrt(a.map(x => x * x).sum).toFloat
    val magB = math.sqrt(b.map(x => x * x).sum).toFloat
    if magA == 0.0 || magB == 0.0 then 0.0 else dot / (magA * magB)



def extractText(path: os.Path): Option[String] =
    val ext = path.ext.toLowerCase
    Option.when(Set("md", "txt", "scala", "py", "cs", "rs", "json").contains(ext)):
        os.read(path)



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

    val ollamaClient = OllamaClient()
    val cache = EmbedRegistry(Some(CACHE_PATH), ollamaClient)

    val root =
        sys.env.get("HOME").flatMap(h => Some(os.Path(h))).getOrElse(os.home) / "Documents" / "Job" / "ApplicationDocs"

    println("Indexing...")
    val index: Vector[(os.Path, String, Vector[Float])] =
        os.walk(root)
            .filter(p =>
                !p.segments.exists(s =>
                    Set(
                      "build",
                      "obj",
                      "bin",
                      "node_modules",
                      ".git",
                      ".godot",
                      ".vscode",
                      ".zed",
                      ".github",
                      ".gradle",
                      "android",
                      "Library",
                      "Temp",
                      ".venv"
                    ).contains(s)
                )
            )
            .flatMap(path => extractText(path).map(text => (path, text)))
            .map((path, text) =>
                val snippet = text.take(1024)
                println(s"  Embedding: $path")
                (path, text, cache.embedCached(snippet, path))
            )
            .toVector

    cache.saveCache()

    println(s"Indexed ${index.size} files. Enter a query:")

    Iterator
        .continually(scala.io.StdIn.readLine())
        .takeWhile(_ != "quit")
        .foreach: query =>
            val qVec = ollamaClient.embed(query)
            val topK = index
                .map((path, text, vec) => (path, text, cosineSim(qVec, vec)))
                .sortBy(-_._3)
                .take(3)

            val context = topK
                .map((path, text, score) => s"[${path.last} (score: ${"%.3f".format(score)})]\n${text.take(1024)}")
                .mkString("\n\n---\n\n")

            val prompt = s"""Answer based on these documents:\n\n$context\n\nQuestion: $query"""
            ollamaClient.getLlmResponse(prompt): chunk =>
                print(chunk.content)
                Console.flush()

            println("\nEnter a question:")
