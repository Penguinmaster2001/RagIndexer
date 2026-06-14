package ragindexer.embeddings



import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import ragindexer.math.*
import ragindexer.*
import scala.collection.mutable.Queue



object EmbedRegistry:

    type EmbedCache = Map[String, CachedChunk]



    def loadCache(path: os.Path): EmbedCache =
        if os.exists(path) then decode[EmbedCache](os.read(path)).getOrElse(Map.empty)
        else Map.empty



class EmbedRegistry(cachePath: Option[os.Path] = None, embedder: Embedder, contentProvider: ContentProvider)
    extends FileEmbeddingProvider,
      EmbeddingProvider:
    import EmbedRegistry.*

    private var cache = cachePath.fold(Map.empty)(loadCache)
    private var embedQueue = Queue.empty[ChunkKey]



    def cacheQueuedEmbeddings(): Unit =
        embedQueue
            .grouped(EMBED_GROUP_SIZE)
            .map(q =>
                println(s"Group embedding: {$q}")
                q
            )
            .map(q => embedder.embed(q.map(k => contentProvider.getContent(k))).zip(q))
            .flatten()
            .foreach(e => addToCache(e._2, e._1))

        embedQueue = Queue.empty



    def saveCache(): Unit =
        cachePath.foreach: path =>
            os.makeDir.all(path / os.up)
            os.write.over(path, cache.asJson.spaces4)



    def ensureCached(key: ChunkKey): Boolean =
        if containsCached(key) then false
        else
            addToCache(key)
            true



    def queueForEmbedding(key: ChunkKey): Boolean =
        if containsCached(key) then false
        else
            embedQueue.addOne(key)
            true



    private def addToCache(key: ChunkKey): Embedding =
        addToCache(key, embedder.embed(contentProvider.getContent(key)))



    private def addToCache(key: ChunkKey, embedding: Embedding): Embedding =
        cache = cache + (key.path.toString -> CachedChunk(
          path = key.path.toString,
          timestamp = os.mtime(key.path),
          embedding = embeddingToBase64(embedding)
        ))
        embedding



    def getEmbedding(key: ChunkKey): Vector[Float] =
        cache.get(key.path.toString) match
            case Some(chunk) if chunk.timestamp >= os.mtime(key.path) => base64ToEmbedding(chunk.embedding)
            case _                                                    => addToCache(key)



    def containsCached(key: ChunkKey): Boolean =
        cache.get(key.path.toString) match
            case Some(chunk) if chunk.timestamp >= os.mtime(key.path) => true
            case _                                                    => false



    def getEmbeddings(): Iterator[EmbeddedChunk] =
        cache.valuesIterator
            .map(c => EmbeddedChunk(ChunkKey(os.Path(c.path)), base64ToEmbedding(c.embedding)))
