package dev.codeforge.ide.language

enum class Severity { ERROR, WARNING, INFO }

data class Diagnostic(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: Severity = Severity.ERROR,
)

data class Token(val start: Int, val length: Int, val kind: TokenKind) {
    val end: Int get() = start + length
}

data class HighlightResult(
    val tokens: List<Token>,
    val diagnostics: List<Diagnostic>,
)

/**
 * Tokenizador declarativo dirigido por [LanguageDefinition].
 *
 * Es un autómata de una sola pasada que no usa expresiones regulares: los literales,
 * comentarios y plantillas se resuelven con la pila de contexto, de modo que el
 * balance de delimitadores y los diagnósticos salen gratis de la misma recorrido.
 */
object Highlighter {

    private const val MAX_TEXT_FOR_TOKENS = 2_000_000

    fun highlight(
        text: CharSequence,
        lang: LanguageDefinition,
        maxTextLength: Int = MAX_TEXT_FOR_TOKENS,
    ): HighlightResult {
        if (text.length > maxTextLength) {
            return HighlightResult(emptyList(), emptyList())
        }
        val scanner = Scanner(text, lang)
        scanner.run()
        return HighlightResult(scanner.tokens, scanner.diagnostics)
    }

    private class Scanner(
        private val text: CharSequence,
        private val lang: LanguageDefinition,
    ) {
        val tokens = ArrayList<Token>(text.length / 4 + 16)
        val diagnostics = ArrayList<Diagnostic>()

        private var pos = 0
        private val n = text.length
        private var bodyStart = 0

        /** Pila de literales abiertos; permite anidar cadenas (`"a ${ "b" } c"`). */
        private val strings = ArrayDeque<StringSpec>()
        private val brackets = ArrayDeque<BracketEntry>()
        private var lastSignificant = ' '
        private var lineStart = 0
        private var line = 0

        private data class BracketEntry(val char: Char, val offset: Int)

        fun run() {
            while (pos < n) {
                val c = text[pos]
                when {
                    c == '\n' -> { line++; pos++; lineStart = pos; lastSignificant = '\n' }
                    c.isWhitespaceChar() -> pos++
                    else -> {
                        val before = pos
                        if (!step(c) || pos <= before) pos = before + 1
                    }
                }
            }
            finish()
        }

        /** Procesa el carácter en [pos]. Devuelve false si no se consumirió nada. */
        private fun step(c: Char): Boolean {
            // Dentro de un literal: solo tienen sentido escapes, plantillas y el cierre.
            if (strings.isNotEmpty()) return stepInsideString(c)

            if (matchBlockComment()) return true
            if (matchLineComment()) return true
            if (matchString()) return true
            if (c == '@' && pos + 1 < n && isIdentStart(text[pos + 1])) return scanAnnotation()
            if (c.isDigitChar() || (c == '.' && pos + 1 < n && text[pos + 1].isDigitChar()
                        && !lastSignificant.isDigitChar())) {
                return scanNumber()
            }
            if (c == '`') {
                if (matchStringFrom("`")) return true
            }
            if (isIdentStart(c)) return scanIdentifier()
            if (lang.brackets.any { it.open == c }) {
                brackets.addLast(BracketEntry(c, pos))
                emit(pos, 1, TokenKind.PUNCTUATION)
                lastSignificant = c
                pos++
                return true
            }
            if (lang.brackets.any { it.close == c }) return scanClosingBracket(c)
            if (c.isOperatorChar()) {
                emit(pos, 1, TokenKind.OPERATOR)
                lastSignificant = c
                pos++
                return true
            }
            emit(pos, 1, TokenKind.PUNCTUATION)
            lastSignificant = c
            pos++
            return true
        }

        private fun stepInsideString(c: Char): Boolean {
            val spec = strings.last()
            if (c == '\n') {
                if (!spec.multiline) {
                    // Cadena sin cerrar antes del salto de línea.
                    flushBody()
                    strings.removeLast()
                    report(pos, "Cadena sin terminar", Severity.ERROR)
                    return false
                }
                line++; pos++; lineStart = pos
                return true
            }
            if (spec.escapes && c == '\\' && pos + 1 < n) {
                pos += 2
                return true
            }
            val interpolator = spec.interpolator
            if (interpolator != null && c == interpolator) return scanTemplate()
            val close = spec.close
            if (close.isNotEmpty() && text.startsWith(close, pos)) {
                flushBody()
                emit(pos, close.length, TokenKind.STRING)
                pos += close.length
                strings.removeLast()
                return true
            }
            pos++
            return true
        }

        /** Cierra el tramo pendiente del literal para que el cuerpo quede coloreado. */
        private fun flushBody() {
            if (pos > bodyStart) emit(bodyStart, pos - bodyStart, TokenKind.STRING)
        }

        /** `${expr}` o `$ident` dentro de un literal: se colorea aparte del cuerpo. */
        private fun scanTemplate(): Boolean {
            flushBody()
            if (pos + 1 < n && text[pos + 1] == '{') {
                var depth = 0
                var i = pos
                while (i < n) {
                    when (text[i]) {
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) { i++; break }
                        }
                    }
                    i++
                }
                emit(pos, i - pos, TokenKind.TEMPLATE)
                pos = i
            } else {
                var i = pos + 1
                while (i < n && isIdentPart(text[i])) i++
                emit(pos, i - pos, TokenKind.TEMPLATE)
                pos = i
            }
            bodyStart = pos
            return true
        }

        private fun matchLineComment(): Boolean {
            for (token in lang.lineComments) {
                if (token.isNotEmpty() && text.startsWith(token, pos)) {
                    var end = pos
                    while (end < n && text[end] != '\n') end++
                    emit(pos, end - pos, commentKind(pos, end))
                    pos = end
                    return true
                }
            }
            return false
        }

        private fun matchBlockComment(): Boolean {
            // El delimitador más largo gana: `/**` debe vencer a `/*`.
            for (pair in lang.blockComments.sortedByDescending { it.open.length }) {
                if (pair.open.isEmpty()) continue
                if (!text.startsWith(pair.open, pos)) continue
                val end = if (pair.close.isEmpty()) n else {
                    val idx = text.indexOfFrom(pair.close, pos + pair.open.length)
                    if (idx < 0) n else idx + pair.close.length
                }
                emit(pos, end - pos, docKindAt(pos))
                countNewlines(pos, end)
                pos = end
                return true
            }
            return false
        }

        /** Clasifica un comentario mirando el texto real, no el par del lenguaje. */
        private fun docKindAt(at: Int): TokenKind {
            if (text.startsWith("/**", at)) return TokenKind.DOC_COMMENT
            if (text.startsWith("///", at)) return TokenKind.DOC_COMMENT
            if (text.startsWith("<!--", at)) return TokenKind.DOC_COMMENT
            if (text.startsWith("=begin", at)) return TokenKind.DOC_COMMENT
            return TokenKind.COMMENT
        }

        private fun matchString(): Boolean {
            for (open in lang.stringOpens) {
                if (matchStringFrom(open)) return true
            }
            return false
        }

        private fun matchStringFrom(open: String): Boolean {
            if (open.isEmpty() || !text.startsWith(open, pos)) return false
            val spec = lang.strings.firstOrNull { it.open == open } ?: return false
            strings.addLast(spec)
            emit(pos, open.length, TokenKind.STRING)
            pos += open.length
            bodyStart = pos
            return true
        }

        private fun scanAnnotation(): Boolean {
            var i = pos + 1
            while (i < n && (isIdentPart(text[i]) || text[i] == '.')) i++
            emit(pos, i - pos, TokenKind.ANNOTATION)
            pos = i
            lastSignificant = '@'
            return true
        }

        private fun scanNumber(): Boolean {
            var i = pos
            if (text[i] == '0' && pos + 1 < n && text[pos + 1] in "xXbBoO") i += 2
            var seenDot = false
            while (i < n) {
                val ch = text[i]
                when {
                    ch.isDigitChar() || ch == '_' -> i++
                    // El punto solo es decimal si NO precedimos otro punto: en `1..10`
                    // el segundo punto es el operador de rango, no parte del número.
                    ch == '.' && !seenDot && i + 1 < n && text[i + 1].isDigitChar() &&
                        (i == 0 || !text[i - 1].isRangeDot()) -> { seenDot = true; i++ }
                    (ch == 'e' || ch == 'E') && i + 1 < n &&
                        (text[i + 1].isDigitChar() || text[i + 1] in "+-") -> i += 2
                    else -> break
                }
            }
            while (i < n && isIdentPart(text[i])) i++ // sufijos: L, f, n, uL
            emit(pos, i - pos, TokenKind.NUMBER)
            pos = i
            lastSignificant = '0'
            return true
        }

        private fun scanIdentifier(): Boolean {
            var i = pos
            while (i < n && isIdentPart(text[i])) i++
            val word = text.subSequence(pos, i).toString()
            val kind = classify(word, i)
            emit(pos, i - pos, kind)
            pos = i
            if (kind != TokenKind.IDENTIFIER && kind != TokenKind.PROPERTY) {
                lastSignificant = word.last()
            }
            return true
        }

        private fun classify(word: String, after: Int): TokenKind {
            val lookup = if (lang.caseInsensitiveKeywords) word.lowercase() else word
            when {
                lang.controlKeywords.contains(lookup) -> return TokenKind.CONTROL_KEYWORD
                lang.keywords.contains(lookup) -> return TokenKind.KEYWORD
                lang.types.contains(lookup) -> return TokenKind.TYPE
                lang.constants.contains(lookup) -> return TokenKind.CONSTANT
                lang.builtins.contains(lookup) -> return TokenKind.BUILTIN
            }
            if (word.first().isUpperCaseChar() && word.length > 1 && word.all { isIdentPart(it) }) {
                val next = nextNonWhitespace(after)
                if (next == '(') return TokenKind.FUNCTION
            }
            if (isCallable(after)) return TokenKind.FUNCTION
            if (lastSignificant == '.') return TokenKind.PROPERTY
            return TokenKind.IDENTIFIER
        }

        private fun isCallable(after: Int): Boolean {
            val next = nextNonWhitespace(after)
            if (next != '(') return false
            // `foo (` con espacio amplio se trata como llamada; `a + b (` no.
            var i = after
            var spaces = 0
            while (i < n && text[i].isWhitespaceChar()) { i++; spaces++ }
            return i < n && text[i] == '(' && spaces <= 1
        }

        private fun nextNonWhitespace(from: Int): Char {
            var i = from
            while (i < n && (text[i] == ' ' || text[i] == '\t')) i++
            return if (i < n) text[i] else '\u0000'
        }

        private fun scanClosingBracket(c: Char): Boolean {
            val top = brackets.lastOrNull()
            if (top == null || lang.brackets.none { it.close == c }) {
                emit(pos, 1, TokenKind.PUNCTUATION)
                pos++
                lastSignificant = c
                return true
            }
            val expected = lang.brackets.first { it.open == top.char }
            if (expected.close != c) {
                emit(pos, 1, TokenKind.PUNCTUATION)
                pos++
                lastSignificant = c
                return true
            }
            brackets.removeLast()
            emit(pos, 1, TokenKind.PUNCTUATION)
            pos++
            lastSignificant = c
            return true
        }

        private fun commentKind(start: Int, end: Int): TokenKind {
            val afterToken = lang.lineComments
                .firstOrNull { text.startsWith(it, start) }
                ?.length ?: 1
            if (lang.docLinePrefixes.any { prefix ->
                    prefix.length > afterToken && text.startsWith(prefix, start + afterToken)
                }
            ) return TokenKind.DOC_COMMENT
            return TokenKind.COMMENT
        }

        private fun finish() {
            if (strings.isNotEmpty()) {
                flushBody()
                report(n, "Cadena sin terminar al final del archivo", Severity.ERROR)
            }
            while (brackets.isNotEmpty()) {
                val entry = brackets.removeLast()
                report(entry.offset, "Falta cerrar '${entry.char}'", Severity.ERROR)
            }
        }

        private fun emit(start: Int, length: Int, kind: TokenKind) {
            if (length > 0) tokens.add(Token(start, length, kind))
        }

        private fun countNewlines(from: Int, to: Int) {
            for (i in from until minOf(to, n)) {
                if (text[i] == '\n') { line++; lineStart = i + 1 }
            }
        }

        private fun report(offset: Int, message: String, severity: Severity) {
            // Convierte el desplazamiento a línea/columna en O(1) usando el
            // contador que ya se lleva: recorrer el texto sería O(n) por
            // diagnóstico y convertiría el escaneo en O(n²).
            val col = offset - lineStart
            diagnostics.add(Diagnostic(line, col, message, severity))
        }
    }

    private fun CharSequence.startsWith(prefix: String, at: Int): Boolean {
        if (at < 0 || at + prefix.length > length) return false
        for (i in prefix.indices) if (this[at + i] != prefix[i]) return false
        return true
    }

    private fun CharSequence.indexOfFrom(needle: String, from: Int): Int {
        if (needle.isEmpty()) return -1
        var i = from
        val limit = length - needle.length
        while (i <= limit) {
            if (startsWith(needle, i)) return i
            i++
        }
        return -1
    }

    private fun Char.isWhitespaceChar(): Boolean = this == ' ' || this == '\t' || this == '\n' || this == '\r'

    private fun Char.isDigitChar(): Boolean = this in '0'..'9'

    private fun Char.isRangeDot(): Boolean = this == '.'

    private fun Char.isUpperCaseChar(): Boolean = this in 'A'..'Z'

    private fun Char.isOperatorChar(): Boolean =
        this in "+-*/%=<>!&|^~?:"

    private fun isIdentStart(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c == '_' || c == '$' || c == '@'

    private fun isIdentPart(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '$'
}