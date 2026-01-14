package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import sk.ainet.app.samples.sinus.SinusTrainer
import sk.ainet.app.samples.sinus.TrainedSinusCalculator

data class TrainingState(
    val epoch: Int = 0,
    val totalEpochs: Int = 1000,
    val currentLoss: Float = 0f,
    val isTraining: Boolean = false,
    val isCompleted: Boolean = false
)

class SinusTrainingViewModel : ViewModel() {
    private val _trainingState = MutableStateFlow(TrainingState())
    val trainingState: StateFlow<TrainingState> = _trainingState.asStateFlow()

    private val trainer = SinusTrainer()
    val trainedCalculator = TrainedSinusCalculator(trainer.getModel())

    fun startTraining() {
        if (_trainingState.value.isTraining) return

        viewModelScope.launch(Dispatchers.Default) {
            _trainingState.value = _trainingState.value.copy(isTraining = true, isCompleted = false)

            trainer.train(epochs = _trainingState.value.totalEpochs)
                .conflate()
                .collect { progress ->
                    _trainingState.value = _trainingState.value.copy(
                        epoch = progress.epoch,
                        currentLoss = progress.loss,
                        isCompleted = progress.isCompleted,
                        isTraining = !progress.isCompleted
                    )
                }
        }
    }
}
