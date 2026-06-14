package ragindexer.embeddings.cache



import ragindexer.embeddings.*
import ragindexer.math.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*



object EmbeddingCache:

    def load(path: os.Path): ragindexer.embeddings.EmbeddingCache =
        val cache = EmbeddingCache()
        cache.load(path)
        cache



class EmbeddingCache extends ragindexer.embeddings.EmbeddingCache:
    var cache = Map.empty[String, CachedChunk]



    def load(path: os.Path): Unit =
        cache =
            if os.exists(path) then decode[Map[String, CachedChunk]](os.read(path)).getOrElse(Map.empty)
            else Map.empty



    def save(path: os.Path): Unit =
        os.makeDir.all(path / os.up)
        os.write.over(path, cache.asJson.spaces4)



    def contains(key: ChunkKey): Boolean =
        cache.get(key.path.toString) match
            case Some(chunk) if chunk.timestamp >= os.mtime(key.path) => true
            case _                                                    => false



    def addOrUpdate(key: ChunkKey, embedding: Embedding): Unit =
        cache = cache + (key.path.toString -> CachedChunk(
          path = key.path.toString,
          timestamp = os.mtime(key.path),
          embedding = embeddingToBase64(embedding)
        ))
