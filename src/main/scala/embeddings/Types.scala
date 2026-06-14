package ragindexer.embeddings

case class EmbedRequest(model: String, input: String)
case class EmbedResponse(embeddings: List[List[Float]])
case class ResponseChunk(content: String, last: Boolean)

case class OllamaEmbeddingRequest(model: String, input: String)
case class OllamaLlmRequestBody(model: String, prompt: String, stream: Boolean = false)

case class ChunkKey(path: os.Path)
case class CachedChunk(path: String, embedding: String, timestamp: Long)
case class EmbeddedChunk(key: ChunkKey, embedding: Vector[Float])

case class QueryResult(content: String, chunk: EmbeddedChunk, score: Float)

type Embedding = Vector[Float]
