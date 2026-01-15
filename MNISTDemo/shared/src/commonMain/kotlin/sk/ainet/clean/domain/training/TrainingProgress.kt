package sk.ainet.clean.domain.training

data class TrainingProgress(
    val epoch: Int,
    val loss: Float,
    val accuracy: Float = 0f,
    val isCompleted: Boolean = false
)
