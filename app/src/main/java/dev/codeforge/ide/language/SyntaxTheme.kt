package dev.codeforge.ide.language

import androidx.compose.ui.graphics.Color

/** Puente entre la paleta pura (testeable en JVM) y los colores de Compose. */
data class SyntaxTheme(val palette: SyntaxPalette) {
    fun colorOf(kind: TokenKind): Color = Color(palette.of(kind).toULong().toLong())

    companion object {
        val Dark = SyntaxTheme(SyntaxPalette.Dark)
        val Light = SyntaxTheme(SyntaxPalette.Light)
    }
}
