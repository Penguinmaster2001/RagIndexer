package ragindexer.embeddings



import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import ragindexer.math.*
import ragindexer.*



class EmbedRegistry(cache: EmbeddingCache) extends EmbeddingProvider:

    def getEmbedding(key: ChunkKey): Option[Embedding] =
        cache.cache.get(key.path.toString).map(c => base64ToEmbedding(c.embedding))



    def getEmbeddings(): Iterator[EmbeddedChunk] =
        cache.cache.valuesIterator
            .map(c => EmbeddedChunk(ChunkKey(os.Path(c.path)), base64ToEmbedding(c.embedding)))
