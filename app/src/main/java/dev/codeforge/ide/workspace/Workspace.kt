package dev.codeforge.ide.workspace

import java.io.File

/**
 * Acceso al sistema de archivos de la app.
 *
 * Usa el directorio externo de la aplicación, que no requiere permisos en Android
 * moderno y sobrevive a reinicios de la app.
 */
class Workspace(private val root: File) {

    val rootPath: String get() = root.absolutePath

    fun ensureReady(): Boolean = root.exists() || root.mkdirs()

    fun rootFile(): File = root

    fun readText(file: File, maxBytes: Long = 8L * 1024 * 1024): Result<String> = runCatching {
        require(file.isFile) { "No es un archivo: ${file.name}" }
        require(file.length() <= maxBytes) {
            "Archivo demasiado grande (${file.length() / 1024} KB)"
        }
        file.readText(Charsets.UTF_8)
    }

    fun writeText(file: File, content: String): Result<Unit> = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    fun createFile(parent: File, name: String): Result<File> = runCatching {
        require(isSafeName(name)) { "Nombre no válido: $name" }
        val target = File(parent, name)
        require(!target.exists()) { "Ya existe: $name" }
        target.parentFile?.mkdirs()
        target.createNewFile()
        target
    }

    fun createDir(parent: File, name: String): Result<File> = runCatching {
        require(isSafeName(name)) { "Nombre no válido: $name" }
        val target = File(parent, name)
        require(!target.exists()) { "Ya existe: $name" }
        target.mkdirs()
        target
    }

    fun rename(file: File, newName: String): Result<File> = runCatching {
        require(isSafeName(newName)) { "Nombre no válido: $newName" }
        val parent = file.parentFile ?: error("El archivo no tiene directorio padre")
        val target = File(parent, newName)
        require(!target.exists()) { "Ya existe: $newName" }
        require(file.renameTo(target)) { "No se pudo renombrar" }
        target
    }

    fun delete(file: File): Result<Unit> = runCatching {
        if (file.isDirectory) file.deleteRecursively() else require(file.delete()) { "No se pudo borrar" }
    }

    /** Ruta relativa al root, para mostrarla en la UI. */
    fun relative(file: File): String =
        file.absolutePath.removePrefix(root.absolutePath).trimStart('/')

    companion object {
        private val ILLEGAL = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

        fun isSafeName(name: String): Boolean =
            name.isNotBlank() && name != "." && name != ".." &&
                name.none { it in ILLEGAL } && name.length <= 255
    }
}
