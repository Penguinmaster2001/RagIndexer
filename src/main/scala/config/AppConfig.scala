package ragindexer.config



import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto.*
import io.circe.parser.*
import io.circe.syntax.*



implicit val encodePath: Encoder[os.Path] = Encoder.encodeString.contramap[os.Path](_.toString)



implicit val decodePath: Decoder[os.Path] = Decoder.decodeString.emap { str =>
    try Right(os.Path(str))
    catch case e: Exception => Left(e.getMessage)
}



implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames



case class CachingConfig(
    cacheRoot: os.Path
)



case class OllamaConfig(
    url: String,
    generationModel: String,
    embedModel: String,
    embedGroupSize: Int
)



case class IndexingConfig(
    indexRoot: os.Path,
    blacklist: Set[String],
    extensionWhitelist: Set[String]
)



case class AppConfig(
    caching: CachingConfig,
    ollama: OllamaConfig,
    indexing: IndexingConfig
)



object AppConfig:

    def load(path: os.Path): Either[io.circe.Error, AppConfig] =
        if os.exists(path) then decode[AppConfig](os.read(path))
        else Left(io.circe.ParsingFailure("Could not find file.", Throwable()))



    def getEmbedCachePath(config: AppConfig): os.Path =
        config.caching.cacheRoot / s"embedding_cache_${config.ollama.embedModel}.json"
