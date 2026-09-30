package dev.codeforge.ide.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.codeforge.ide.language.Highlighter
import dev.codeforge.ide.language.LanguageDefinition
import dev.codeforge.ide.language.SyntaxPalette
import dev.codeforge.ide.language.Token
import dev.codeforge.ide.language.TokenKind

private const val LINE_HEIGHT = 20

/** Alto aproximado de una línea del gutter, en píxeles, para el scroll. */
private const val GUTTER_LINE_PX = 20

/**
 * Editor de código: `BasicTextField` + `VisualTransformation`.
 *
 * El resaltado se calcula con el tokenizador propio y se aplica como un
 * `AnnotatedString`, por lo que el cursor, la selección y el IME los gestiona
 * el sistema en vez de reimplementarlos.
 */
@Composable
fun CodeEditor(
    file: OpenFile,
    palette: SyntaxPalette,
    darkTheme: Boolean,
    onTextChange: (String, Int, Int) -> Unit,
    onCursorLineChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(file.path, file.text, file.language.id) {
        Highlighter.highlight(file.text, file.language).tokens
    }

    var value by remember(file.path) {
        mutableStateOf(
            TextFieldValue(
                text = file.text,
                selection = TextRange(file.selectionStart.coerceIn(0, file.text.length)),
            )
        )
    }

    // Sincroniza el texto externo (p. ej. al guardar/revertir) con el campo.
    LaunchedEffect(file.text) {
        if (file.text != value.text) {
            val limit = file.text.length
            val sel = TextRange(
                value.selection.start.coerceIn(0, limit),
                value.selection.end.coerceIn(0, limit),
            )
            value = value.copy(text = file.text, selection = sel)
        }
    }

    val cursorLine by remember {
        derivedStateOf { value.text.take(value.selection.start).count { it == '\n' } + 1 }
    }
    LaunchedEffect(cursorLine) { onCursorLineChange(cursorLine) }

    val transformation = remember(tokens, palette) {
        SyntaxTransformation(tokens, palette)
    }

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = LINE_HEIGHT.sp,
        color = colorOf(palette.plain, darkTheme),
    )

    val background = if (darkTheme) Color(0xFF0B0E14) else Color(0xFFFFFFFF)
    val gutterBg = if (darkTheme) Color(0xFF0D1117) else Color(0xFFF6F8FA)
    val gutterFg = if (darkTheme) Color(0xFF4B5563) else Color(0xFF8B949E)

    Row(modifier = modifier.fillMaxSize().background(background)) {
        LineNumberGutter(
            lineCount = file.lineCount,
            activeLine = cursorLine,
            color = gutterFg,
            background = gutterBg,
        )
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val processed = EditorActions.process(value.text, newValue, file.language)
                value = processed
                onTextChange(processed.text, processed.selection.start, processed.selection.end)
            },
            textStyle = textStyle,
            cursorBrush = SolidColor(colorOf(palette.builtin, darkTheme)),
            visualTransformation = transformation,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun LineNumberGutter(
    lineCount: Int,
    activeLine: Int,
    color: Color,
    background: Color,
) {
    // El gutter se desplaza por líneas para mantener visible la línea del cursor.
    // `BasicTextField` gestiona su propio scroll, así que se sincroniza por línea
    // en lugar de por píxeles.
    val scroll = rememberScrollState()
    LaunchedEffect(activeLine, lineCount) {
        val target = ((activeLine - 1).coerceAtLeast(0) * GUTTER_LINE_PX)
        val maxValue = (lineCount * GUTTER_LINE_PX - 1).coerceAtLeast(0)
        scroll.scrollTo(target.coerceIn(0, maxValue))
    }

    Column(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .background(background)
            .clipToBounds()
            .verticalScroll(scroll)
            .padding(vertical = 8.dp),
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                color = if (i == activeLine) color else color.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = LINE_HEIGHT.sp,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp)
                    .then(
                        if (i == activeLine) Modifier.background(color.copy(alpha = 0.10f))
                        else Modifier
                    ),
            )
        }
    }
}

/** Convierte los tokens en spans de color para el `VisualTransformation`. */
private class SyntaxTransformation(
    private val tokens: List<Token>,
    private val palette: SyntaxPalette,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        for (token in tokens) {
            if (token.start < 0 || token.end > text.length || token.length <= 0) continue
            val kind = token.kind
            if (kind == TokenKind.PLAIN || kind == TokenKind.IDENTIFIER) continue
            builder.addStyle(
                SpanStyle(color = Color(palette.of(kind).toULong().toLong())),
                token.start,
                token.end,
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

internal fun colorOf(argb: Long, darkTheme: Boolean): Color = Color(argb.toULong().toLong())
