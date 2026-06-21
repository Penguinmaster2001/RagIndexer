package ragindexer.embeddings



import ragindexer.math.*
import io.circe.Json



case class EmbedRequest(model: String, input: String)
case class EmbedResponse(embeddings: List[Embedding])
case class ResponseChunk(content: String, last: Boolean)

case class OllamaEmbeddingRequest(model: String, input: String)
case class OllamaGroupEmbeddingRequest(model: String, input: Array[String])



case class OllamaLlmRequestBody(
    model: String,
    prompt: String,
    stream: Boolean = false,
    think: Boolean = false,
    options: Map[String, Int] = Map("num_ctx" -> 8192)
)



case class OllamaLlmStructuredRequestBody(
    model: String,
    prompt: String,
    stream: Boolean = false,
    think: Boolean = false,
    format: Json,
    options: Map[String, Int] = Map("num_ctx" -> 8192)
)



case class ChunkKey(path: os.Path)
case class CachedChunk(path: String, embedding: String, timestamp: Long)
case class EmbeddedChunk(key: ChunkKey, embedding: Embedding)

case class QueryResult(content: String, chunk: EmbeddedChunk, score: Float)
