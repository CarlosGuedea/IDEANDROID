package dev.codeforge.ide.language

/**
 * Paleta de colores en formato ARGB, independiente de Android.
 *
 * Vive aparte de `SyntaxTheme` (que usa `compose.ui.graphics.Color`) para que el
 * núcleo del tokenizador se pueda compilar y testear en una JVM desnuda.
 */
data class SyntaxPalette(
    val plain: Long,
    val keyword: Long,
    val controlKeyword: Long,
    val type: Long,
    val constant: Long,
    val builtin: Long,
    val function: Long,
    val string: Long,
    val template: Long,
    val number: Long,
    val operator: Long,
    val punctuation: Long,
    val comment: Long,
    val docComment: Long,
    val annotation: Long,
    val property: Long,
    val identifier: Long,
) {
    fun of(kind: TokenKind): Long = when (kind) {
        TokenKind.PLAIN -> plain
        TokenKind.KEYWORD -> keyword
        TokenKind.CONTROL_KEYWORD -> controlKeyword
        TokenKind.TYPE -> type
        TokenKind.CONSTANT -> constant
        TokenKind.BUILTIN -> builtin
        TokenKind.FUNCTION -> function
        TokenKind.STRING -> string
        TokenKind.TEMPLATE -> template
        TokenKind.NUMBER -> number
        TokenKind.OPERATOR -> operator
        TokenKind.PUNCTUATION -> punctuation
        TokenKind.COMMENT -> comment
        TokenKind.DOC_COMMENT -> docComment
        TokenKind.ANNOTATION -> annotation
        TokenKind.PROPERTY -> property
        TokenKind.IDENTIFIER -> identifier
    }

    companion object {
        val Dark = SyntaxPalette(
            plain = 0xFFC9D1D9,
            keyword = 0xFFC792EA,
            controlKeyword = 0xFFFF7B72,
            type = 0xFF82AAFF,
            constant = 0xFFF78C6C,
            builtin = 0xFF89DDFF,
            function = 0xFFD2A8FF,
            string = 0xFFC3E88D,
            template = 0xFF9CDCFE,
            number = 0xFFF78C6C,
            operator = 0xFF89DDFF,
            punctuation = 0xFF8B949E,
            comment = 0xFF6A737D,
            docComment = 0xFF7EA2C4,
            annotation = 0xFFFFB86C,
            property = 0xFFB9C6D3,
            identifier = 0xFFC9D1D9,
        )

        val Light = SyntaxPalette(
            plain = 0xFF24292F,
            keyword = 0xFFCF222E,
            controlKeyword = 0xFF0550AE,
            type = 0xFF953800,
            constant = 0xFF0550AE,
            builtin = 0xFF8250DF,
            function = 0xFF8250DF,
            string = 0xFF0A7D33,
            template = 0xFF0A3069,
            number = 0xFF0550AE,
            operator = 0xFF0550AE,
            punctuation = 0xFF57606A,
            comment = 0xFF6E7781,
            docComment = 0xFF3B5F8A,
            annotation = 0xFF953800,
            property = 0xFF24292F,
            identifier = 0xFF24292F,
        )
    }
}
