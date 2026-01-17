# Training Tasks Documentation

## Task 1: ViewModel for Model Training
- Created `SinusTrainingViewModel` to manage the training lifecycle.
- Implemented `TrainingState` to track epoch, loss, and training status.
- Used `viewModelScope` with `Dispatchers.Default` for non-blocking training.

## Task 2: Training Screen Visualization
- Created `SinusTrainingScreen` with:
    - Progress tracking (LinearProgressIndicator).
    - Loss display.
    - Real-time model visualization using `SinusVisualization`.
- Reused existing plot component for consistency.

## Task 3: App Integration
- Updated `App.kt` to include a `Scaffold` with `NavigationBar`.
- Integrated `SinusSliderScreen` and `SinusTrainingScreen` as separate tabs.
- Shared `SinusTrainingViewModel` across screens to show trained model results in real-time.

## Task 4: 4th Curve for Trained Model
- Updated `SinusVisualization` to support an optional 4th curve (`approximatedSinusTrained`).
- Added `TrainedSinusCalculator` to bridge the raw `Module` and the calculator interface.
- Displayed the trained model's approximation in both Slider and Training screens.
