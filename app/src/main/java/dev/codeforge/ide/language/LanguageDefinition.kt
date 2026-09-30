package dev.codeforge.ide.language

/** Categorías de token que el motor de resaltado puede emitir. */
enum class TokenKind {
    PLAIN,
    KEYWORD,
    CONTROL_KEYWORD,
    TYPE,
    CONSTANT,
    BUILTIN,
    FUNCTION,
    STRING,
    TEMPLATE,
    NUMBER,
    OPERATOR,
    PUNCTUATION,
    COMMENT,
    DOC_COMMENT,
    ANNOTATION,
    PROPERTY,
    IDENTIFIER,
}

/** Estilo de indentación automático al pulsar Enter. */
enum class IndentStyle { BRACES, PYTHON_LIKE, C_LIKE }

data class CommentPair(val open: String, val close: String)

data class BracketPair(val open: Char, val close: Char)

/**
 * Definición de un literal de cadena.
 *
 * [interpolator] activa el soporte de plantillas (`$name`, `${...}`) cuando no es nulo.
 */
data class StringSpec(
    val open: String,
    val close: String = open,
    val multiline: Boolean = false,
    val escapes: Boolean = true,
    val interpolator: Char? = null,
)

/**
 * Descripción declarativa de un lenguaje. Todo el soporte multi-lenguaje del IDE
 * se reduce a instancias de esta clase: añadir un lenguaje = añadir una entrada aquí.
 */
data class LanguageDefinition(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val filenames: List<String> = emptyList(),
    val lineComments: List<String> = listOf("//"),
    val blockComments: List<CommentPair> = emptyList(),
    val strings: List<StringSpec> = listOf(
        StringSpec("\"", escapes = true),
        StringSpec("'", escapes = true),
    ),
    val keywords: Set<String> = emptySet(),
    val controlKeywords: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val constants: Set<String> = emptySet(),
    val builtins: Set<String> = emptySet(),
    val brackets: List<BracketPair> = listOf(
        BracketPair('(', ')'),
        BracketPair('[', ']'),
        BracketPair('{', '}'),
    ),
    val indentStyle: IndentStyle = IndentStyle.BRACES,
    val indentUnit: String = "    ",
    val docLinePrefixes: List<String> = listOf("///", "/**", "*", "#"),
    /** SQL y similares: las palabras clave se comparan sin distinguir mayúsculas. */
    val caseInsensitiveKeywords: Boolean = false,
) {
    /** Extensiones normalizadas (sin punto, en minúsculas) para comparación rápida. */
    val normalizedExtensions: List<String> = extensions.map { it.removePrefix(".").lowercase() }

    val lineCommentToken: String get() = lineComments.firstOrNull() ?: "//"

    /** Todos los delimitadores de apertura de cadena, ordenados de más largo a más corto. */
    val stringOpens: List<String> = strings.map { it.open }.distinct().sortedByDescending { it.length }

    fun matchesExtension(ext: String): Boolean =
        normalizedExtensions.contains(ext.removePrefix(".").lowercase())

    fun matchesFilename(name: String): Boolean {
        if (filenames.contains(name)) return true
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return false
        return matchesExtension(name.substring(dot + 1))
    }
}

/** Resolución de lenguaje por extensión o nombre de archivo. */
object LanguageRegistry {
    val languages: List<LanguageDefinition> = listOf(
        Languages.KOTLIN,
        Languages.JAVA,
        Languages.JAVASCRIPT,
        Languages.TYPESCRIPT,
        Languages.PYTHON,
        Languages.C,
        Languages.CPP,
        Languages.CSHARP,
        Languages.GO,
        Languages.RUST,
        Languages.SWIFT,
        Languages.PHP,
        Languages.RUBY,
        Languages.DART,
        Languages.SCALA,
        Languages.LUA,
        Languages.PERL,
        Languages.HTML,
        Languages.CSS,
        Languages.SCSS,
        Languages.JSON,
        Languages.XML,
        Languages.YAML,
        Languages.TOML,
        Languages.SQL,
        Languages.SHELL,
        Languages.MARKDOWN,
        Languages.DOCKERFILE,
        Languages.GRADLE,
        Languages.PLAIN_TEXT,
    )

    private val byId: Map<String, LanguageDefinition> = languages.associateBy { it.id }

    private val byExtension: Map<String, LanguageDefinition> = buildMap {
        for (lang in languages) {
            for (ext in lang.normalizedExtensions) putIfAbsent(ext, lang)
        }
    }

    private val byFilename: Map<String, LanguageDefinition> = buildMap {
        for (lang in languages) {
            for (name in lang.filenames) putIfAbsent(name, lang)
        }
    }

    fun byId(id: String): LanguageDefinition? = byId[id]

    /** Detecta el lenguaje a partir del nombre del archivo; cae a texto plano. */
    fun detect(fileName: String): LanguageDefinition {
        byFilename[fileName]?.let { return it }
        val dot = fileName.lastIndexOf('.')
        if (dot > 0) {
            byExtension[fileName.substring(dot + 1).lowercase()]?.let { return it }
        }
        return Languages.PLAIN_TEXT
    }
}
