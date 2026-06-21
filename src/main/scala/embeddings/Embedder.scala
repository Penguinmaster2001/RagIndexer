package ragindexer.embeddings

import ragindexer.math.*



trait Embedder:
    def embed(text: String): Embedding

    def embed(text: Iterable[String]): Iterable[Embedding]



trait LlmProvider:
    def generate(prompt: String)(onChunk: ResponseChunk => Unit): Unit

    def generateStructured(prompt: String): Either[(io.circe.Error, String), List[String]]



trait EmbeddingProvider:
    def getEmbedding(key: ChunkKey): Option[Embedding]

    def getEmbeddings(): Iterator[EmbeddedChunk]



trait EmbeddingStore:
    var cache: Map[String, CachedChunk]

    def load(path: os.Path): Unit

    def save(path: os.Path): Unit

    def contains(key: ChunkKey): Boolean

    def addOrUpdate(key: ChunkKey, embedding: Embedding): Unit



trait EmbeddingPipelineBuilder:
    def queue(key: ChunkKey): this.type

    def queueAll(keySource: Iterable[ChunkKey]): this.type



trait ContentProvider:
    def getContent(key: ChunkKey): String



trait EmbeddedContentProvider:
    def topK(query: String, k: Int): Iterable[QueryResult]
