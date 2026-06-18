package ragindexer.embeddings

import java.nio.file.{FileSystems, Paths}



class FileFilter(val segmentBlacklist: Set[String], val extensionWhitelist: Set[String], val glob: String):

    private val matcher = FileSystems.getDefault().getPathMatcher(s"glob:$glob")



    def skip(path: os.Path): Boolean =
        path.segments.exists(s => segmentBlacklist.contains(s))
            || !(os.isDir(path) || (os.isFile(path) && extensionWhitelist.contains(path.ext.toLowerCase)))



    def filter(path: os.Path): Boolean =
        !skip(path) && matcher.matches(Paths.get(path.toURI))
