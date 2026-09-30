package dev.codeforge.ide.language

/**
 * Definiciones de los lenguajes soportados.
 *
 * Cada entrada es puramente datos: añadir soporte para un lenguaje nuevo no
 * requiere tocar el tokenizador, solo añadir una [LanguageDefinition].
 */
object Languages {

    private val C_LIKE = listOf(
        CommentPair("/*", "*/"),
    )

    private val HASH_COMMENT = listOf("#")

    private val DOUBLE_QUOTE = listOf(StringSpec("\"", escapes = true, interpolator = '$'))
    private val DOUBLE_QUOTE_PLAIN = listOf(StringSpec("\"", escapes = true))
    private val SINGLE_QUOTE_PLAIN = listOf(StringSpec("'", escapes = true))

    val KOTLIN = LanguageDefinition(
        id = "kotlin",
        displayName = "Kotlin",
        extensions = listOf("kt", "kts"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("\"\"\"", multiline = true, escapes = false, interpolator = '$'),
            StringSpec("\"", escapes = true, interpolator = '$'),
            StringSpec("'", escapes = true),
        ),
        keywords = setOf(
            "as", "break", "by", "catch", "class", "constructor", "continue", "do", "else",
            "finally", "for", "get", "if", "in", "init", "interface", "is", "out", "package",
            "set", "super", "this", "throw", "try", "typealias", "val", "var", "when", "where",
            "while", "companion", "operator", "data", "sealed", "inline", "value", "open",
            "suspend", "internal", "lateinit", "override", "abstract", "final", "reified",
            "crossinline", "noinline", "vararg", "it", "fun", "object",
        ),
        controlKeywords = setOf(
            "if", "else", "when", "for", "while", "do", "return", "break", "continue", "throw",
            "try", "catch", "finally",
        ),
        types = setOf(
            "Any", "Unit", "Nothing", "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double",
            "Char", "String", "Array", "List", "MutableList", "Map", "MutableMap", "Set",
            "MutableSet", "Sequence", "Pair", "Triple", "IntRange", "LongRange", "CharRange",
        ),
        constants = setOf("true", "false", "null"),
        builtins = setOf(
            "listOf", "mutableListOf", "setOf", "mutableSetOf", "mapOf", "mutableMapOf",
            "arrayOf", "emptyList", "println", "print", "readLine", "require", "check", "error",
            "TODO", "lazy", "lateinit", "apply", "let", "run", "also", "with", "takeIf", "takeUnless",
        ),
    )

