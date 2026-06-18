package ragindexer



import ragindexer.embeddings.*
import ragindexer.embeddings.cache.*
import ragindexer.embeddings.pipeline.*
import ragindexer.config.*
import ragindexer.ollama.*
import ragindexer.content.*
import ragindexer.math.*
import scala.util.CommandLineParser



enum RunMode:
    case CacheOnly, ListFiles, Full



given CommandLineParser.FromString[RunMode] with

    def fromString(value: String): RunMode =
        try RunMode.valueOf(value)
        catch case _: Throwable => RunMode.Full



def walkFiles(config: IndexingConfig, fileFilter: FileFilter): Iterable[os.Path] =
    os.walk(config.indexRoot, skip = p => fileFilter.skip(p))
        .filter(os.isFile)
        .filter(fileFilter.filter)



def listFiles(config: AppConfig, fileFilter: FileFilter): Unit =
    var count = 0
    walkFiles(config.indexing, fileFilter).foreach(p => {
        println(s"Path: $p")
        count += 1
    })
    println(s"Num files: $count")



def loadCache(path: os.Path): EmbeddingStore =
    println("Loading.")
    val cache = EmbeddingCache.load(path)
    println(s"Loaded ${cache.cache.size} cached files.")
    cache



def indexAndSearch(
    config: AppConfig,
    cache: EmbeddingStore,
    embedder: Embedder,
    contentProvider: ContentProvider,
    fileFilter: FileFilter
): Unit =
    println("Searching")
    EmbeddingPipeliner.withPipeline(config, cache, embedder, contentProvider) { p =>
        p.queueAll(
          walkFiles(config.indexing, fileFilter)
              .map(path => ChunkKey(path))
        )
    }

    println("Saving")
    cache.save(AppConfig.getEmbedCachePath(config))



def buildEmbeddings(
    cache: EmbeddingStore,
    embedder: Embedder,
    contentProvider: ContentProvider
): EmbeddedContentProvider =
    val registry = EmbedRegistry(cache)
    EmbeddedFileSystem(registry, embedder, contentProvider, cosineSim)



def run(config: AppConfig, embeddings: EmbeddedContentProvider, llm: LlmProvider): Unit =

    println(s"Enter a question:")

    Iterator
        .continually(scala.io.StdIn.readLine())
        .takeWhile(_ != "quit")
        .foreach: query =>
            val topK = embeddings.topK(query, config.ollama.queryResultCount)

            topK.foreach(r => println(r.chunk.key))

            val MaxContextChars = 6000

            val context = topK
                .scanLeft(("", 0)) { case ((_, total), r) =>
                    val content = s"[${r.chunk.key.path.last}]\n${r.content}"
                    (content, total + content.length)
                }
                .drop(1)
                .takeWhile(_._2 <= MaxContextChars)
                .map(_._1)
                .mkString("\n\n---\n\n")

            val prompt = s"""You are a personal assistant with access to the user's documents.
Answer the question using ONLY the provided documents. Be specific and cite which document each fact comes from.
If the documents don't contain enough information to answer, say so clearly.

DOCUMENTS:
$context

QUESTION: $query

ANSWER:"""
            llm.generate(prompt): chunk =>
                print(chunk.content)
                Console.flush()

            println("\nEnter a question:")



@main def main(runMode: RunMode = RunMode.Full): Unit =

    val config = AppConfig.load(os.home / ".config" / "RagIndexer" / "config.json") match
        case Right(c) => c
        case Left(e)  => throw RuntimeException(e.getLocalizedMessage(), e.getCause())
    val fileFilter = FileFilter(config.indexing.blacklist, config.indexing.extensionWhitelist, config.indexing.glob)

    runMode match
        case RunMode.ListFiles                         => listFiles(config, fileFilter)
        case mode @ (RunMode.CacheOnly | RunMode.Full) =>
            val contentProvider = FilesystemContentProvider(config.ollama)
            val ollamaClient = OllamaClient(config.ollama)
            val cache = loadCache(AppConfig.getEmbedCachePath(config))
            if mode == RunMode.Full then indexAndSearch(config, cache, ollamaClient, contentProvider, fileFilter)
            val embeddings = buildEmbeddings(cache, ollamaClient, contentProvider)
            run(config, embeddings, ollamaClient)
