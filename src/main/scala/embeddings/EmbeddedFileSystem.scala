package ragindexer.embeddings


import ragindexer.embeddings.*



class EmbeddedFileSystem(
    embeddingProvider: EmbeddingProvider,
    embedder: Embedder,
    contentProvider: ContentProvider,
    similarityMetric: (Embedding, Embedding) => Float
) extends EmbeddedContentProvider:
    private val embeddings = embeddingProvider.getEmbeddings().toList

    def topK(query: String, k: Int = 1): Iterable[QueryResult] =
        val embedding = embedder.embed(query)
        embeddings
            .map(e => (e, similarityMetric(e.embedding, embedding)))
            .sortBy(-_._2)
            .take(k)
            .map(t => QueryResult(contentProvider.getContent(t._1.key), t._1, t._2))
