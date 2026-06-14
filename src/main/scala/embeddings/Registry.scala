package ragindexer.embeddings



import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import java.nio.ByteBuffer
import java.util.Base64



def floatsToBase64(v: Vector[Float]): String =
    val buf = ByteBuffer.allocate(v.length * 4)
    v.foreach(buf.putFloat)
    Base64.getEncoder.encodeToString(buf.array())



def base64ToFloats(s: String): Vector[Float] =
    val buf = ByteBuffer.wrap(Base64.getDecoder.decode(s))
    Vector.fill(buf.capacity / 4)(buf.getFloat)



object EmbedRegistry:
    type EmbedCache = Map[String, CachedChunk]

    def loadCache(path: os.Path): EmbedCache =
        if os.exists(path) then decode[EmbedCache](os.read(path)).getOrElse(Map.empty)
        else Map.empty



class EmbedRegistry(cachePath: Option[os.Path] = None, embedder: Embedder, contentProvider: ContentProvider)
    extends FileEmbeddingProvider,
      EmbeddingProvider:
    import EmbedRegistry.*

    var cache = cachePath.fold(Map.empty)(loadCache)

    def saveCache(): Unit =
        cachePath.foreach: path =>
            os.makeDir.all(path / os.up)
            os.write.over(path, cache.asJson.spaces4)

    def ensureCached(key: ChunkKey): Boolean =
        if containsCached(key) then false
        else
            addToCache(key)
            true

    private def addToCache(key: ChunkKey): Vector[Float] =
        val embedding = embedder.embed(contentProvider.getContent(key))
        cache = cache + (key.path.toString -> CachedChunk(
          path = key.path.toString,
          timestamp = os.mtime(key.path),
          embedding = floatsToBase64(embedding)
        ))
        embedding

    def getEmbedding(key: ChunkKey): Vector[Float] =
        cache.get(key.path.toString) match
            case Some(chunk) if chunk.timestamp >= os.mtime(key.path) => base64ToFloats(chunk.embedding)
            case _                                                    => addToCache(key)

    def containsCached(key: ChunkKey): Boolean =
        cache.get(key.path.toString) match
            case Some(chunk) if chunk.timestamp >= os.mtime(key.path) => true
            case _                                                    => false

    def getEmbeddings(): Iterator[EmbeddedChunk] =
        cache.valuesIterator
            .map(c => EmbeddedChunk(ChunkKey(os.Path(c.path)), base64ToFloats(c.embedding)))
