package dev.codeforge.ide.workspace

import androidx.compose.runtime.Immutable

/**
 * Nodo del árbol de archivos. Se construye leyendo directorios reales bajo la
 * carpeta de trabajo de la app.
 */
@Immutable
sealed interface FileNode {
    val name: String
    val path: String

    @Immutable
    data class Dir(
        override val name: String,
        override val path: String,
        val children: List<FileNode>,
        val expanded: Boolean = false,
        val dirCount: Int = children.count { it is Dir },
    ) : FileNode

    @Immutable
    data class File(
        override val name: String,
        override val path: String,
        val sizeBytes: Long = 0L,
        val lastModified: Long = 0L,
    ) : FileNode
}

object FileTreeBuilder {

    private const val MAX_ENTRIES = 4_000
    private const val MAX_DEPTH = 24

    private val SKIP_DIRS = setOf(
        ".git", ".gradle", ".idea", "build", "node_modules", ".svn", ".hg",
        ".kotlin", "captures", ".cxx", "out", ".DS_Store",
    )

    private val BINARY_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "pdf", "zip", "jar", "apk",
        "aab", "so", "a", "o", "class", "dex", "ttf", "otf", "mp3", "mp4", "wav", "avi",
        "mov", "bin", "dat", "db", "sqlite", "wasm", "exe", "dll", "dylib", "keystore",
        "jks", "onnx", "tflite", "mlmodel", "safetensors", "woff", "woff2", "eot",
    )

    fun isProbablyBinary(name: String): Boolean {
        val dot = name.lastIndexOf('.')
        if (dot < 0) return false
        return name.substring(dot + 1).lowercase() in BINARY_EXTENSIONS
    }

    fun isHidden(name: String): Boolean = name.startsWith(".") && name != "." && name != ".."

    /**
     * Construye el árbol desde un [root] real, ordenando directorios primero.
     * Limita el número de entradas para no bloquear el UI en proyectos enormes.
     */
    fun build(root: java.io.File, expanded: Set<String> = emptySet()): FileNode.Dir {
        val counter = intArrayOf(0)
        return buildDir(root, expanded, counter, 0)
    }

    private fun buildDir(
        dir: java.io.File,
        expanded: Set<String>,
        counter: IntArray,
        depth: Int,
    ): FileNode.Dir {
        val children = if (depth >= MAX_DEPTH) {
            emptyList()
        } else {
            val listing = dir.listFiles() ?: return FileNode.Dir(
                dir.name, dir.absolutePath, emptyList(),
            )
            listing.asSequence()
                .filter { !isHidden(it.name) }
                .filter { !(it.isDirectory && it.name in SKIP_DIRS) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .take(MAX_ENTRIES - counter[0])
                .mapNotNull { file ->
                    if (counter[0]++ >= MAX_ENTRIES) return@mapNotNull null
                    if (file.isDirectory) {
                        buildDir(file, expanded, counter, depth + 1)
                    } else {
                        FileNode.File(
                            name = file.name,
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                        )
                    }
                }
                .toList()
        }
        return FileNode.Dir(
            name = dir.name.ifEmpty { dir.absolutePath },
            path = dir.absolutePath,
            children = children,
            expanded = dir.absolutePath in expanded,
            dirCount = children.count { it is FileNode.Dir },
        )
    }
}
