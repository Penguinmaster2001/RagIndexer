package ragindexer.embeddings

import ragindexer.math.*

case class EmbedRequest(model: String, input: String)
case class EmbedResponse(embeddings: List[Embedding])
case class ResponseChunk(content: String, last: Boolean)

case class OllamaEmbeddingRequest(model: String, input: String)
case class OllamaLlmRequestBody(model: String, prompt: String, stream: Boolean = false)

case class ChunkKey(path: os.Path)
case class CachedChunk(path: String, embedding: String, timestamp: Long)
case class EmbeddedChunk(key: ChunkKey, embedding: Embedding)

case class QueryResult(content: String, chunk: EmbeddedChunk, score: Float)
