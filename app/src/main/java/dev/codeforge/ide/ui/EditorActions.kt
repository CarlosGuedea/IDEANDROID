package dev.codeforge.ide.ui

import dev.codeforge.ide.language.IndentStyle
import dev.codeforge.ide.language.LanguageDefinition
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Transformaciones de edición independientes de la UI: cierre automático de
 * delimitadores, indentación al pulsar Enter y alternancia de comentarios.
 *
 * Son funciones puras sobre el valor del campo para poder testearlas en JVM.
 */
object EditorActions {

    private val CLOSERS = mapOf('(' to ')', '[' to ']', '{' to '}', '<' to '>')
    private val QUOTES = setOf('"', '\'', '`')

    private val INDENT_OPENERS = setOf('{', '(', '[', ':')
    private const val BLANK = " "

    /**
     * Aplica las transformaciones de edición comparando [previousText] con el
     * contenido actual de [value].
     *
     * [previousText] es imprescindible: sin el texto previo no se puede saber si
     * el usuario acaba de teclear un carácter, pegó un bloque o deshizo.
     */
    fun process(
        previousText: String,
        value: TextFieldValue,
        lang: LanguageDefinition,
    ): TextFieldValue {
        val now = value.text
        // Solo actuamos sobre inserciones de un único carácter.
        if (now.length != previousText.length + 1) return value

        val cursor = value.selection.start
        if (!value.selection.collapsed || cursor <= 0 || cursor > now.length) return value
        val inserted = now[cursor - 1]

        // Enter → salto con indentación heredada.
        if (inserted == '\n') {
            return handleEnter(value, lang, cursor)
        }

        // Cierre automático de delimitadores.
        val closer = CLOSERS[inserted]
        if (closer != null) {
            val next = now.getOrNull(cursor)
            // Solo se inserta si no hay nada detrás. Si ya viene el cierre
            // (`( | )`), no duplicamos: simplemente no se toca nada.
            if (next == null || next.isWhitespaceCharForEdits()) {
                val newText = now.substring(0, cursor) + closer + now.substring(cursor)
                return value.copy(text = newText, selection = TextRange(cursor))
            }
        }

        // Cierre automático de comillas.
        if (inserted in QUOTES) {
            val langQuotes = lang.strings.any { it.open.startsWith(inserted.toString()) }
            if (langQuotes) {
                val next = now.getOrNull(cursor)
                if (next == null || next.isWhitespaceCharForEdits()) {
                    val newText = now.substring(0, cursor) + inserted + now.substring(cursor)
                    return value.copy(text = newText, selection = TextRange(cursor))
                }
            }
        }

        return value
    }

    private fun Char.isWhitespaceCharForEdits(): Boolean = this == ' ' || this == '\t'

    /**
     * Enter: el salto de línea ya lo insertó el teclado, así que aquí solo se
     * añade la indentación correspondiente al final del cursor.
     */
    private fun handleEnter(value: TextFieldValue, lang: LanguageDefinition, cursor: Int): TextFieldValue {
        val text = value.text
        val lineStart = text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
        val currentLine = text.substring(lineStart, cursor - 1)
        val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
        val trimmed = currentLine.trimEnd()
        val opensBlock = trimmed.isNotEmpty() && trimmed.last() in INDENT_OPENERS
        val closesBlock = trimmed.endsWith("}") || trimmed.endsWith(")")

        val newIndent = when {
            // Python no tiene llaves: se replica la indentación tal cual.
            lang.indentStyle == IndentStyle.PYTHON_LIKE -> indent
            // La línea ya cerraba el bloque: no se abre otro nivel.
            closesBlock -> indent
            opensBlock -> indent + lang.indentUnit
            else -> indent
        }

        val newText = text.substring(0, cursor) + newIndent + text.substring(cursor)
        return value.copy(text = newText, selection = TextRange(cursor + newIndent.length))
    }

    /**
     * Alterna el comentario de línea en la selección.
     * Si todo lo seleccionado ya está comentado, lo desconecta.
     */
    fun toggleComment(value: TextFieldValue, lang: LanguageDefinition): TextFieldValue {
        val token = lang.lineCommentToken
        if (lang.lineComments.isEmpty()) return value

        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        val blockStart = text.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
        val blockEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }

        val lines = text.substring(blockStart, blockEnd).split("\n")
        val allCommented = lines.isNotEmpty() && lines.all {
            it.isBlank() || it.trimStart().startsWith(token)
        }

        val newLines = if (allCommented) {
            lines.map { line ->
                val idx = line.indexOf(token)
                if (idx < 0) line else line.removeRange(idx, idx + token.length)
            }
        } else {
            lines.map { line ->
                if (line.isBlank()) line else "$token$BLANK$line"
            }
        }
        val newText = text.substring(0, blockStart) + newLines.joinToString("\n") + text.substring(blockEnd)
        val delta = newText.length - text.length
        return value.copy(
            text = newText,
            selection = TextRange(start, (end + delta).coerceAtLeast(start)),
        )
    }
}
