package ragindexer.content

import ragindexer.embeddings.*
import ragindexer.config.OllamaConfig



class FilesystemContentProvider(config: OllamaConfig) extends ContentProvider:

    def getContent(key: ChunkKey): String =
        os.read(key.path).take((config.embedContextLength * 4 * 0.85).toInt)
