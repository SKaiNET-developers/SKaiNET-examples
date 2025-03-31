package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kkon.kmp.ai.sinus.approximator.ASinusCalculator
import kotlinx.coroutines.*
import kotlinx.io.Source
import kotlin.math.sin

class SinusSliderViewModel(private val handleSource: () -> Source) {
    private val viewModelScope = CoroutineScope(Dispatchers.Default)
    private val calculator = ASinusCalculator(handleSource)

    var sliderValue by mutableStateOf(0f)
        private set

    var isModelLoaded by mutableStateOf(false)
        private set

    var sinusValue by mutableStateOf(0.0)
        private set

    var modelSinusValue by mutableStateOf(0.0)
        private set

    fun updateSliderValue(value: Float) {
        sliderValue = value
        sinusValue = sin(value.toDouble())
        modelSinusValue = calculator.calculate(value.toDouble())
    }

    fun loadModel() {
        viewModelScope.launch {
            calculator.loadModel()
            isModelLoaded = true
        }
    }
}
