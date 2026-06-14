package ragindexer.content

import ragindexer.embeddings.*



class FilesystemContentProvider extends ContentProvider:

    def getContent(key: ChunkKey): String =
        os.read(key.path).take(1024)
