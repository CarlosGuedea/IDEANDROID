package dev.codeforge.ide.ui

import androidx.compose.runtime.Immutable
import dev.codeforge.ide.language.Diagnostic
import dev.codeforge.ide.language.LanguageDefinition
import dev.codeforge.ide.language.LanguageRegistry

/**
 * Archivo abierto en el editor.
 *
 * Todos los campos son `val` a propósito: el ViewModel reemplaza la instancia
 * con `copy(...)` en cada cambio. Con `var` dentro de un `data class` de un
 * `StateFlow`, mutar el objeto en sitio no cambia la igualdad y Compose no
 * recompone, dejando el editor desincronizado.
 */
data class OpenFile(
    val path: String,
    val name: String,
    val language: LanguageDefinition,
    val text: String = "",
    val savedText: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val scroll: Float = 0f,
) {
    val isDirty: Boolean get() = text != savedText
    val lineCount: Int get() = text.count { it == '\n' } + 1
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()
}

enum class BottomPanel { NONE, DIAGNOSTICS, CONSOLE }

data class IdeUiState(
    val tree: dev.codeforge.ide.workspace.FileNode.Dir? = null,
    val expandedDirs: Set<String> = emptySet(),
    val openFiles: List<OpenFile> = emptyList(),
    val activePath: String? = null,
    val selectedPath: String? = null,
    val diagnostics: Map<String, List<Diagnostic>> = emptyMap(),
    val bottomPanel: BottomPanel = BottomPanel.CONSOLE,
    val consoleLines: List<String> = emptyList(),
    val statusMessage: String = "Listo",
    val isBusy: Boolean = false,
    val darkTheme: Boolean = true,
    val explorerVisible: Boolean = true,
) {
    val activeFile: OpenFile? get() = openFiles.firstOrNull { it.path == activePath }
    val errorCount: Int get() = diagnostics.values.sumOf { d -> d.count { it.severity == dev.codeforge.ide.language.Severity.ERROR } }
    val totalDiagnostics: Int get() = diagnostics.values.sumOf { it.size }
}
