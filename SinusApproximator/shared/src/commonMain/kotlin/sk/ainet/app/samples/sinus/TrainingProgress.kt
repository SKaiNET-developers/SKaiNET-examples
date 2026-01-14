package sk.ainet.app.samples.sinus

data class TrainingProgress(
    val epoch: Int,
    val loss: Float,
    val isCompleted: Boolean = false
)
