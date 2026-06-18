package ragindexer.embeddings.pipeline



import ragindexer.*
import ragindexer.config.*
import ragindexer.embeddings.*



object EmbeddingPipeliner:

    final class PipelineBuilder private[EmbeddingPipeliner] (
        filter: ChunkKey => Boolean,
        private var pending: List[ChunkKey] = Nil
    ) extends EmbeddingPipelineBuilder:
        def queue(key: ChunkKey): this.type = { pending = key :: pending; this }
        def queueAll(keySource: Iterable[ChunkKey]): this.type = { pending = keySource.toList ::: pending; this }
        private[EmbeddingPipeliner] def pendingKeys = pending



    def withPipeline[A](
        config: AppConfig,
        cache: EmbeddingStore,
        embedder: Embedder,
        contentProvider: ContentProvider
    )(
        body: EmbeddingPipelineBuilder => A
    ): A =
        val builder = PipelineBuilder(k => !cache.contains(k))
        val result = body(builder)

        val count = builder.pendingKeys
            .filterNot(k => cache.contains(k))
            .grouped(config.ollama.embedGroupSize)
            .length
        println(s"Embedding $count groups.")

        builder.pendingKeys
            .filterNot(k => cache.contains(k))
            .grouped(config.ollama.embedGroupSize)
            .zipWithIndex
            .foreach((g, i) => {
                println(s"Embedding group ${i + 1}/$count")
                embedder.embed(g.map(k => contentProvider.getContent(k)))
                    .zip(g)
                    .foreach((e, k) => cache.addOrUpdate(k, e))
                    cache.save(AppConfig.getEmbedCachePath(config))
            })

        result
