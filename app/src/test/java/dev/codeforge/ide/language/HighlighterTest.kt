package dev.codeforge.ide.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests del núcleo multi-lenguaje. Corren en JVM puro: no dependen de Android,
 * así que validan el tokenizador sin necesidad de aapt2 ni emulador.
 */
class HighlighterTest {

    private fun tokensOf(code: String, lang: LanguageDefinition): List<Pair<String, TokenKind>> {
        val res = Highlighter.highlight(code, lang)
        return res.tokens.map { code.substring(it.start, it.end) to it.kind }
    }

    private fun kindsOf(code: String, lang: LanguageDefinition): List<TokenKind> =
        tokensOf(code, lang).map { it.second }

    // ---------- Detección de lenguaje ----------

    @Test
    fun `detecta kotlin por extension`() {
        assertEquals("kotlin", LanguageRegistry.detect("MainActivity.kt").id)
        assertEquals("kotlin", LanguageRegistry.detect("Foo.kt").id)
    }

    @Test
    fun `detecta gradle por nombre de archivo`() {
        // Se resuelve por nombre exacto antes que por extensión `kts`.
        assertEquals("gradle", LanguageRegistry.detect("build.gradle.kts").id)
        assertEquals("gradle", LanguageRegistry.detect("build.gradle").id)
    }

    @Test
    fun `detecta por nombre de archivo exacto`() {
        assertEquals("dockerfile", LanguageRegistry.detect("Dockerfile").id)
        assertEquals("xml", LanguageRegistry.detect("AndroidManifest.xml").id)
        assertEquals("gradle", LanguageRegistry.detect("build.gradle").id)
    }

    @Test
    fun `detecta json por package json`() {
        assertEquals("json", LanguageRegistry.detect("package.json").id)
    }

    @Test
    fun `cae a texto plano si no reconoce`() {
        assertEquals("plaintext", LanguageRegistry.detect("archivo.xyz").id)
        assertEquals("plaintext", LanguageRegistry.detect("LICENSE").id)
    }

    @Test
    fun `deteccion no distingue mayusculas`() {
        assertEquals("python", LanguageRegistry.detect("script.PY").id)
    }

    // ---------- Clasificación de palabras clave ----------

    @Test
    fun `kotlin clasifica if como control y class como keyword`() {
        val toks = tokensOf("if (a) class B", Languages.KOTLIN)
        assertEquals(TokenKind.CONTROL_KEYWORD, toks[0].second)
        assertTrue(toks.any { it.first == "class" && it.second == TokenKind.KEYWORD })
    }

    @Test
    fun `java clasifica public como keyword`() {
        val toks = tokensOf("public class A", Languages.JAVA)
        assertEquals(TokenKind.KEYWORD, toks[0].second)
        assertEquals(TokenKind.KEYWORD, toks[1].second)
    }

    @Test
    fun `constantes de cada lenguaje`() {
        assertTrue(TokenKind.CONSTANT in kindsOf("True", Languages.PYTHON))
        assertTrue(TokenKind.CONSTANT in kindsOf("true", Languages.RUBY))
        assertTrue(TokenKind.CONSTANT in kindsOf("nil", Languages.LUA))
        assertTrue(TokenKind.CONSTANT in kindsOf("null", Languages.DART))
    }

    @Test
    fun `python distingue True de true`() {
        // En Python las constantes van en mayúscula: `true` es un identificador.
        assertTrue(TokenKind.IDENTIFIER in kindsOf("true", Languages.PYTHON))
    }

    @Test
    fun `sql clasifica palabras en mayusculas`() {
        val toks = tokensOf("SELECT * FROM users", Languages.SQL)
        assertEquals(TokenKind.CONTROL_KEYWORD, toks[0].second)
        assertTrue(toks.any { it.first == "FROM" && it.second == TokenKind.KEYWORD })
    }

    // ---------- Cadenas ----------

    @Test
    fun `comentario de linea no colorea el codigo posterior`() {
        val toks = tokensOf("// hola\nval x = 1", Languages.KOTLIN)
        val comment = toks.first { it.first.startsWith("//") }
        assertEquals(TokenKind.COMMENT, comment.second)
        assertTrue(toks.any { it.first == "val" && it.second == TokenKind.KEYWORD })
    }

    @Test
    fun `comentario de bloque se maneja`() {
        val toks = tokensOf("/* bloque */ val x", Languages.KOTLIN)
        assertEquals(TokenKind.COMMENT, toks[0].second)
        assertTrue(toks.any { it.first == "val" })
    }

