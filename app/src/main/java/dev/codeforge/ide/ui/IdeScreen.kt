package dev.codeforge.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.codeforge.ide.language.Severity
import dev.codeforge.ide.language.SyntaxPalette
import dev.codeforge.ide.workspace.FileNode

@Composable
fun IdeScreen(viewModel: IdeViewModel) {
    val state by viewModel.state.collectAsState()
    val dark = state.darkTheme

    val bg = if (dark) Color(0xFF0B0E14) else Color(0xFFFFFFFF)
    val panelBg = if (dark) Color(0xFF0D1117) else Color(0xFFF6F8FA)
    val accent = if (dark) Color(0xFF4FC3F7) else Color(0xFF0969DA)
    val fg = if (dark) Color(0xFFC9D1D9) else Color(0xFF24292F)
    val palette = if (dark) SyntaxPalette.Dark else SyntaxPalette.Light

    var prompt by remember { mutableStateOf<Prompt?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        ActivityBar(
            dark = dark,
            accent = accent,
            fg = fg,
            explorerVisible = state.explorerVisible,
            panel = state.bottomPanel,
            errorCount = state.errorCount,
            onToggleExplorer = viewModel::toggleExplorer,
            onToggleTheme = viewModel::toggleTheme,
            onPanel = viewModel::setBottomPanel,
            onReanalyze = viewModel::reanalyzeAll,
        )

        Row(modifier = Modifier.weight(1f)) {
            if (state.explorerVisible) {
                ExplorerPanel(
                    tree = state.tree,
                    expandedDirs = state.expandedDirs,
                    activePath = state.activePath,
                    selectedPath = state.selectedPath,
                    background = panelBg,
                    foreground = fg,
                    onToggleDir = viewModel::toggleDir,
                    onOpenFile = viewModel::openFile,
                    onSelect = viewModel::selectNode,
                    onRefresh = viewModel::refreshTree,
                    onCreateFile = { dir, name -> prompt = Prompt.NewFile(dir, name) },
                    onCreateFolder = { dir, name -> prompt = Prompt.NewFolder(dir, name) },
                    onDelete = viewModel::deleteNode,
                    onRename = { path, name -> prompt = Prompt.Rename(path, name) },
                    modifier = Modifier.width(230.dp),
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TabBar(
                    files = state.openFiles,
                    activePath = state.activePath,
                    fg = fg,
                    accent = accent,
                    panelBg = panelBg,
                    onSelect = viewModel::activateFile,
                    onClose = viewModel::closeFile,
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val file = state.activeFile
                    if (file == null) {
                        EmptyState(fg = fg, accent = accent, fileCount = state.openFiles.size)
                    } else {
                        CodeEditor(
                            file = file,
                            palette = palette,
                            darkTheme = dark,
                            onTextChange = { text, selStart, selEnd ->
                                viewModel.updateText(file.path, text, selStart, selEnd)
                            },
                            onCursorLineChange = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (state.bottomPanel != BottomPanel.NONE) {
                    BottomPanelView(
                        state = state,
                        fg = fg,
                        accent = accent,
                        bg = panelBg,
                        onClose = { viewModel.setBottomPanel(BottomPanel.NONE) },
                    )
                }
            }
        }

        StatusBar(state = state, fg = fg, accent = accent, bg = panelBg)
    }

    prompt?.let { p ->
        PromptDialog(
            prompt = p,
            onDismiss = { prompt = null },
            onConfirm = { name ->
                when (p) {
                    is Prompt.NewFile -> viewModel.createFile(p.parentPath, name)
                    is Prompt.NewFolder -> viewModel.createFolder(p.parentPath, name)
                    is Prompt.Rename -> viewModel.renameNode(p.path, name)
                }
                prompt = null
            },
        )
    }
}

sealed interface Prompt {
    data class NewFile(val parentPath: String, val initial: String) : Prompt
    data class NewFolder(val parentPath: String, val initial: String) : Prompt
    data class Rename(val path: String, val initial: String) : Prompt
}

@Composable
private fun PromptDialog(prompt: Prompt, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(prompt) {
        mutableStateOf(
            when (prompt) {
                is Prompt.NewFile -> prompt.initial
                is Prompt.NewFolder -> prompt.initial
                is Prompt.Rename -> prompt.initial
            }
        )
    }
    val title = when (prompt) {
        is Prompt.NewFile -> "Nuevo archivo"
        is Prompt.NewFolder -> "Nueva carpeta"
        is Prompt.Rename -> "Renombrar"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ActivityBar(
    dark: Boolean,
    accent: Color,
    fg: Color,
    explorerVisible: Boolean,
    panel: BottomPanel,
    errorCount: Int,
    onToggleExplorer: () -> Unit,
    onToggleTheme: () -> Unit,
    onPanel: (BottomPanel) -> Unit,
    onReanalyze: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(fg.copy(alpha = 0.06f)).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "CodeForge",
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.width(10.dp))
        BarButton("Explorador", explorerVisible, accent, fg) { onToggleExplorer() }
        BarButton("Problemas", panel == BottomPanel.DIAGNOSTICS, accent, fg) { onPanel(panel.toggle(BottomPanel.DIAGNOSTICS)) }
        BarButton("Consola", panel == BottomPanel.CONSOLE, accent, fg) { onPanel(panel.toggle(BottomPanel.CONSOLE)) }
        BarButton("Analizar", false, accent, fg) { onReanalyze() }
        Spacer(Modifier.weight(1f))
        if (errorCount > 0) {
            Text("$errorCount", color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
        }
        IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DarkMode, "Tema", tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

private fun BottomPanel.toggle(other: BottomPanel): BottomPanel =
    if (this == other) BottomPanel.NONE else other

@Composable
private fun BarButton(label: String, active: Boolean, accent: Color, fg: Color, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        color = if (active) accent else fg.copy(alpha = 0.7f),
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun TabBar(
    files: List<OpenFile>,
    activePath: String?,
    fg: Color,
    accent: Color,
    panelBg: Color,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp).background(panelBg)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        files.forEach { file ->
            val active = file.path == activePath
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .background(if (active) Color.Transparent else panelBg.copy(alpha = 0.6f))
                    .clickable { onSelect(file.path) }
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    file.name,
                    fontSize = 12.sp,
                    color = if (active) accent else fg.copy(alpha = 0.7f),
                    maxLines = 1,
                )
                if (file.isDirty) {
                    Text(" •", color = accent, fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.Close, "Cerrar",
                    tint = fg.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp).clickable { onClose(file.path) },
                )
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(fg: Color, accent: Color, fileCount: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CodeForge", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (fileCount == 0) "Abre un archivo del explorador para empezar"
                else "Selecciona una pestaña para editar",
                color = fg.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun BottomPanelView(
    state: IdeUiState,
    fg: Color,
    accent: Color,
    bg: Color,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().height(170.dp).background(bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (state.bottomPanel == BottomPanel.DIAGNOSTICS) "PROBLEMAS" else "CONSOLA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
            )
            Text("x", fontSize = 11.sp, color = fg, modifier = Modifier.clickable(onClick = onClose).padding(4.dp))
        }
        if (state.bottomPanel == BottomPanel.DIAGNOSTICS) {
            val entries = state.diagnostics.filterValues { it.isNotEmpty() }
            if (entries.isEmpty()) {
                Text("Sin problemas detectados", color = fg.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    entries.forEach { (path, diags) ->
                        items(diags) { d ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
                                Text(
                                    path.substringAfterLast('/') + ":" + (d.line + 1),
                                    color = accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(d.message, color = fg, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                items(state.consoleLines) { line ->
                    Text(line, color = fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun StatusBar(state: IdeUiState, fg: Color, accent: Color, bg: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().height(26.dp).background(accent.copy(alpha = 0.15f)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(state.statusMessage, color = fg, fontSize = 11.sp, modifier = Modifier.weight(1f))
        state.activeFile?.let { file ->
            Text(
                "${file.language.displayName}  ·  ${file.lineCount} líneas",
                color = fg.copy(alpha = 0.8f),
                fontSize = 11.sp,
            )
        }
    }
}
