# Sinus Approximator (Kotlin Multiplatform Sample)

This project demonstrates a small Kotlin Multiplatform (KMP) app that approximates the sine function with a tiny neural network and visualizes it with Compose Multiplatform UI. The same codebase runs on Android, iOS, WebAssembly (Wasm), Desktop, and can be extended to Server. It also showcases capabilities of the SKaiNET ML framework.

What you can do in the app:
- Adjust sliders to change neural network parameters (e.g., layer sizes, learning steps) and see the approximation change in real time.
- Visualize the target sine curve and the model’s predicted curve.

## Screenshots 

### Android

![Android screenshot placeholder](docs/screenshots/android.png)

### JVM Desktop

![Android screenshot placeholder](docs/screenshots/jvm.png)


### iOS

![iOS screenshot placeholder](docs/screenshots/ios.png)

### WebAssembly (Wasm / Browser)

![Wasm screenshot placeholder](docs/screenshots/wasm.png)

## Project layout
- `/composeApp` — shared UI and platform code for Compose Multiplatform.
  - `commonMain` — code common to all targets (UI, view models, etc.).
  - `androidMain`, `iosMain`, `desktopMain`, `wasmJsMain` — platform-specific glue.
- `/iosApp` — iOS application host (entry point and Xcode project).
- `/shared` — platform-agnostic logic shared across all targets (e.g., ML/Sinus calculator).

## Prerequisite: build and publish SKaiNET to Maven Local
This project depends on SKaiNET libraries that, for now, need to be built locally and published to your Maven Local repository. This will be simpler with the upcoming 0.2.2 release of SKaiNET.

Known working SKaiNET commit: 22cfbbe

Steps:
1. Clone the SKaiNET repository
   - git clone https://github.com/sk-ai-net/SKaiNET.git
   - cd SKaiNET
2. Check out the known good commit
   - git checkout 22cfbbe
3. Build and publish artifacts to Maven Local
   - macOS/Linux: ./gradlew clean publishToMavenLocal -x test
   - Windows:    gradlew.bat clean publishToMavenLocal -x test
4. Verify the artifacts exist (optional)
   - Look under ~/.m2/repository/sk/ainet/core/ (or %USERPROFILE%\.m2\repository\sk\ainet\core on Windows)

Version alignment note:
- This sample currently references SKaiNET version defined in gradle/libs.versions.toml under the key skainet (e.g., 0.2.0).
- If the version you published from SKaiNET differs, either:
  - change the skainet version in gradle/libs.versions.toml to match what you just published, or
  - publish SKaiNET with the same version (if applicable) so the sample can resolve it from mavenLocal.

## How to run
- Desktop (Compose for Desktop): run the `run` configuration under `composeApp/src/desktopMain` or Gradle task `:composeApp:run`.
- Android: open the project in Android Studio and run the `androidApp` target/device.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.
- WebAssembly (browser): `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

## Learn more
- SKaiNET framework: https://github.com/sk-ai-net/SKaiNET


If you face any issues, please report them here: https://github.com/sk-ai-net/skainet-samples/issues