    @Test
    fun `comentario de documentacion se distingue`() {
        val toks = tokensOf("/** doc */ val x", Languages.KOTLIN)
        assertEquals(TokenKind.DOC_COMMENT, toks[0].second)
    }

    @Test
    fun `comentario de linea segun lenguaje - python usa hash`() {
        val toks = tokensOf("# comentario\ndef f():", Languages.PYTHON)
        assertEquals(TokenKind.COMMENT, toks[0].second)
        assertTrue(toks.any { it.first == "def" && it.second == TokenKind.KEYWORD })
    }

    @Test
    fun `shell usa hash y no doble slash`() {
        val toks = tokensOf("#!/bin/bash\necho 1", Languages.SHELL)
        assertEquals(TokenKind.COMMENT, toks[0].second)
    }

    // ---------- Cadenas y plantillas ----------

    @Test
    fun `cadena simple`() {
        val toks = tokensOf("val s = \"hola\"", Languages.KOTLIN)
        assertTrue(toks.any { it.first == "hola" && it.second == TokenKind.STRING })
    }

    @Test
    fun `comilla simple es cadena en python`() {
        val toks = tokensOf("x = 'texto'", Languages.PYTHON)
        assertTrue(toks.any { it.first == "texto" && it.second == TokenKind.STRING })
    }

    @Test
    fun `plantilla kotlin con interpolacion`() {
        val code = "val s = \"hola \$nombre mundo\""
        val toks = tokensOf(code, Languages.KOTLIN)
        assertTrue(toks.any { it.first == "\$nombre" && it.second == TokenKind.TEMPLATE })
    }

    @Test
    fun `plantilla con llaves`() {
        val code = "val s = \"suma \${a + b} fin\""
        val toks = tokensOf(code, Languages.KOTLIN)
        assertTrue(toks.any { it.first == "\${a + b}" && it.second == TokenKind.TEMPLATE })
    }

    @Test
    fun `raw string kotlin multilinea`() {
        val code = "val s = \"\"\"linea1\nlinea2\"\"\""
        val res = Highlighter.highlight(code, Languages.KOTLIN)
        assertTrue(res.diagnostics.isEmpty(), "raw string no debe producir diagnósticos")
    }

    @Test
    fun `escape en cadena no rompe el cierre`() {
        val code = "val s = \"a\\\"b\""
        val toks = tokensOf(code, Languages.KOTLIN)
        assertTrue(toks.any { it.second == TokenKind.STRING })
    }

    @Test
    fun `plantilla javascript con backtick`() {
        val code = "const s = `hola \${x} mundo`"
        val toks = tokensOf(code, Languages.JAVASCRIPT)
        assertTrue(toks.any { it.first == "\${x}" && it.second == TokenKind.TEMPLATE })
    }

    @Test
    fun `comentario dentro de cadena no es comentario`() {
        val code = "val s = \"// no soy comentario\""
        val toks = tokensOf(code, Languages.KOTLIN)
        assertFalse(toks.any { it.second == TokenKind.COMMENT })
    }

    // ---------- Números ----------

    @Test
    fun `enteros y decimales`() {
        assertTrue(TokenKind.NUMBER in kindsOf("42", Languages.KOTLIN))
        assertTrue(TokenKind.NUMBER in kindsOf("3.14", Languages.PYTHON))
    }

    @Test
    fun `hexadecimal`() {
        val toks = tokensOf("0xFF", Languages.JAVA)
        assertEquals(TokenKind.NUMBER, toks[0].second)
        assertEquals("0xFF", toks[0].first)
    }

    @Test
    fun `rango de kotlin no se come los puntos`() {
        val code = "for (i in 1..10) {}"
        val toks = tokensOf(code, Languages.KOTLIN)
        assertTrue(toks.any { it.first == "1" && it.second == TokenKind.NUMBER })
        assertTrue(toks.any { it.first == "10" && it.second == TokenKind.NUMBER })
    }

    @Test
    fun `acceso a propiedad tras punto`() {
        val toks = tokensOf("val n = obj.prop", Languages.KOTLIN)
        assertTrue(toks.any { it.first == "prop" && it.second == TokenKind.PROPERTY })
    }

    // ---------- Anotaciones y llamadas ----------

