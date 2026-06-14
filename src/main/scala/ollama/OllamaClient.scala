package ragindexer.ollamaclient



import ragindexer.*
import ragindexer.math.*
import ragindexer.embeddings.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*



class OllamaClient() extends Embedder:

    private def parseChunk(line: String): Option[ResponseChunk] =
        parse(line).toOption.flatMap: json =>
            val cursor = json.hcursor
            for
                token <- cursor.get[String]("response").toOption
                done <- cursor.get[Boolean]("done").toOption
            yield ResponseChunk(token, done)



    def embed(text: String): Embedding =
        val res = requests.post(
          s"$OLLAMA/api/embed",
          data = OllamaEmbeddingRequest(EMBED_MODEL, text).asJson.noSpaces,
          headers = Map("Content-Type" -> "application/json")
        )
        decode[EmbedResponse](res.text()).toOption
            .flatMap(_.embeddings.headOption)
            .map(_.toVector)
            .getOrElse(Vector.empty)



    def embed(text: Iterable[String]): Iterable[Embedding] =
        val res = requests.post(
          s"$OLLAMA/api/embed",
          data = OllamaGroupEmbeddingRequest(EMBED_MODEL, Array.from(text)).asJson.noSpaces,
          headers = Map("Content-Type" -> "application/json")
        )
        decode[EmbedResponse](res.text()).toOption
            .flatMap(r => Some(r.embeddings))
            .getOrElse(Iterable.empty)



    def getLlmResponse(prompt: String)(onChunk: ResponseChunk => Unit): Unit =
        println(prompt)
        val llmRes = requests
            .post(
              s"$OLLAMA/api/generate",
              data = OllamaLlmRequestBody(LLM_MODEL, prompt, true).asJson.noSpaces,
              headers = Map("Content-Type" -> "application/json"),
              readTimeout = 60000
            )
            .readBytesThrough(stream =>
                scala.io.Source
                    .fromInputStream(stream)
                    .getLines()
                    .flatMap(parseChunk)
                    .foreach(onChunk)
            )
