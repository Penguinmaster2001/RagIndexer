package ragindexer.embeddings



class FileFilter(val segmentBlacklist: Set[String], val extensionWhitelist: Set[String]):
    def filter(path: os.Path): Boolean =
        if os.isDir(path) then !path.segments.exists(s => segmentBlacklist.contains(s))
        else if os.isFile(path) then
            !path.segments.exists(s => segmentBlacklist.contains(s))
            && extensionWhitelist.contains(path.ext.toLowerCase)
        else false
