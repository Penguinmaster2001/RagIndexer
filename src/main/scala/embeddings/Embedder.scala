package ragindexer.embeddings



trait Embedder:
    def embed(text: String): Vector[Float]



trait EmbeddingProvider:
    def getEmbeddings(): Iterator[EmbeddedChunk]



trait FileEmbeddingProvider:
    def getEmbedding(key: ChunkKey): Vector[Float]



trait ContentProvider:
    def getContent(key: ChunkKey): String



trait EmbeddedContentProvider:
    def topK(query: String, k: Int): Iterable[QueryResult]
