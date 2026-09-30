# CodeForge IDE

Un IDE de código multilenguaje para Android, escrito en Kotlin y Jetpack Compose.

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-blue) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM2024-blue) ![Gradle](https://img.shields.io/badge/Gradle-8.7-green) ![AGP](https://img.shields.io/badge/AGP-8.5.2-green)

## Qué hace

- **Editor** con resaltado de sintaxis en tiempo real, numeración de líneas y auto-indentación.
- **Explorador de archivos** con creación, renombrado y borrado.
- **Pestañas** multitarea.
- **Panel de diagnósticos** con errores de sintaxis (comentarios sin cerrar, delimitadores desbalanceados).
- **Consola y barra de estado** con el lenguaje y la posición del cursor.
- **Tema claro/oscuro** según el sistema.

## Lenguajes soportados

Kotlin, Java, JavaScript, TypeScript, Python, Go, Rust, C, C++, C#, Swift, Ruby, PHP, Shell, SQL, JSON, YAML, HTML, CSS, SCSS, Markdown, XML, Dart, Kotlin Script, Lua, Perl, R, TOML e INI.

Se añaden en `Languages.kt` mediante un `LanguageDefinition` (comentarios de línea/bloque, delimitadores de cadena, palabras clave, plantillas, estilo de indentación, regex de números y comentarios de documentación).

## Arquitectura

```
app/src/main/java/dev/codeforge/ide/
├── MainActivity.kt
├── language/
│   ├── LanguageDefinition.kt   modelo de datos del lenguaje
│   ├── Languages.kt            registro de ~30 lenguajes
│   ├── Highlighter.kt          tokenizador + diagnósticos (sin dependencias Android)
│   ├── SyntaxTheme.kt          paleta de colores por token
│   └── SyntaxPalette.kt        mapeo token -> color
├── ui/
│   ├── IdeScreen.kt            pantalla principal
│   ├── IdeViewModel.kt         estado y operaciones
│   ├── CodeEditor.kt           editor Compose
│   ├── EditorActions.kt        autocierre e indentación
│   ├── ExplorerPanel.kt        árbol de archivos
│   └── IdeUiState.kt           modelo de estado
└── workspace/
    ├── FileNode.kt
    └── Workspace.kt
```

`language/` es **Kotlin puro sin dependencias de Android**, lo que permite testearlo en la JVM sin instrumentación.

## Tests

57 tests JVM:

| Suite | Tests | Cubre |
|---|---|---|
| `HighlighterTest` | 41 | tokenización por lenguaje, comentarios, cadenas, plantillas, diagnósticos |
| `EditorActionsTest` | 16 | autocierre de delimitadores, auto-indentación, toggle de comentarios |

```bash
./gradlew testDebugUnitTest
```

## Compilar el APK

El build requiere JDK 17 y Android SDK (API 34). El `gradle-wrapper` incluido se encarga de Gradle 8.7.

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/
./gradlew assembleRelease   # app/build/outputs/apk/release/
```

### Build en la nube (Codemagic)

`codemagic.yaml` ejecuta los tests y genera ambos APKs. Tras conectar el repositorio en Codemagic, cada push dispara el workflow `build_android` y los APKs quedan disponibles como artefactos.

> En hosts ARM64 (Apple Silicon) el build local no funciona: `aapt2` solo se distribuye para x86-64. Codemagic usa runners x86-64, por eso el APK se compila allí.

## Requisitos

- Android Studio Ladybug o superior, o JDK 17 + Android SDK 34
- `minSdk` 26 · `targetSdk` 34

## Estado

MVP. Incluye edición, resaltado, navegación y diagnósticos estáticos. Pendiente: motor de compilación por lenguaje (LSP), terminal integrada, búsqueda global y refactorización.
