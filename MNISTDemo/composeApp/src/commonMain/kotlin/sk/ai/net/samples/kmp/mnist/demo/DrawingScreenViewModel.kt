package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.di.ServiceLocator
import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import sk.ai.net.samples.kmp.mnist.demo.settings.AppSettings
import kotlinx.io.Source

class DrawingScreenViewModel(handleSource: () -> Source) : ViewModel() {

    // Keep the old parameter for backward compatibility with the UI, but no longer used directly.
    @Suppress("UnusedPrivateMember")
    private val handleSourceFn = handleSource

    // Selected model (default to CNN). Exposed to UI for selection.
    var selectedModelId by mutableStateOf(ModelId.CNN_MNIST)
        private set

    // Available model options for the selector
    val availableModelIds: List<ModelId> = listOf(ModelId.CNN_MNIST, ModelId.MLP_MNIST)

    // Clean-architecture DigitClassifier obtained via ServiceLocator (PRD §6)
    private var classifier: DigitClassifier = ServiceLocator.provideDigitClassifier(selectedModelId)

    init {
        // Sync initial value from AppSettings
        selectedModelId = AppSettings.selectedModelId.value

        // Observe changes from settings and update classifier accordingly
        viewModelScope.launch {
            AppSettings.selectedModelId.collect { newId ->
                if (newId != selectedModelId) {
                    changeModel(newId)
                }
            }
        }
    }

    // Screen mode states
    var isModelLoaded by mutableStateOf(false)
        private set

    // No need for polling; we control the loaded state locally after invoking the use case.
    var isChooseImageMode by mutableStateOf(false)
        private set
    var selectedImageIndex by mutableStateOf(-1)
        private set
    var classificationResult by mutableStateOf<String?>(null)
        private set

    // Drawing states
    var paths by mutableStateOf(listOf<Path>())
        private set
    var currentPath by mutableStateOf<Path?>(null)
        private set
    var currentPathRef by mutableStateOf(1)
        private set
    var lastOffset by mutableStateOf<Offset?>(null)
        private set

    // Load the model for the current selection via the clean DigitClassifier port
    fun loadModel() {
        if (isModelLoaded) return
        viewModelScope.launch {
            try {
                classifier.loadModel(selectedModelId)
                isModelLoaded = true
            } catch (e: Exception) {
                classificationResult = "Model load error: ${e.message}"
            }
        }
    }

    // Change selected model. This resets loaded state and prepares a classifier for the new model.
    fun changeModel(modelId: ModelId) {
        if (selectedModelId == modelId) return
        selectedModelId = modelId
        // New strategy instance for the selected model
        classifier = ServiceLocator.provideDigitClassifier(selectedModelId)
        // Force reload for the new model
        isModelLoaded = false
        classificationResult = null
    }

    // Switch between drawing and image selection modes
    fun switchMode() {
        isChooseImageMode = !isChooseImageMode

        if (isChooseImageMode) {
            selectedImageIndex = -1
        } else {
            clearCanvas()
        }

        classificationResult = null
    }

    // Select an image from the grid
    fun selectImage(index: Int) {
        selectedImageIndex = index
        classificationResult = null
    }

    // Clear the drawing canvas
    fun clearCanvas() {
        paths = emptyList()
        currentPath = null
        currentPathRef = 0
        lastOffset = null
        classificationResult = null
    }

    // Handle drag start event
    fun onDragStart(offset: Offset) {
        currentPath = Path()
        currentPath?.moveTo(offset.x, offset.y)
        currentPathRef += 1
        lastOffset = offset
    }

    // Handle drag event
    fun onDrag(pointerInputChange: PointerInputChange, offset: Offset) {
        if (lastOffset != null) {
            val newOffset = Offset(
                lastOffset!!.x + offset.x,
                lastOffset!!.y + offset.y
            )
            currentPath?.lineTo(newOffset.x, newOffset.y)
            currentPathRef += 1
            lastOffset = newOffset
        }
    }

    // Handle drag end event
    fun onDragEnd() {
        currentPath.let { value ->
            if (value != null) {
                paths = paths + value
                currentPath = null
                currentPathRef = 0
            }
        }
    }

    // Classify the drawn digit or selected image
    fun classify(image: GrayScale28To28Image) {
        if (!isModelLoaded) return

        viewModelScope.launch {
            try {
                val result = classifier.classify(image)
                classificationResult = "Predicted digit: $result"
            } catch (e: Exception) {
                classificationResult = "Classification error: ${e.message}"
                println("Classification error: ${e.message}")
            }
        }
    }

    // Process an ImageBitmap into a GrayScale28To28Image
    private fun processImageBitmap(bitmap: ImageBitmap, output: GrayScale28To28Image) {
        // For now, we'll use a simplified approach
        // In a real implementation, we would extract pixel data from the bitmap
        // and convert it to grayscale

        // Fill with a placeholder pattern for testing
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                // Create a simple pattern (a diagonal line)
                val value = if (x == y || x == 27 - y) 1.0f else 0.0f
                output.setPixel(x, y, value)
            }
        }

        // In the future, implement proper image processing
        // This would involve:
        // 1. Getting pixel data from the bitmap
        // 2. Converting to grayscale
        // 3. Scaling to 28x28
        // 4. Normalizing values between 0 and 1
    }
}
