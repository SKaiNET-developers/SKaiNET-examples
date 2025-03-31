package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkon.kmp.ai.sinus.approximator.ASinusCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.io.Source
import kotlin.math.abs
import kotlin.math.sin

sealed interface ModelLoadingState {
    data object Loading : ModelLoadingState
    data object Success : ModelLoadingState
    data class Error(val message: String) : ModelLoadingState
    data object Initial : ModelLoadingState
}

class SinusSliderViewModel(private val handleSource: () -> Source) : ViewModel() {
    private val calculator = ASinusCalculator(handleSource)
    private val _modelLoadingState = MutableStateFlow<ModelLoadingState>(ModelLoadingState.Initial)
    val modelLoadingState: StateFlow<ModelLoadingState> = _modelLoadingState.asStateFlow()

    var sliderValue by mutableStateOf(0f)
        private set

    var sinusValue by mutableStateOf(0.0)
        private set

    var modelSinusValue by mutableStateOf(0.0)
        private set

    var errorValue by mutableStateOf(0.0)
        private set

    fun updateSliderValue(value: Float) {
        sliderValue = value
        sinusValue = sin(value.toDouble())
        modelSinusValue = calculator.calculate(value.toDouble())
        errorValue = abs(sinusValue - modelSinusValue)
    }

    fun loadModel() {
        viewModelScope.launch {
            _modelLoadingState.value = ModelLoadingState.Loading
            try {
                launch(Dispatchers.IO) {
                    calculator.loadModel()
                }.join()
                _modelLoadingState.value = ModelLoadingState.Success
                // Recalculate values after model is loaded
                updateSliderValue(sliderValue)
            } catch (e: Exception) {
                _modelLoadingState.value = ModelLoadingState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
