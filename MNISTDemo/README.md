This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop, Server.

Clean architecture overview (MNIST demo)
- Presentation (UI/ViewModel): Compose screens and view models. UI talks only to use cases, not to model types or storage.
- Domain (Entities + Use Cases + Ports):
  - Entities: image and model identifiers used by the demo (for example, ModelId).
  - Use cases: LoadModel and ClassifyDigit orchestrate model loading and classification.
  - Ports (interfaces): DigitClassifier, ModelWeightsRepository.
- Data (Repositories + Data Sources): ModelWeightsRepositoryImpl composes cache and local resource readers to provide model weights.
- Framework/DI (Platform Adapters): expect/actual ResourceReader and a lightweight ServiceLocator for wiring dependencies.

Entry points for developers
- ServiceLocator: central place to wire platform resources and obtain a classifier.
  - Configure once at app startup: ServiceLocator.configure(resourceReader, digitClassifierFactory)
  - Get a classifier by model id: ServiceLocator.provideDigitClassifier(modelId)
- Use cases:
  - LoadModel(modelWeightsRepository, digitClassifier).invoke(modelId)
  - ClassifyDigit(digitClassifier).invoke(image)

See also
- PRD-clean.md describes the architecture in detail, design goals, and boundaries between layers.
- clean-task.md tracks the implementation checklist and testing/migration items.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here too.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [GitHub](https://github.com/JetBrains/compose-multiplatform/issues).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.