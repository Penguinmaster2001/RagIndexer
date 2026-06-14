package ragindexer.math



import java.nio.ByteBuffer
import java.util.Base64



def embeddingToBase64(embedding: Embedding): String =
    val buf = ByteBuffer.allocate(embedding.length * 4)
    embedding.foreach(buf.putFloat)
    Base64.getEncoder.encodeToString(buf.array())



def base64ToEmbedding(base64String: String): Embedding =
    val buf = ByteBuffer.wrap(Base64.getDecoder.decode(base64String))
    Vector.fill(buf.capacity / 4)(buf.getFloat)



def cosineSim(a: Embedding, b: Embedding): Float =
    val dot = a.zip(b).map(_ * _).sum
    val magA = math.sqrt(a.map(x => x * x).sum).toFloat
    val magB = math.sqrt(b.map(x => x * x).sum).toFloat
    if magA == 0.0 || magB == 0.0 then 0.0 else dot / (magA * magB)
