package dev.codeforge.ide.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.codeforge.ide.language.Languages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests de las transformaciones de edición. Solo necesitan `compose.ui.text`,
 * que es una dependencia JVM pura, así que corren sin Android.
 */
class EditorActionsTest {

    private fun value(text: String, cursor: Int = text.length) =
        TextFieldValue(text = text, selection = TextRange(cursor))

    /** Simula teclear [char] al final de [base]. */
    private fun type(
        base: String,
        char: Char,
        lang: dev.codeforge.ide.language.LanguageDefinition = Languages.KOTLIN,
    ): String {
        val text = base + char
        val v = TextFieldValue(text = text, selection = TextRange(text.length))
        return EditorActions.process(base, v, lang).text
    }

    /** Simula pulsar Enter al final de [base]. */
    private fun enter(
        base: String,
        lang: dev.codeforge.ide.language.LanguageDefinition = Languages.KOTLIN,
    ): String {
        val text = base + "\n"
        val v = TextFieldValue(text = text, selection = TextRange(text.length))
        return EditorActions.process(base, v, lang).text
    }

    // ---------- Cierre automático ----------

    @Test
    fun `cierra parentesis`() {
        assertEquals("f()", type("f", '('))
    }

    @Test
    fun `cierra llave`() {
        assertEquals("{}", type("", '{'))
    }

    @Test
    fun `cierra corchete`() {
        assertEquals("[]", type("", '['))
    }

    @Test
    fun `cierra comilla doble`() {
        assertEquals("\"\"", type("", '"'))
    }

    @Test
    fun `no cierra si ya hay un cierre delante`() {
        // Escribir `(` justo antes de `)` no debe duplicar el cierre.
        val v2 = TextFieldValue(text = "()", selection = TextRange(1))
        val out = EditorActions.process(")", v2, Languages.KOTLIN)
        assertEquals("()", out.text)
    }

    // ---------- Auto-indentación ----------

    @Test
    fun `enter conserva la indentacion`() {
        val out = enter("    val x = 1")
        assertEquals("    val x = 1\n    ", out)
    }

    @Test
    fun `enter tras llave abre un nivel mas`() {
        val out = enter("fun f() {")
        assertEquals("fun f() {\n    ", out)
    }

    @Test
    fun `enter tras llave de cierre no indenta`() {
        assertEquals("}\n", enter("}"))
    }

    @Test
    fun `python solo replica la indentacion`() {
        assertEquals("    x = 1\n    ", enter("    x = 1", Languages.PYTHON))
    }

    @Test
    fun `el cursor queda tras la indentacion insertada`() {
        val text = "    val x = 1\n"
        val v = TextFieldValue(text = text, selection = TextRange(text.length))
        val out = EditorActions.process("    val x = 1", v, Languages.KOTLIN)
        assertEquals(out.text.length, out.selection.end)
        assertTrue(out.text.endsWith("\n    "), "esperaba indent: ${out.text}")
    }

    // ---------- Comentarios ----------

    @Test
    fun `comenta la linea actual`() {
        val v = value("val x = 1", 3)
        val out = EditorActions.toggleComment(v, Languages.KOTLIN)
        assertEquals("// val x = 1", out.text)
    }

    @Test
    fun `descomenta si ya esta comentado`() {
        val v = value("// val x = 1", 3)
        val out = EditorActions.toggleComment(v, Languages.KOTLIN)
        assertEquals(" val x = 1", out.text)
    }

    @Test
    fun `usa el comentario del lenguaje - python hash`() {
        val v = value("x = 1", 0)
        val out = EditorActions.toggleComment(v, Languages.PYTHON)
        assertEquals("# x = 1", out.text)
    }

    @Test
    fun `comenta varias lineas`() {
        val v = TextFieldValue(text = "a\nb", selection = TextRange(0, 3))
        val out = EditorActions.toggleComment(v, Languages.KOTLIN)
        assertEquals("// a\n// b", out.text)
    }

    @Test
    fun `no hace nada en lenguaje sin comentarios - html`() {
        val v = value("<p/>", 0)
        val out = EditorActions.toggleComment(v, Languages.HTML)
        assertEquals("<p/>", out.text)
    }

    @Test
    fun `json admite comentarios de linea estilo jsonc`() {
        val v = value("{}", 0)
        val out = EditorActions.toggleComment(v, Languages.JSON)
        assertEquals("// {}", out.text)
    }
}
