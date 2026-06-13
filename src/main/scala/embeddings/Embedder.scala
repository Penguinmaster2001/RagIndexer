package ragindexer.embeddings



trait Embedder:
    def embed(text: String): Vector[Float]
