# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **monorepo** containing sample applications built with the SKaiNET ML framework using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. The applications run on Android, iOS, Desktop (JVM), and WebAssembly (Wasm).

## Sample Applications

### SinusApproximator
Neural network that approximates the sine function with in-app training visualization.

### MNISTDemo
Handwritten digit classifier using CNN/MLP models following clean architecture (domain/data/presentation layers).

## Build Commands

Each sample is a standalone Gradle project. Navigate to the project directory first.

```bash
# Desktop (JVM)
./gradlew :composeApp:run

# Android - use Android Studio or:
./gradlew :composeApp:assembleDebug

# WebAssembly (browser dev server)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# WebAssembly (production build)
./gradlew :composeApp:wasmJsBrowserProductionWebpack

# Run tests
./gradlew :shared:allTests
./gradlew :composeApp:allTests
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode.

## Project Structure (per sample)

```
<Sample>/
├── composeApp/           # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/   # Shared UI, ViewModels
│       ├── androidMain/  # Android entry point
│       ├── desktopMain/  # Desktop entry point
│       ├── iosMain/      # iOS entry point
│       └── wasmJsMain/   # WebAssembly entry point
├── shared/               # Business logic (ML, domain)
│   └── src/
│       ├── commonMain/   # Platform-agnostic code
│       └── commonTest/   # Shared tests
├── iosApp/               # Xcode project wrapper
└── gradle/libs.versions.toml  # Version catalog
```

## Key Dependencies

All SKaiNET dependencies follow the pattern `sk.ainet.core:skainet-*`:
- `skainet-lang-core`, `skainet-lang-models` - Model definition DSL
- `skainet-compile-core`, `skainet-compile-dag` - Model compilation
- `skainet-backend-cpu` - CPU inference engine
- `skainet-backend-cpu-jvm` - JVM-optimized ops (desktop only)
- `skainet-data-api`, `skainet-data-basic` - Data handling

## Architecture Notes

### MNISTDemo Clean Architecture
- **ServiceLocator** (`shared/src/commonMain/.../di/ServiceLocator.kt`): Central DI - configure with platform `ResourceReader` and `DigitClassifierFactory` at app startup
- **Domain ports**: `DigitClassifier`, `ModelWeightsRepository` - interfaces for classification and weight loading
- **Use cases**: `LoadModel`, `ClassifyDigit` - orchestrate model loading and inference
- **Data layer**: Repository pattern with cache/local data sources

### SinusApproximator
- `SinusCalculator` interface with `MLPSinusCalculator` implementation
- `SinusTrainer` for in-app model training with progress callbacks

## Git Workflow

This project uses GitFlow:
- `main`: Production releases only
- `develop`: Integration branch (PR target)
- `feature/*`: New features branch from `develop`
- `release/*`: Release preparation
- `hotfix/*`: Critical fixes from `main`

See `GITFLOW.adoc` for detailed workflow documentation.

## CI/CD

GitHub Actions deploys WebAssembly builds to GitHub Pages on tag push. Each sample has a `webapp.json` defining:
- Build command
- Distribution directories
- App metadata for the landing page