    val JAVA = LanguageDefinition(
        id = "java",
        displayName = "Java",
        extensions = listOf("java"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE_PLAIN + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "abstract", "assert", "class", "enum", "extends", "final", "implements", "import",
            "instanceof", "interface", "native", "new", "package", "private", "protected",
            "public", "record", "sealed", "static", "strictfp", "synchronized", "transient",
            "volatile", "yield", "permits", "var", "class", "module", "requires", "exports",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "throw", "try", "catch", "finally",
        ),
        types = setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void",
            "String", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte",
            "Short", "Object", "List", "Map", "Set", "Optional", "Stream", "StringBuilder",
        ),
        constants = setOf("true", "false", "null"),
        builtins = setOf("System", "Math", "Arrays", "Collections", "Objects", "Optional"),
    )

    val JAVASCRIPT = LanguageDefinition(
        id = "javascript",
        displayName = "JavaScript",
        extensions = listOf("js", "mjs", "cjs", "jsx"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("`", multiline = true, escapes = true, interpolator = '$'),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "as", "async", "await", "class", "const", "delete", "export", "extends", "from",
            "get", "import", "in", "instanceof", "let", "new", "of", "set", "static", "super",
            "typeof", "void", "with", "yield", "function",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "throw", "try", "catch", "finally",
        ),
        constants = setOf("true", "false", "null", "undefined", "NaN", "Infinity"),
        builtins = setOf(
            "console", "document", "window", "Math", "JSON", "Object", "Array", "String",
            "Number", "Boolean", "Promise", "Symbol", "Map", "Set", "RegExp", "Error", "globalThis",
        ),
    )

    val TYPESCRIPT = JAVASCRIPT.copy(
        id = "typescript",
        displayName = "TypeScript",
        extensions = listOf("ts", "tsx", "mts", "cts"),
        filenames = listOf("tsconfig.json"),
        types = setOf(
            "string", "number", "boolean", "any", "unknown", "never", "void", "object", "symbol",
            "bigint", "Record", "Partial", "Readonly", "Pick", "Omit", "Array", "Promise",
        ),
        keywords = JAVASCRIPT.keywords + setOf(
            "interface", "type", "enum", "implements", "declare", "namespace", "readonly",
            "abstract", "public", "private", "protected", "keyof", "infer", "is", "satisfies",
        ),
    )

    val PYTHON = LanguageDefinition(
        id = "python",
        displayName = "Python",
        extensions = listOf("py", "pyw", "pyi"),
        lineComments = HASH_COMMENT,
        strings = listOf(
            StringSpec("\"\"\"", multiline = true, escapes = true),
            StringSpec("'''", multiline = true, escapes = true),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "and", "as", "assert", "async", "await", "class", "def", "del", "elif", "except",
            "from", "global", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "with", "yield", "match", "case",
        ),
        controlKeywords = setOf(
            "if", "elif", "else", "for", "while", "break", "continue", "return", "try", "except",
            "finally", "raise", "with", "assert", "pass",
        ),
        types = setOf(
            "int", "float", "complex", "str", "bytes", "bytearray", "bool", "list", "dict",
            "set", "frozenset", "tuple", "object", "type",
        ),
        constants = setOf("True", "False", "None"),
        builtins = setOf(
            "print", "len", "range", "open", "input", "isinstance", "getattr", "setattr",
            "hasattr", "zip", "map", "filter", "sum", "min", "max", "abs", "sorted", "enumerate",
            "super", "self", "Exception", "ValueError", "TypeError", "KeyError",
        ),
        indentStyle = IndentStyle.PYTHON_LIKE,
        indentUnit = "    ",
        docLinePrefixes = listOf("\"\"\"", "'''", "#"),
    )

    val C = LanguageDefinition(
        id = "c",
        displayName = "C",
        extensions = listOf("c", "h"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE_PLAIN + listOf(StringSpec("'", escapes = true)),
        keywords = setOf(
            "auto", "const", "extern", "inline", "register", "restrict", "sizeof", "static",
            "typedef", "union", "volatile", "struct", "_Atomic", "_Noreturn", "_Alignas",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "goto",
        ),
        types = setOf(
            "char", "double", "float", "int", "long", "short", "signed", "unsigned", "void",
            "bool", "size_t", "ssize_t", "int8_t", "int16_t", "int32_t", "int64_t", "uint8_t",
            "FILE",
        ),
        constants = setOf("NULL", "true", "false"),
        builtins = setOf("printf", "malloc", "free", "memcpy", "memset", "strlen", "strcmp", "fopen"),
        indentUnit = "  ",
    )

    val CPP = C.copy(
        id = "cpp",
        displayName = "C++",
        extensions = listOf("cpp", "cc", "cxx", "hpp", "hh", "hxx", "ipp"),
        keywords = C.keywords + setOf(
            "class", "namespace", "template", "typename", "public", "private", "protected",
            "virtual", "override", "final", "new", "delete", "this", "using", "operator",
            "explicit", "constexpr", "consteval", "noexcept", "nullptr", "friend", "mutable",
        ),
        controlKeywords = C.controlKeywords + setOf("try", "catch", "throw"),
        types = C.types + setOf("string", "wstring", "vector", "map", "set", "auto", "uint32_t"),
        constants = setOf("nullptr", "true", "false", "NULL"),
        builtins = C.builtins + setOf("std", "cout", "cin", "cerr", "endl", "move", "forward"),
    )

    val CSHARP = LanguageDefinition(
        id = "csharp",
        displayName = "C#",
        extensions = listOf("cs"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("$@\"", close = "\"", multiline = true, escapes = false, interpolator = '$'),
        ) + listOf(
            StringSpec("@\"", close = "\"", multiline = true, escapes = false),
            StringSpec("$@\"", close = "\"", multiline = true, escapes = true, interpolator = '$'),
            StringSpec("$\"", close = "\"", escapes = true, interpolator = '$'),
        ) + DOUBLE_QUOTE_PLAIN,
        keywords = setOf(
            "abstract", "as", "base", "class", "delegate", "enum", "event", "explicit", "extern",
            "fixed", "implicit", "interface", "internal", "lock", "namespace", "operator", "out",
            "override", "params", "partial", "private", "protected", "public", "readonly", "ref",
            "sealed", "static", "struct", "unchecked", "unsafe", "virtual", "volatile", "where",
            "async", "await", "record", "init", "global", "required", "file", "scoped", "with",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "foreach", "while", "do", "switch", "case", "default", "break",
            "continue", "return", "throw", "try", "catch", "finally", "yield",
        ),
        types = setOf(
            "bool", "byte", "char", "decimal", "double", "float", "int", "long", "object", "sbyte",
            "short", "string", "uint", "ulong", "ushort", "var", "void", "dynamic", "Task", "List",
            "Dictionary",
        ),
        constants = setOf("true", "false", "null"),
        builtins = setOf("Console", "Math", "String", "Exception", "Task", "IEnumerable"),
    )

    val GO = LanguageDefinition(
        id = "go",
        displayName = "Go",
        extensions = listOf("go"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE + listOf(
            StringSpec("`", multiline = true, escapes = false),
            StringSpec("'", escapes = true),
        ),
        keywords = setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else",
            "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map",
            "package", "range", "return", "select", "struct", "switch", "type", "var",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "range", "switch", "case", "default", "break", "continue",
            "return", "goto", "defer", "select",
        ),
        types = setOf(
            "bool", "byte", "complex64", "complex128", "error", "float32", "float64", "int",
            "int8", "int16", "int32", "int64", "rune", "string", "uint", "uint8", "uint16",
            "uint32", "uint64", "uintptr", "any",
        ),
        constants = setOf("true", "false", "nil", "iota"),
        builtins = setOf("make", "new", "len", "cap", "append", "copy", "delete", "panic", "recover", "print", "println"),
        indentUnit = "\t",
    )

    val RUST = LanguageDefinition(
        id = "rust",
        displayName = "Rust",
        extensions = listOf("rs"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("r#\"", close = "\"#", multiline = true, escapes = false),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "as", "async", "await", "crate", "dyn", "extern", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self",
            "static", "struct", "super", "trait", "type", "unsafe", "use", "where", "while",
            "union", "macro_rules",
        ),
        controlKeywords = setOf(
            "if", "else", "match", "loop", "while", "for", "break", "continue", "return",
        ),
        types = setOf(
            "i8", "i16", "i32", "i64", "i128", "isize", "u8", "u16", "u32", "u64", "u128",
            "usize", "f32", "f64", "bool", "char", "str", "String", "Vec", "Option", "Result",
            "Box", "Rc", "Arc", "HashMap",
        ),
        constants = setOf("true", "false", "None", "Some", "Ok", "Err"),
        builtins = setOf("println", "print", "format", "vec", "panic", "assert", "write", "writeln"),
    )

    val SWIFT = LanguageDefinition(
        id = "swift",
        displayName = "Swift",
        extensions = listOf("swift"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("\"\"\"", multiline = true, escapes = true, interpolator = '$'),
        ) + DOUBLE_QUOTE,
        keywords = setOf(
            "as", "associatedtype", "class", "deinit", "enum", "extension", "fileprivate",
            "func", "import", "init", "inout", "internal", "let", "open", "operator", "private",
            "protocol", "public", "rethrows", "static", "struct", "subscript", "typealias",
            "var", "where", "indirect", "lazy", "weak", "unowned", "final", "override",
        ),
        controlKeywords = setOf(
            "if", "else", "guard", "switch", "case", "default", "for", "while", "repeat", "break",
            "continue", "return", "throw", "throws", "do", "catch", "defer", "in",
        ),
        types = setOf(
            "Int", "Double", "Float", "Bool", "String", "Character", "Array", "Dictionary",
            "Set", "Optional", "Any", "AnyObject", "Void", "Result",
        ),
        constants = setOf("true", "false", "nil"),
        builtins = setOf("print", "map", "filter", "reduce", "append", "assert", "precondition"),
    )

    val PHP = LanguageDefinition(
        id = "php",
        displayName = "PHP",
        extensions = listOf("php", "phtml", "php5"),
        filenames = listOf("composer.json"),
        lineComments = listOf("//", "#"),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class",
            "clone", "const", "continue", "declare", "default", "do", "echo", "else", "elseif",
            "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile",
            "extends", "final", "finally", "fn", "for", "foreach", "function", "global", "goto",
            "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface",
            "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public",
            "require", "require_once", "return", "static", "switch", "throw", "trait", "try",
            "unset", "use", "var", "while", "xor", "yield",
        ),
        controlKeywords = setOf(
            "if", "elseif", "else", "for", "foreach", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "throw", "try", "catch", "finally", "goto",
        ),
        constants = setOf("true", "false", "null", "TRUE", "FALSE", "NULL"),
        builtins = setOf("echo", "print_r", "var_dump", "count", "isset", "empty", "array", "sprintf"),
    )

    val RUBY = LanguageDefinition(
        id = "ruby",
        displayName = "Ruby",
        extensions = listOf("rb", "rake", "gemspec"),
        filenames = listOf("Gemfile", "Rakefile"),
        lineComments = HASH_COMMENT,
        blockComments = listOf(CommentPair("=begin", "=end")),
        strings = listOf(
            StringSpec(":\"", close = "\"", multiline = true, escapes = true, interpolator = '#'),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "alias", "and", "begin", "break", "case", "class", "def", "defined?", "do", "else",
            "elsif", "end", "ensure", "for", "if", "in", "module", "next", "not", "or", "redo",
            "rescue", "retry", "return", "self", "super", "then", "undef", "unless", "until",
            "when", "while", "yield", "require", "require_relative", "attr_accessor",
            "attr_reader", "attr_writer", "include", "extend", "lambda", "proc",
        ),
        controlKeywords = setOf(
            "if", "elsif", "else", "unless", "case", "when", "for", "while", "until", "do",
            "begin", "rescue", "ensure", "break", "next", "redo", "retry", "return", "yield",
        ),
        constants = setOf("true", "false", "nil", "__FILE__", "__LINE__"),
        builtins = setOf("puts", "print", "p", "gets", "raise", "loop", "new", "freeze"),
        indentStyle = IndentStyle.C_LIKE,
        indentUnit = "  ",
    )

    val DART = LanguageDefinition(
        id = "dart",
        displayName = "Dart",
        extensions = listOf("dart"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("'''", multiline = true, escapes = true, interpolator = '$'),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "abstract", "as", "assert", "async", "await", "class", "covariant", "deferred",
            "dynamic", "export", "extension", "external", "factory", "final", "get", "implements",
            "import", "interface", "late", "library", "mixin", "on", "operator", "part",
            "required", "rethrow", "sealed", "set", "static", "super", "this", "throw", "typedef",
            "var", "with", "yield", "base", "when",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "return", "try", "catch", "finally", "throw", "rethrow", "await", "yield",
        ),
        types = setOf(
            "int", "double", "num", "bool", "String", "List", "Map", "Set", "Iterable", "Future",
            "Stream", "Object", "void", "Never", "dynamic", "Widget", "BuildContext", "State",
        ),
        constants = setOf("true", "false", "null"),
        builtins = setOf("print", "setState", "runApp", "Future", "Stream"),
        docLinePrefixes = listOf("///", "/**"),
    )

    val SCALA = LanguageDefinition(
        id = "scala",
        displayName = "Scala",
        extensions = listOf("scala", "sc"),
        lineComments = listOf("//"),
        blockComments = C_LIKE,
        strings = listOf(
            StringSpec("\"\"\"", multiline = true, escapes = true, interpolator = '$'),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "abstract", "case", "catch", "class", "def", "do", "else", "extends", "final",
            "finally", "for", "forSome", "if", "implicit", "import", "lazy", "match", "new",
            "object", "override", "package", "private", "protected", "return", "sealed", "super",
            "this", "throw", "trait", "try", "type", "val", "var", "while", "with", "yield",
        ),
        controlKeywords = setOf(
            "if", "else", "for", "while", "do", "match", "case", "try", "catch", "finally",
            "return", "throw", "yield",
        ),
        types = setOf(
            "Any", "AnyVal", "AnyRef", "Unit", "Boolean", "Byte", "Short", "Int", "Long", "Float",
            "Double", "Char", "String", "List", "Map", "Set", "Option", "Seq",
        ),
        constants = setOf("true", "false", "null", "None", "Some"),
        builtins = setOf("println", "print", "List", "Map", "Set", "Option"),
    )

    val LUA = LanguageDefinition(
        id = "lua",
        displayName = "Lua",
        extensions = listOf("lua"),
        lineComments = listOf("--"),
        blockComments = listOf(CommentPair("--[[", "]]")),
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN +
            listOf(StringSpec("[[", close = "]]", multiline = true, escapes = false)),
        keywords = setOf("and", "do", "else", "elseif", "end", "for", "function", "goto", "if",
            "in", "local", "not", "or", "repeat", "return", "then", "until", "while"),
        controlKeywords = setOf("if", "elseif", "else", "for", "while", "repeat", "until", "do",
            "return", "break", "goto"),
        constants = setOf("true", "false", "nil"),
        builtins = setOf("print", "pairs", "ipairs", "type", "tostring", "tonumber", "require",
            "setmetatable", "getmetatable", "pcall", "error", "table", "string", "math"),
        indentUnit = "  ",
    )

    val PERL = LanguageDefinition(
        id = "perl",
        displayName = "Perl",
        extensions = listOf("pl", "pm", "t"),
        lineComments = HASH_COMMENT,
        strings = listOf(
            StringSpec("qw", close = ")", multiline = true, escapes = false),
        ) + DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf("my", "our", "local", "sub", "package", "use", "no", "require", "qw",
            "qq", "return", "unless", "until", "while", "foreach", "last", "next", "redo"),
        controlKeywords = setOf("if", "elsif", "else", "unless", "while", "until", "for",
            "foreach", "do", "return", "last", "next", "redo"),
        constants = setOf("undef", "__PACKAGE__", "__FILE__", "__LINE__"),
        builtins = setOf("print", "printf", "push", "pop", "shift", "unshift", "splice", "map",
            "grep", "sort", "join", "split", "die", "warn", "ref", "defined"),
    )

    val HTML = LanguageDefinition(
        id = "html",
        displayName = "HTML",
        extensions = listOf("html", "htm", "xhtml"),
        lineComments = listOf(),
        blockComments = listOf(CommentPair("<!--", "-->")),
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "html", "head", "body", "div", "span", "p", "a", "img", "ul", "ol", "li", "table",
            "tr", "td", "th", "thead", "tbody", "form", "input", "button", "select", "option",
            "label", "header", "footer", "nav", "section", "article", "aside", "main", "script",
            "style", "link", "meta", "title", "h1", "h2", "h3", "h4", "h5", "h6", "canvas",
            "svg", "video", "audio", "iframe", "template", "slot",
        ),
        constants = setOf("true", "false"),
        indentUnit = "  ",
        docLinePrefixes = listOf("<!--"),
    )

    val CSS = LanguageDefinition(
        id = "css",
        displayName = "CSS",
        extensions = listOf("css"),
        lineComments = listOf(),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "important", "media", "import", "charset", "keyframes", "supports", "font-face",
        ),
        controlKeywords = emptySet(),
        types = setOf(
            "display", "position", "top", "left", "right", "bottom", "width", "height", "margin",
            "padding", "border", "background", "color", "font", "flex", "grid", "align",
            "justify", "gap", "overflow", "z-index", "opacity", "transform", "transition",
        ),
        constants = setOf("inherit", "initial", "unset", "auto", "none", "block", "flex", "grid", "absolute", "relative", "fixed"),
        indentUnit = "  ",
    )

    val SCSS = CSS.copy(
        id = "scss",
        displayName = "SCSS",
        extensions = listOf("scss", "sass"),
        keywords = CSS.keywords + setOf(
            "mixin", "include", "extend", "placeholder", "each", "if", "else", "function",
            "return", "use", "forward", "content", "while", "@if", "@else", "@each", "@mixin",
            "@include", "@extend", "@use", "@function", "@return",
        ),
    )

    val JSON = LanguageDefinition(
        id = "json",
        displayName = "JSON",
        extensions = listOf("json", "jsonc", "json5"),
        filenames = listOf("tsconfig.json", "jsconfig.json", "composer.json", "package.json", "package-lock.json"),
        lineComments = listOf("//"),
        strings = DOUBLE_QUOTE_PLAIN,
        keywords = setOf(),
        constants = setOf("true", "false", "null"),
        indentUnit = "  ",
    )

    val XML = LanguageDefinition(
        id = "xml",
        displayName = "XML",
        extensions = listOf("xml", "xsl", "xsd", "svg", "plist", "rss", "pom"),
        filenames = listOf("AndroidManifest.xml", "strings.xml", "pom.xml"),
        lineComments = listOf(),
        blockComments = listOf(CommentPair("<!--", "-->")),
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "manifest", "application", "activity", "intent-filter", "meta-data", "uses-permission",
            "service", "receiver", "provider", "resources", "string", "style", "layout", "item",
            "name", "value", "color", "dimen", "bool", "integer", "vector", "selector", "path",
        ),
        constants = setOf("true", "false"),
        indentUnit = "  ",
        docLinePrefixes = listOf("<!--"),
    )

    val YAML = LanguageDefinition(
        id = "yaml",
        displayName = "YAML",
        extensions = listOf("yml", "yaml"),
        filenames = listOf("docker-compose.yml", "docker-compose.yaml"),
        lineComments = HASH_COMMENT,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(),
        constants = setOf("true", "false", "null", "yes", "no", "on", "off", "~"),
        builtins = setOf("version", "services", "image", "build", "ports", "volumes", "environment",
            "depends_on", "container_name", "restart", "command", "entrypoint"),
        indentStyle = IndentStyle.PYTHON_LIKE,
        indentUnit = "  ",
    )

    val TOML = LanguageDefinition(
        id = "toml",
        displayName = "TOML",
        extensions = listOf("toml"),
        filenames = listOf("Cargo.toml", "pyproject.toml"),
        lineComments = HASH_COMMENT,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf("true", "false"),
        constants = setOf("true", "false"),
        indentUnit = "  ",
    )

    val SQL = LanguageDefinition(
        id = "sql",
        displayName = "SQL",
        extensions = listOf("sql", "ddl", "dml"),
        lineComments = listOf("--"),
        blockComments = C_LIKE,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "select", "insert", "update", "delete", "from", "where", "join", "inner", "left",
            "right", "full", "outer", "cross", "on", "group", "by", "order", "having", "limit",
            "offset", "union", "all", "distinct", "as", "and", "or", "not", "in", "exists",
            "between", "like", "is", "null", "into", "values", "set", "create", "table", "view",
            "index", "drop", "alter", "add", "column", "primary", "key", "foreign", "references",
            "constraint", "default", "cascade", "with", "case", "when", "then", "else", "end",
            "begin", "commit", "rollback", "transaction", "grant", "revoke", "asc", "desc",
        ),
        controlKeywords = setOf("select", "insert", "update", "delete", "where", "join", "group",
            "order", "having", "limit", "case", "when", "then", "else", "begin", "commit", "rollback"),
        types = setOf("int", "integer", "bigint", "smallint", "varchar", "char", "text", "boolean",
            "date", "timestamp", "datetime", "numeric", "decimal", "float", "real", "blob", "uuid"),
        constants = setOf("true", "false", "null", "current_timestamp", "current_date"),
        builtins = setOf("count", "sum", "avg", "min", "max", "coalesce", "cast", "now"),
        indentUnit = "  ",
        caseInsensitiveKeywords = true,
    )

    val SHELL = LanguageDefinition(
        id = "shell",
        displayName = "Shell",
        extensions = listOf("sh", "bash", "zsh", "fish", "ksh"),
        filenames = listOf(".bashrc", ".zshrc", ".profile", "PKGBUILD"),
        lineComments = HASH_COMMENT,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case",
            "esac", "function", "in", "select", "return", "break", "continue", "local",
            "export", "readonly", "declare", "source", "alias", "trap", "shift", "set", "unset",
        ),
        controlKeywords = setOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do",
            "done", "case", "esac", "return", "break", "continue", "exit"),
        constants = setOf("true", "false"),
        builtins = setOf("echo", "cd", "ls", "cat", "grep", "sed", "awk", "cp", "mv", "rm", "mkdir",
            "rmdir", "chmod", "chown", "sudo", "apt", "apt-get", "curl", "wget", "git", "make",
            "docker", "kubectl", "ps", "kill", "tar", "find", "xargs"),
        indentUnit = "  ",
    )

    val MARKDOWN = LanguageDefinition(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf("md", "markdown", "mdx"),
        lineComments = listOf(),
        strings = listOf(
            StringSpec("`", multiline = false, escapes = false),
        ) + DOUBLE_QUOTE,
        keywords = setOf(
            "title", "subtitle", "author", "date", "description", "draft",
        ),
        indentUnit = "  ",
    )

    val DOCKERFILE = LanguageDefinition(
        id = "dockerfile",
        displayName = "Dockerfile",
        extensions = listOf("dockerfile"),
        filenames = listOf("Dockerfile", "Containerfile", ".dockerignore", ".gitignore"),
        lineComments = HASH_COMMENT,
        strings = DOUBLE_QUOTE + SINGLE_QUOTE_PLAIN,
        keywords = setOf(
            "FROM", "RUN", "CMD", "LABEL", "EXPOSE", "ENV", "ADD", "COPY", "ENTRYPOINT",
            "VOLUME", "USER", "WORKDIR", "ARG", "ONBUILD", "STOPSIGNAL", "HEALTHCHECK",
            "SHELL", "AS", "FROM",
        ),
        constants = setOf("true", "false"),
        builtins = setOf("apt-get", "apk", "yum", "curl", "wget", "git", "npm", "yarn", "pnpm", "pip", "python", "node"),
        indentUnit = "  ",
    )

    val GRADLE = KOTLIN.copy(
        id = "gradle",
        displayName = "Gradle",
        extensions = listOf("gradle"),
        filenames = listOf("build.gradle", "settings.gradle", "build.gradle.kts", "settings.gradle.kts"),
    )

    val PLAIN_TEXT = LanguageDefinition(
        id = "plaintext",
        displayName = "Texto plano",
        extensions = listOf("txt", "text", "log", "csv", "tsv", "conf", "cfg", "ini", "env", "properties"),
        lineComments = listOf(),
        strings = listOf(),
        keywords = setOf(),
        indentUnit = "  ",
    )
}
