# Sinus Approximator (Kotlin Multiplatform Sample)

This project demonstrates a small Kotlin Multiplatform (KMP) app that approximates the sine function with a tiny neural network and visualizes it with Compose Multiplatform UI. The same codebase runs on Android, iOS, WebAssembly (Wasm), Desktop, and can be extended to Server. It also showcases capabilities of the SKaiNET ML framework.

What you can do in the app:
- Adjust sliders to change input value(angle) and see the result of the approximation in real time.
- Visualize the target sine curve and the model’s predicted curve.
- Visualization of Neural network architecture

## Screenshots

| Platform | Screenshot                                                                      |
| --- |---------------------------------------------------------------------------------|
| Android | <img src="docs/screenshots/android.png" alt="Android screenshot" width="640" /> |
| JVM Desktop | ![JVM Desktop screenshot](docs/screenshots/jvm.png)                             |
| iOS | <img src="docs/screenshots/ios.png" alt="iOS screenshot" width="640" />         |
| WebAssembly (Wasm / Browser) | ![Wasm screenshot](docs/screenshots/wasm.png)                                   |

## Project layout
- `/composeApp` — shared UI and platform code for Compose Multiplatform.
  - `commonMain` — code common to all targets (UI, view models, etc.).
  - `androidMain`, `iosMain`, `desktopMain`, `wasmJsMain` — platform-specific glue.
- `/iosApp` — iOS application host (entry point and Xcode project).
- `/shared` — platform-agnostic logic shared across all targets (e.g., ML/Sinus calculator).

## Prerequisite: build and publish SKaiNET to Maven Local
This project depends on SKaiNET libraries that, for now, need to be built locally and published to your Maven Local repository. This will be simpler with the upcoming 0.2.2 release of SKaiNET.

Known working SKaiNET commit: 22cfbbe

Before you start
- Require Git and JDK 17+ installed (Gradle wrapper will download Gradle automatically).
- Close any IDE builds of this project while publishing, to avoid file locks on Windows.

Option A — macOS/Linux (Terminal)
Copy–paste these lines into your terminal:
```bash
git clone https://github.com/sk-ai-net/SKaiNET.git
cd SKaiNET
git checkout 22cfbbe
./gradlew clean publishToMavenLocal -x test
```

Option B — Windows (PowerShell or CMD)
Copy–paste these lines into PowerShell or CMD:
```bat
git clone https://github.com/sk-ai-net/SKaiNET.git
cd SKaiNET
git checkout 22cfbbe
gradlew.bat clean publishToMavenLocal -x test
```

Verify (optional)
- Check that artifacts are present in your local Maven repository:
  - macOS/Linux: `~/.m2/repository/sk/ainet/core/`
  - Windows: `%USERPROFILE%\.m2\repository\sk\ainet\core\`

Version alignment note:
- This sample uses the SKaiNET version defined in `gradle/libs.versions.toml` under the key `skainet` (for example `0.2.0`).
- If the version you published from SKaiNET differs, either:
  - change the `skainet` version in `gradle/libs.versions.toml` to match what you just published, or
  - publish SKaiNET using that same version so this sample can resolve it from `mavenLocal()`.

## How to run
- Desktop (Compose for Desktop): run the `run` configuration under `composeApp/src/desktopMain` or Gradle task `:composeApp:run`.
- Android: open the project in Android Studio and run the `androidApp` target/device.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.
- WebAssembly (browser): `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

## Learn more
- SKaiNET framework: https://github.com/sk-ai-net/SKaiNET


If you face any issues, please report them here: https://github.com/sk-ai-net/skainet-samples/issues