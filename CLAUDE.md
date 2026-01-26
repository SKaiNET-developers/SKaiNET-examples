# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SKaiNET-examples is a Kotlin Multiplatform (KMP) repository containing example applications demonstrating the SKaiNET deep learning framework. The project showcases cross-platform development with Compose Multiplatform for UI across Android, iOS, Desktop (JVM), and Web (Wasm/JS).

## Build Commands

Each demo is a standalone Gradle project. Run commands from within the specific demo directory (e.g., `MNISTDemo/`, `SinusApproximator/`, `KllamaDemo/`).

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

### Web/WebAssembly
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # Dev server with hot reload
./gradlew :composeApp:wasmJsBrowserProductionWebpack # Production build
```

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run.

### Server (KllamaDemo only)
```bash
./gradlew :server:run
```

### CLI (MNISTDemo only)
```bash
./gradlew :cli:run
```

## Testing

```bash
./gradlew :shared:allTests   # All shared module tests
./gradlew :cli:test          # CLI module tests (MNISTDemo)
./gradlew :server:test       # Server tests (KllamaDemo)
```

Tests use `kotlin.test` with JUnit. Test files are in `src/*/test/kotlin/`.

## Architecture

### Demo Projects

- **MNISTDemo**: Digit classification using pre-trained models. Demonstrates clean architecture with domain/data/presentation layers and ServiceLocator for DI.
- **SinusApproximator**: Real-time neural network training UI showing sine function approximation with live visualization.
- **KllamaDemo**: LLM chat application with Ktor server backend.

### SKaiNET Execution Contexts

**DirectCpuExecutionContext** - Use for inference:
- Fast execution without gradient recording
- Phase.EVAL by default

**DefaultGraphExecutionContext** - Use for training:
- Enables automatic differentiation
- Phase.TRAIN records operations on tape for backpropagation
- Requires `DefaultGradientTape` factory

### Module Structure (per demo)

- `/composeApp` - Compose Multiplatform UI
- `/shared` - Common business logic and models
- `/cli` - Command-line interface (MNISTDemo)
- `/server` - Ktor server (KllamaDemo)
- `/iosApp` - iOS Xcode project

## Key Dependencies

- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **SKaiNET**: 0.9.0
- **JVM Toolchain**: 21
- **AGP**: 8.12.3

Dependencies managed via `gradle/libs.versions.toml`.

## Git Workflow

Uses GitFlow branching model:
- `main` - Production-ready
- `develop` - Integration branch (main branch for PRs)
- `feature/*` - Feature development
- `release/*` - Release preparation
- `hotfix/*` - Critical production fixes

See `GITFLOW.adoc` for detailed workflow documentation.

## CI/CD

GitHub Actions (`.github/workflows/deploy.yml`) builds and deploys web apps to GitHub Pages on tag push. Each demo has a `webapp.json` specifying build configuration.
