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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.codeforge.ide.workspace.FileNode
import dev.codeforge.ide.workspace.FileTreeBuilder

@Composable
fun ExplorerPanel(
    tree: FileNode.Dir?,
    expandedDirs: Set<String>,
    activePath: String?,
    selectedPath: String?,
    background: Color,
    foreground: Color,
    onToggleDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateFile: (String, String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight().background(background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "EXPLORADOR",
                color = foreground.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Refresh, "Refrescar", tint = foreground, modifier = Modifier.size(16.dp))
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (tree == null) {
                Text("Cargando…", color = foreground.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tree.children, key = { it.path }) { node ->
                        FileNodeRow(
                            node = node,
                            depth = 0,
                            expandedDirs = expandedDirs,
                            activePath = activePath,
                            selectedPath = selectedPath,
                            foreground = foreground,
                            onToggleDir = onToggleDir,
                            onOpenFile = onOpenFile,
                            onSelect = onSelect,
                            onCreateFile = onCreateFile,
                            onCreateFolder = onCreateFolder,
                            onDelete = onDelete,
                            onRename = onRename,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileNodeRow(
    node: FileNode,
    depth: Int,
    expandedDirs: Set<String>,
    activePath: String?,
    selectedPath: String?,
    foreground: Color,
    onToggleDir: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onSelect: (String) -> Unit,
    onCreateFile: (String, String) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit,
) {
    val isActive = node.path == activePath
    val isSelected = node.path == selectedPath
    val background = if (isActive) foreground.copy(alpha = 0.16f)
    else if (isSelected) foreground.copy(alpha = 0.08f) else Color.Transparent

    when (node) {
        is FileNode.Dir -> {
            val expanded = node.path in expandedDirs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .clickable {
                        onSelect(node.path)
                        onToggleDir(node.path)
                    }
                    .padding(start = (4 + depth * 12).dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    null,
                    tint = foreground.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Folder, null, tint = Color(0xFF82AAFF), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    node.name,
                    color = foreground,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    TinyAction("+F") { onCreateFile(node.path, "nuevo${System.currentTimeMillis() % 100000}.txt") }
                    TinyAction("+D") { onCreateFolder(node.path, "carpeta${System.currentTimeMillis() % 100000}") }
                    TinyAction("x") { onDelete(node.path) }
                }
            }
            if (expanded) {
                node.children.forEach { child ->
                    FileNodeRow(
                        child, depth + 1, expandedDirs, activePath, selectedPath, foreground,
                        onToggleDir, onOpenFile, onSelect, onCreateFile, onCreateFolder, onDelete, onRename,
                    )
                }
            }
        }

        is FileNode.File -> {
            val binary = FileTreeBuilder.isProbablyBinary(node.name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .clickable {
                        onSelect(node.path)
                        onOpenFile(node.path)
                    }
                    .padding(start = (20 + depth * 12).dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    null,
                    tint = if (binary) foreground.copy(alpha = 0.35f) else Color(0xFF89DDFF),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    node.name,
                    color = if (binary) foreground.copy(alpha = 0.45f) else foreground,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (binary) {
                    Text("bin", color = foreground.copy(alpha = 0.4f), fontSize = 9.sp)
                } else {
                    TinyAction("x") { onDelete(node.path) }
                }
            }
        }
    }
}

@Composable
private fun TinyAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 9.sp,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
