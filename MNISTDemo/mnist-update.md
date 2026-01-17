# MNIST Demo Update Plan (v0.3.0 → v0.7.1)

## Overview

Update MNISTDemo to current SKaiNET library versions and add training capabilities similar to SinusApproximator.

## Current State Analysis

### Version Comparison

| Component | MNISTDemo (current) | SinusApproximator (target) |
|-----------|---------------------|---------------------------|
| SKaiNET | 0.3.0 | 0.7.1 |
| Kotlin | 2.2.21 | 2.3.0 |
| Compose Multiplatform | 1.9.3 | 1.10.0 |
| JVM Toolchain | 11 | 21 |
| androidx-activityCompose | 1.12.0 | 1.12.2 |

### Missing SKaiNET Modules

MNISTDemo lacks these modules available in 0.7.1:
- `skainet-compile-dag` - DAG compilation support
- `skainet-io-core` - I/O utilities
- `skainet-io-gguf` - GGUF model format support
- `skainet-io-onnx` - ONNX model format support
- `skainet-lang-dag` - DAG language constructs

### Current Issues

1. **DummyInferenceModule**: `CompatDigitClassifier.kt` uses placeholder inference (returns checksum-based digit)
2. **No training support**: Only inference with pre-trained weights
3. **Backward-compat shims**: Complex type aliasing between old/new APIs
4. **Outdated JVM target**: JVM 11 vs modern JVM 21

---

## Implementation Tasks

### Phase 1: Library Updates

#### Task 1.1: Update `gradle/libs.versions.toml`

Update versions to match SinusApproximator:

```toml
[versions]
kotlin = "2.3.0"
composeMultiplatform = "1.10.0"
skainet = "0.7.1"
androidx-activityCompose = "1.12.2"
```

Add new SKaiNET library entries:

```toml
skainet-compile-dag = { module = "sk.ainet.core:skainet-compile-dag", version.ref = "skainet" }
skainet-io-core = { module = "sk.ainet.core:skainet-io-core", version.ref = "skainet" }
skainet-io-gguf = { module = "sk.ainet.core:skainet-io-gguf", version.ref = "skainet" }
skainet-io-onnx = { module = "sk.ainet.core:skainet-io-onnx", version.ref = "skainet" }
skainet-lang-dag = { module = "sk.ainet.core:skainet-lang-dag", version.ref = "skainet" }
```

#### Task 1.2: Update `shared/build.gradle.kts`

- Set `jvmToolchain(21)`
- Add new SKaiNET dependencies
- Update Android compile options to Java 21
- Add `js` target if missing (for parity with SinusApproximator)

#### Task 1.3: Update `composeApp/build.gradle.kts`

- Update JVM target configuration
- Add desktop JVM-specific optimizations (`skainet-backend-cpu-jvm`)

#### Task 1.4: Build Verification

- Run `./gradlew :shared:allTests`
- Run `./gradlew :composeApp:run` (Desktop)
- Verify Wasm build: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

---

### Phase 2: Training Infrastructure

#### Task 2.1: Create Training Data Types

Create `shared/src/commonMain/kotlin/sk/ainet/clean/domain/training/TrainingProgress.kt`:

```kotlin
package sk.ainet.clean.domain.training

data class TrainingProgress(
    val epoch: Int,
    val totalEpochs: Int,
    val loss: Float,
    val accuracy: Float,
    val isCompleted: Boolean = false
)
```

#### Task 2.2: Create MnistTrainer

Create `shared/src/commonMain/kotlin/sk/ainet/clean/domain/training/MnistTrainer.kt`:

**Key components (following SinusApproximator pattern):**

```kotlin
class MnistTrainer {
    private val baseCtx = DirectCpuExecutionContext()
    private val trainCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.TRAIN,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )

    // CNN model definition using sequential DSL
    private val model = sequential<FP32, Float>(trainCtx) {
        input(784)  // 28x28 flattened
        dense(128, "fc1") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(64, "fc2") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(10, "fc3") { weights { randn(std = 0.1f) } }
    }

    fun train(
        trainImages: List<FloatArray>,
        trainLabels: List<Int>,
        epochs: Int = 10,
        batchSize: Int = 32,
        lr: Double = 0.01
    ): Flow<TrainingProgress>
}
```

#### Task 2.3: Create MNIST Data Loader

Create `shared/src/commonMain/kotlin/sk/ainet/clean/data/mnist/MnistDataLoader.kt`:

- Load MNIST images from resources
- Convert to normalized float arrays
- Support batching for training

---

### Phase 3: Training UI

#### Task 3.1: Create MnistTrainingViewModel

Create `composeApp/src/commonMain/kotlin/.../MnistTrainingViewModel.kt`:

**Pattern from SinusApproximator:**

