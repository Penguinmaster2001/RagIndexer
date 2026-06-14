package ragindexer.math

type Embedding = Vector[Float]
type SimilarityMetric = (Embedding, Embedding) => Float
