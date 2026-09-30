package dev.codeforge.ide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.codeforge.ide.language.Highlighter
import dev.codeforge.ide.language.LanguageRegistry
import dev.codeforge.ide.workspace.FileNode
import dev.codeforge.ide.workspace.FileTreeBuilder
import dev.codeforge.ide.workspace.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class IdeViewModel(private val workspace: Workspace) : ViewModel() {

    private val _state = MutableStateFlow(IdeUiState())
    val state: StateFlow<IdeUiState> = _state.asStateFlow()

    init {
        workspace.ensureReady()
        log("CodeForge iniciado. Workspace: ${workspace.rootPath}")
        refreshTree()
    }

    // ---------- Árbol de archivos ----------

    fun refreshTree() = viewModelScope.launch {
        val expanded = _state.value.expandedDirs
        val root = workspace.rootFile()
        val tree = withContext(Dispatchers.IO) { FileTreeBuilder.build(root, expanded) }
        _state.update { it.copy(tree = tree, isBusy = false) }
    }

    fun toggleDir(path: String) {
        val current = _state.value.expandedDirs
        val next = if (path in current) current - path else current + path
        _state.update { it.copy(expandedDirs = next) }
        refreshTree()
    }

    fun selectNode(path: String?) {
        _state.update { it.copy(selectedPath = path) }
    }

    /** Activa una pestaña ya abierta sin releer el archivo del disco. */
    fun activateFile(path: String) {
        val file = _state.value.openFiles.firstOrNull { it.path == path } ?: return
        _state.update {
            it.copy(
                activePath = path,
                statusMessage = "${file.name} — ${file.language.displayName}",
            )
        }
    }

    // ---------- Apertura / edición / guardado ----------

    fun openFile(path: String) {
        val file = File(path)
        if (!file.isFile) {
            log("No se puede abrir: ${file.name} (no es un archivo)")
            return
        }
        if (FileTreeBuilder.isProbablyBinary(file.name)) {
            log("Archivo binario, no se abre en el editor: ${file.name}")
            return
        }
        if (_state.value.openFiles.any { it.path == path }) {
            _state.update { it.copy(activePath = path) }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { workspace.readText(file) }
            result.onSuccess { content ->
                val lang = LanguageRegistry.detect(file.name)
                val open = OpenFile(
                    path = path,
                    name = file.name,
                    language = lang,
                    text = content,
                    savedText = content,
                )
                _state.update { st ->
                    st.copy(
                        openFiles = st.openFiles + open,
                        activePath = path,
                        statusMessage = "${file.name} — ${lang.displayName}",
                    )
                }
                rehighlight(path, content, lang)
                log("Abierto ${file.name} (${lang.displayName}, ${content.length} bytes)")
            }.onFailure { err ->
                log("Error al abrir ${file.name}: ${err.message}")
            }
        }
    }

    fun closeFile(path: String) {
        _state.update { st ->
            val remaining = st.openFiles.filterNot { it.path == path }
            val newActive = if (st.activePath == path) remaining.lastOrNull()?.path else st.activePath
            st.copy(
                openFiles = remaining,
                activePath = newActive,
                diagnostics = st.diagnostics - path,
            )
        }
    }

    fun updateText(path: String, text: String, selectionStart: Int = 0, selectionEnd: Int = 0) {
        _state.update { st ->
            st.copy(
                openFiles = st.openFiles.map {
                    if (it.path == path) {
                        it.copy(text = text, selectionStart = selectionStart, selectionEnd = selectionEnd)
                    } else it
                },
            )
        }
    }

    fun updateScroll(path: String, scroll: Float) {
        _state.update { st ->
            st.copy(openFiles = st.openFiles.map { if (it.path == path) it.copy(scroll = scroll) else it })
        }
    }

    fun saveFile(path: String) {
        val open = _state.value.openFiles.firstOrNull { it.path == path } ?: return
        val file = File(path)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { workspace.writeText(file, open.text) }
                .onSuccess {
                    _state.update { st ->
                        st.copy(
                            openFiles = st.openFiles.map {
                                if (it.path == path) it.copy(savedText = it.text) else it
                            },
                            statusMessage = "Guardado ${file.name}",
                        )
                    }
                    log("Guardado ${file.name}")
                    refreshTree()
                }
                .onFailure { log("Error al guardar ${file.name}: ${it.message}") }
        }
    }

    fun saveAll() {
        _state.value.openFiles.filter { it.isDirty }.forEach { saveFile(it.path) }
    }

    fun revertFile(path: String) {
        val open = _state.value.openFiles.firstOrNull { it.path == path } ?: return
        _state.update { st ->
            st.copy(openFiles = st.openFiles.map { if (it.path == path) it.copy(text = it.savedText) else it })
        }
        rehighlight(path, open.savedText, open.language)
    }

    // ---------- Operaciones de archivo ----------

    fun createFile(parentPath: String, name: String) = create(parentPath, name, isDir = false)
    fun createFolder(parentPath: String, name: String) = create(parentPath, name, isDir = true)

    private fun create(parentPath: String, name: String, isDir: Boolean) {
        val parent = File(parentPath)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (isDir) workspace.createDir(parent, name) else workspace.createFile(parent, name)
            }
            result.onSuccess {
                log("${if (isDir) "Carpeta" else "Archivo"} creado: $name")
                if (isDir) {
                    // Asegura que quede visible: expande solo si no lo estaba.
                    if (parentPath !in _state.value.expandedDirs) toggleDir(parentPath)
                    else refreshTree()
                } else refreshTree()
            }.onFailure { log("No se pudo crear $name: ${it.message}") }
        }
    }

    fun renameNode(path: String, newName: String) {
        val file = File(path)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { workspace.rename(file, newName) }
                .onSuccess {
                    log("Renombrado a $newName")
                    closeFile(path)
                    refreshTree()
                }
                .onFailure { log("No se pudo renombrar: ${it.message}") }
        }
    }

    fun deleteNode(path: String) {
        val file = File(path)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { workspace.delete(file) }
                .onSuccess {
                    log("Eliminado ${file.name}")
                    closeFile(path)
                    refreshTree()
                }
                .onFailure { log("No se pudo eliminar: ${it.message}") }
        }
    }

    // ---------- Análisis ----------

    private fun rehighlight(path: String, text: String, lang: dev.codeforge.ide.language.LanguageDefinition) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { Highlighter.highlight(text, lang) }
            _state.update { it.copy(diagnostics = it.diagnostics + (path to result.diagnostics)) }
        }
    }

    fun reanalyze(path: String) {
        val open = _state.value.openFiles.firstOrNull { it.path == path } ?: return
        rehighlight(path, open.text, open.language)
    }

    fun reanalyzeAll() {
        _state.value.openFiles.forEach { reanalyze(it.path) }
    }

    // ---------- UI ----------

    fun setBottomPanel(panel: BottomPanel) {
        _state.update { it.copy(bottomPanel = panel) }
    }

    fun toggleExplorer() {
        _state.update { it.copy(explorerVisible = !it.explorerVisible) }
    }

    fun toggleTheme() {
        _state.update { it.copy(darkTheme = !it.darkTheme) }
    }

    fun setStatus(message: String) {
        _state.update { it.copy(statusMessage = message) }
    }

    fun log(line: String) {
        _state.update {
            it.copy(consoleLines = (it.consoleLines + line).takeLast(500))
        }
    }

    companion object {
        /** Factoría para inyectar el [Workspace] real de la app. */
        fun factory(workspace: Workspace): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    IdeViewModel(workspace) as T
            }
    }
}