    @Test
    fun `anotacion kotlin`() {
        val toks = tokensOf("@Override fun f()", Languages.KOTLIN)
        assertTrue(toks.any { it.first == "@Override" && it.second == TokenKind.ANNOTATION })
    }

    @Test
    fun `llamada a funcion se detecta`() {
        val toks = tokensOf("println(\"x\")", Languages.KOTLIN)
        assertTrue(toks.any { it.first == "println" && it.second == TokenKind.BUILTIN })
        val custom = tokensOf("miFuncion(1)", Languages.KOTLIN)
        assertTrue(custom.any { it.first == "miFuncion" && it.second == TokenKind.FUNCTION })
    }

    // ---------- Diagnósticos ----------

    @Test
    fun `detecta cadena sin terminar`() {
        val res = Highlighter.highlight("val s = \"sin cerrar", Languages.KOTLIN)
        assertTrue(res.diagnostics.isNotEmpty(), "debe reportar cadena sin terminar")
        assertTrue(res.diagnostics.any { it.message.contains("sin terminar") })
    }

    @Test
    fun `detecta parentesis sin cerrar`() {
        val res = Highlighter.highlight("fun f( {", Languages.KOTLIN)
        assertTrue(res.diagnostics.any { it.message.contains("Falta cerrar") })
    }

    @Test
    fun `codigo balanceado no genera diagnosticos`() {
        val res = Highlighter.highlight("fun f(a: Int) { val x = listOf(1, 2) }", Languages.KOTLIN)
        assertTrue(res.diagnostics.isEmpty(), "no debe haber diagnósticos: ${res.diagnostics}")
    }

    @Test
    fun `corchetes dentro de cadena no afectan el balance`() {
        val res = Highlighter.highlight("val s = \"((\"", Languages.KOTLIN)
        assertTrue(res.diagnostics.isEmpty(), "los paréntesis en cadena no cuentan: ${res.diagnostics}")
    }

    @Test
    fun `comentario con parentesis no afecta el balance`() {
        val res = Highlighter.highlight("// ( unbalanced\nval x = 1", Languages.KOTLIN)
        assertTrue(res.diagnostics.isEmpty())
    }

    // ---------- Cobertura transversal ----------

    @Test
    fun `todos los lenguajes tokenizan sin excepciones`() {
        for (lang in LanguageRegistry.languages) {
            val sample = "fun f() { /* c */ val s = \"x\"; return 42 } // fin"
            val res = Highlighter.highlight(sample, lang)
            assertTrue(res.tokens.isNotEmpty() || lang.id == "markdown",
                "el lenguaje ${lang.id} no produjo tokens")
        }
    }

    @Test
    fun `ningun token se sale de rango`() {
        val code = "class A { val s = \"x\" // c\n fun f() = 1 }"
        for (lang in LanguageRegistry.languages) {
            val res = Highlighter.highlight(code, lang)
            for (t in res.tokens) {
                assertTrue(t.start >= 0, "start negativo en ${lang.id}")
                assertTrue(t.end <= code.length, "token fuera de rango en ${lang.id}: $t")
                assertTrue(t.length > 0, "token vacío en ${lang.id}")
            }
        }
    }

    @Test
    fun `los tokens no se solapan`() {
        val code = "val x = foo(1, \"s\") // c"
        val res = Highlighter.highlight(code, Languages.KOTLIN)
        var prevEnd = 0
        for (t in res.tokens.sortedBy { it.start }) {
            assertTrue(t.start >= prevEnd, "token solapado: $t")
            prevEnd = t.end
        }
    }

    @Test
    fun `texto vacio y edge cases`() {
        assertEquals(0, Highlighter.highlight("", Languages.KOTLIN).tokens.size)
        Highlighter.highlight("\\", Languages.KOTLIN)
        Highlighter.highlight("\"", Languages.KOTLIN)
        Highlighter.highlight("'", Languages.PYTHON)
        Highlighter.highlight("/*", Languages.KOTLIN)
        Highlighter.highlight("unterminated /*", Languages.JAVA)
    }

    @Test
    fun `texto mayor al limite devuelve lista vacia sin colgarse`() {
        val res = Highlighter.highlight("a".repeat(100), Languages.KOTLIN, maxTextLength = 10)
        assertTrue(res.tokens.isEmpty())
    }

    @Test
    fun `texto en el limite si se tokeniza`() {
        val res = Highlighter.highlight("a".repeat(10), Languages.KOTLIN, maxTextLength = 10)
        assertEquals(1, res.tokens.size)
    }
}
