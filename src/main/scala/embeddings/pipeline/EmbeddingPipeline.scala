package ragindexer.embeddings.pipeline

import ragindexer.*
import ragindexer.embeddings.*



object EmbeddingPipeliner:

    final class PipelineBuilder private[EmbeddingPipeliner] (
        filter: ChunkKey => Boolean,
        private var pending: List[ChunkKey] = Nil
    ) extends EmbeddingPipelineBuilder:
        def queue(key: ChunkKey): this.type = { pending = key :: pending; this }
        def queueAll(keySource: Iterable[ChunkKey]): this.type = { pending = keySource.toList ::: pending; this }
        private[EmbeddingPipeliner] def pendingKeys = pending



    def withPipeline[A](cache: EmbeddingCache, embedder: Embedder, contentProvider: ContentProvider)(
        body: EmbeddingPipelineBuilder => A
    ): A =
        val builder = PipelineBuilder(k => !cache.contains(k))
        val result = body(builder)

        builder.pendingKeys
            .filterNot(k => cache.contains(k))
            .grouped(EMBED_GROUP_SIZE)
            .flatMap(q => embedder.embed(q.map(k => contentProvider.getContent(k))).zip(q))
            .foreach((e, k) => cache.addOrUpdate(k, e))
        result
