package ragindexer.embeddings

import ragindexer.math.*



trait Embedder:
    def embed(text: String): Embedding

    def embed(text: Iterable[String]): Iterable[Embedding]



trait EmbeddingProvider:
    def getEmbeddings(): Iterator[EmbeddedChunk]



trait FileEmbeddingProvider:
    def getEmbedding(key: ChunkKey): Embedding



trait ContentProvider:
    def getContent(key: ChunkKey): String



trait EmbeddedContentProvider:
    def topK(query: String, k: Int): Iterable[QueryResult]
