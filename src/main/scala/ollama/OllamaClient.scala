package ragindexer.ollama



import ragindexer.*
import ragindexer.config.*
import ragindexer.math.*
import ragindexer.embeddings.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*



class OllamaClient(config: OllamaConfig) extends Embedder, LlmProvider:

    private def parseChunk(line: String): Option[ResponseChunk] =
        parse(line).toOption.flatMap: json =>
            val cursor = json.hcursor
            for
                token <- cursor.get[String]("response").toOption
                done <- cursor.get[Boolean]("done").toOption
            yield ResponseChunk(token, done)



    def embed(text: String): Embedding =
        val res = requests.post(
          s"${config.url}/api/embed",
          data = OllamaEmbeddingRequest(config.embedModel, text).asJson.noSpaces,
          headers = Map("Content-Type" -> "application/json"),
          readTimeout = 600000
        )
        decode[EmbedResponse](res.text()).toOption
            .flatMap(_.embeddings.headOption)
            .map(_.toVector)
            .getOrElse(Vector.empty)



    def embed(text: Iterable[String]): Iterable[Embedding] =
        val res = requests.post(
          s"${config.url}/api/embed",
          data = OllamaGroupEmbeddingRequest(config.embedModel, Array.from(text)).asJson.noSpaces,
          headers = Map("Content-Type" -> "application/json"),
          readTimeout = 600000
        )
        decode[EmbedResponse](res.text()).toOption
            .flatMap(r => Some(r.embeddings))
            .getOrElse(Iterable.empty)



    def generate(prompt: String)(onChunk: ResponseChunk => Unit): Unit =
        val llmRes = requests
            .post(
              s"${config.url}/api/generate",
              data = OllamaLlmRequestBody(config.generationModel, prompt, true).asJson.noSpaces,
              headers = Map("Content-Type" -> "application/json"),
              readTimeout = 600000
            )
            .readBytesThrough(stream =>
                scala.io.Source
                    .fromInputStream(stream)
                    .getLines()
                    .flatMap(parseChunk)
                    .foreach(onChunk)
            )
