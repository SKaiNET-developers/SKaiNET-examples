package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sk.ai.net.nn.Module
import com.kkon.kmp.ai.sinus.approximator.ASinusCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import kotlinx.io.Source
import sk.ai.net.Shape
import sk.ai.net.io.csv.CsvParametersLoader
import sk.ai.net.io.mapper.NamesBasedValuesModelMapper
import sk.ai.net.nn.reflection.flattenParams
import sk.ai.net.nn.reflection.summary
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

class SinusSliderViewModel(private val handleSource: () -> Source) : ViewModel() {
    private val calculator = ASinusCalculator(::handleModelLoad)
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

    // Formatted values for display
    var formattedAngle by mutableStateOf("0.0000")
        private set

    var formattedSinusValue by mutableStateOf("0.000000")
        private set

    var formattedModelSinusValue by mutableStateOf("0.000000")
        private set

    var formattedErrorValue by mutableStateOf("0.000000")
        private set

    private fun Double.formatDecimal(decimals: Int): String {
        val factor = 10.0.pow(decimals.toDouble())
        return (round(this * factor) / factor).toString()
    }

    private fun Float.formatDecimal(decimals: Int): String {
        return this.toDouble().formatDecimal(decimals)
    }

    private fun handleModelLoad(model:Module) {
        print(model.summary(Shape(1)))
        val parametersLoader = CsvParametersLoader(handleSource)

        val mapper = NamesBasedValuesModelMapper()
        print(model.summary(Shape(1)))

        CoroutineScope(Dispatchers.IO).launch {
            parametersLoader.load { name, shape ->
                mapper.mapToModel(model, mapOf(name to shape))
            }
            val params = flattenParams(model)
            println(params)
        }

    }

    private fun updateFormattedValues() {
        formattedAngle = sliderValue.formatDecimal(4)
        formattedSinusValue = sinusValue.formatDecimal(6)
        formattedModelSinusValue = modelSinusValue.formatDecimal(6)
        formattedErrorValue = errorValue.formatDecimal(6)
    }

    fun updateSliderValue(value: Float) {
        sliderValue = value
        sinusValue = sin(value.toDouble())
        modelSinusValue = calculator.calculate(value.toDouble())
        errorValue = abs(sinusValue - modelSinusValue)
        updateFormattedValues()
    }

    fun loadModel() {
        viewModelScope.launch {
            _modelLoadingState.value = ModelLoadingState.Loading
            try {
                launch(Dispatchers.Default) {
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
