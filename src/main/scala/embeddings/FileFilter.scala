package ragindexer.embeddings

import java.nio.file.{FileSystems, Paths}



class FileFilter(val segmentBlacklist: Set[String], val extensionWhitelist: Set[String], val glob: String):

    private val matcher = FileSystems.getDefault().getPathMatcher(s"glob:$glob")



    def skip(path: os.Path): Boolean =
        path.segments.exists(s => segmentBlacklist.contains(s))
            || !(os.isDir(path) || (os.isFile(path) && extensionWhitelist.contains(path.ext.toLowerCase)))



    def filter(path: os.Path): Boolean =
        val size = os.size(path)
        if size > 200_000 then
            println(s"Skipping large file (${size / 1024}KB): $path")
            false
        else !skip(path) && matcher.matches(Paths.get(path.toURI))