```kotlin
data class MnistTrainingState(
    val epoch: Int = 0,
    val totalEpochs: Int = 10,
    val currentLoss: Float = 0f,
    val currentAccuracy: Float = 0f,
    val lossHistory: List<Float> = emptyList(),
    val accuracyHistory: List<Float> = emptyList(),
    val isTraining: Boolean = false,
    val isCompleted: Boolean = false
)

class MnistTrainingViewModel : ViewModel() {
    private val _trainingState = MutableStateFlow(MnistTrainingState())
    val trainingState: StateFlow<MnistTrainingState> = _trainingState.asStateFlow()

    private val trainer = MnistTrainer()

    fun startTraining() { ... }
    fun stopTraining() { ... }
}
```

#### Task 3.2: Create MnistTrainingScreen

Create `composeApp/src/commonMain/kotlin/.../screens/TrainingScreen.kt`:

**UI Components:**

- Training controls (Start/Stop buttons)
- Epoch progress indicator
- Loss chart visualization
- Accuracy chart visualization
- Sample predictions preview during training

#### Task 3.3: Update Navigation

Update `navigation/Navigation.kt`:

- Add `Screen.TRAINING` enum value
- Add training tab to NavigationBar/NavigationRail
- Wire TrainingScreen into NavigationHost

---

### Phase 4: Inference Integration

#### Task 4.1: Replace DummyInferenceModule

Update classifier implementations to use real SKaiNET inference:

- Create `RealCnnInferenceModule` using compiled model
- Create `RealMlpInferenceModule` for MLP variant
- Support loading GGUF weights via `skainet-io-gguf`

#### Task 4.2: Update DigitClassifierFactoryImpl

Connect factory to real inference modules:

```kotlin
class DigitClassifierFactoryImpl : DigitClassifierFactory {
    override fun create(modelId: ModelId): DigitClassifier {
        val module = when (modelId) {
            ModelId.CNN_MNIST -> RealCnnInferenceModule()
            ModelId.MLP_MNIST -> RealMlpInferenceModule()
            else -> error("Unknown model")
        }
        return CnnDigitClassifier(module, repository)
    }
}
```

#### Task 4.3: Add Trained Model Support

- Allow using in-app trained model for inference
- Add model export/save functionality (optional)

---

### Phase 5: Cleanup

#### Task 5.1: Remove Compatibility Shims

- Remove `CompatDigitClassifier.kt` backward-compat code
- Consolidate type definitions
- Clean up unused imports

#### Task 5.2: Update Tests

- Update shared module tests for new APIs
- Add training integration tests
- Verify inference accuracy tests

#### Task 5.3: Documentation

- Update README.md with training instructions
- Document model architecture choices
- Add CHANGELOG.md entry

---

## File Changes Summary

### Modified Files

| File | Changes |
|------|---------|
| `gradle/libs.versions.toml` | Update versions, add new libraries |
| `shared/build.gradle.kts` | JVM 21, new dependencies |
| `composeApp/build.gradle.kts` | JVM updates, desktop optimizations |
| `navigation/Navigation.kt` | Add Training screen |
| `App.kt` | Add Training tab |

### New Files

| File | Purpose |
|------|---------|
| `domain/training/TrainingProgress.kt` | Training state data class |
| `domain/training/MnistTrainer.kt` | Core training logic |
| `data/mnist/MnistDataLoader.kt` | MNIST data handling |
| `MnistTrainingViewModel.kt` | Training UI state |
| `screens/TrainingScreen.kt` | Training UI |

### Files to Remove/Refactor

| File | Action |
|------|--------|
| `CompatDigitClassifier.kt` | Remove after migration |

---

## Dependencies Between Tasks

```
Phase 1 (Library Updates)
    │
    ├── Task 1.1 → Task 1.2 → Task 1.3 → Task 1.4
    │
    ▼
Phase 2 (Training Infrastructure)
    │
    ├── Task 2.1 ──┬── Task 2.2
    │              │
    │              └── Task 2.3
    ▼
Phase 3 (Training UI)
    │
    ├── Task 3.1 → Task 3.2 → Task 3.3
    │
    ▼
Phase 4 (Inference Integration)
    │
    ├── Task 4.1 → Task 4.2 → Task 4.3
    │
    ▼
Phase 5 (Cleanup)
    │
    └── Task 5.1, 5.2, 5.3 (parallel)
```

---

## Risk Considerations

1. **API Breaking Changes**: SKaiNET 0.3.0 → 0.7.1 may have breaking API changes requiring code adaptation
2. **Training Performance**: MNIST training on mobile/Wasm may be slow; consider limiting epochs or providing progress feedback
3. **Memory Usage**: Training requires more memory than inference; monitor for OOM on constrained platforms
4. **iOS Build**: Training may require iOS-specific optimizations

---

## Success Criteria

- [ ] All library versions updated to match SinusApproximator
- [ ] Desktop build runs successfully
- [ ] WebAssembly build works in browser
- [ ] Android build compiles (test on device)
- [ ] Training screen shows real-time progress
- [ ] Trained model can classify digits
- [ ] Existing inference functionality preserved
