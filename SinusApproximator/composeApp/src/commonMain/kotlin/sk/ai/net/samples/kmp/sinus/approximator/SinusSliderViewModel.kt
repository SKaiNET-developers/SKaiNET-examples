package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sk.ainet.app.samples.sinus.KanSinusCalculator
import sk.ainet.app.samples.sinus.MLPSinusCalculator
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

sealed interface ModelLoadingState {
    data object Loading : ModelLoadingState
    data object Success : ModelLoadingState
    data class Error(val message: String) : ModelLoadingState
    data object Initial : ModelLoadingState
}

class SinusSliderViewModel() : ViewModel() {
    private val calculator = MLPSinusCalculator()
    private val kanCalculator = KanSinusCalculator()

    private val _modelLoadingState = MutableStateFlow<ModelLoadingState>(ModelLoadingState.Initial)
    val modelLoadingState: StateFlow<ModelLoadingState> = _modelLoadingState.asStateFlow()

    // Expose the neural network model (use KAN calculator model)
    val neuralNetworkModel get() = kanCalculator.model

    var sliderValue by mutableStateOf(0f)
        private set

    var sinusValue by mutableStateOf(0.0)
        private set

    // For backward compatibility (kept but not used by UI anymore). Defaults to KAN value.
    var modelSinusValue by mutableStateOf(0.0f)
        private set

    // Both models at once
    var modelSinusValueKan by mutableStateOf(0.0f)
        private set

    var modelSinusValueMlp by mutableStateOf(0.0f)
        private set

    var errorValue by mutableStateOf(0.0)
        private set

    var errorValueKan by mutableStateOf(0.0)
        private set

    var errorValueMlp by mutableStateOf(0.0)
        private set

    // Formatted values for display
    var formattedAngle by mutableStateOf("0.0000")
        private set

    var formattedSinusValue by mutableStateOf("0.000000")
        private set

    var formattedModelSinusValue by mutableStateOf("0.000000")
        private set

    var formattedErrorValue by mutableStateOf("0.000000")
        private set

    // New formatted values for dual display
    var formattedModelSinusValueKan by mutableStateOf("0.000000")
        private set

    var formattedModelSinusValueMlp by mutableStateOf("0.000000")
        private set

    var formattedErrorValueKan by mutableStateOf("0.000000")
        private set

    var formattedErrorValueMlp by mutableStateOf("0.000000")
        private set

    private fun Double.formatDecimal(decimals: Int): String {
        val factor = 10.0.pow(decimals.toDouble())
        return (round(this * factor) / factor).toString()
    }

    private fun Float.formatDecimal(decimals: Int): String {
        return this.toDouble().formatDecimal(decimals)
    }

    private fun updateFormattedValues() {
        formattedAngle = sliderValue.formatDecimal(4)
        formattedSinusValue = sinusValue.formatDecimal(6)
        formattedModelSinusValue = modelSinusValue.formatDecimal(6)
        formattedErrorValue = errorValue.formatDecimal(6)

        // New ones
        formattedModelSinusValueKan = modelSinusValueKan.formatDecimal(6)
        formattedModelSinusValueMlp = modelSinusValueMlp.formatDecimal(6)
        formattedErrorValueKan = errorValueKan.formatDecimal(6)
        formattedErrorValueMlp = errorValueMlp.formatDecimal(6)
    }

    fun updateSliderValue(value: Float) {
        sliderValue = value
        sinusValue = sin(value.toDouble())
        // Compute both models
        modelSinusValueKan = kanCalculator.calculate(value)
        modelSinusValueMlp = calculator.calculate(value)

        // Keep legacy fields aligned to KAN for compatibility
        modelSinusValue = modelSinusValueKan

        // Errors
        errorValueKan = abs(sinusValue - modelSinusValueKan)
        errorValueMlp = abs(sinusValue - modelSinusValueMlp)
        // Legacy single error equals KAN error for now
        errorValue = errorValueKan
        updateFormattedValues()
    }

    fun loadModel() {
        viewModelScope.launch {
            _modelLoadingState.value = ModelLoadingState.Loading
            try {
                // Load both models (currently no-ops as they are preloaded with weights)
                kanCalculator.loadModel()
                calculator.loadModel()
                _modelLoadingState.value = ModelLoadingState.Success
                // Recalculate values after model is loaded
                updateSliderValue(sliderValue)
            } catch (e: Exception) {
                _modelLoadingState.value = ModelLoadingState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
