package ragindexer

val CACHE_PATH = os.home / ".cache" / "rag-indexer" / "embeddings.json"



val INDEX_ROOT =
    sys.env.get("HOME").flatMap(h => Some(os.Path(h))).getOrElse(os.home) / "Documents" / "Job" / "ApplicationDocs"



val OLLAMA = "http://localhost:11434"
val EMBED_MODEL = "nomic-embed-text-v2-moe"
val LLM_MODEL = "llama3.2:3b"

val EMBED_GROUP_SIZE = 5



val SEGMENT_BLACKLIST = Set(
  "build",
  "obj",
  "bin",
  "node_modules",
  ".git",
  ".godot",
  ".vscode",
  ".zed",
  ".github",
  ".gradle",
  "android",
  "Library",
  "Temp",
  ".venv"
)



val EXTENSION_WHITELIST = Set(
  "md",
  "txt",
  "scala",
  "py",
  "cs",
  "rs",
  "json"
)
