package sk.ainet.app.samples.sinus


interface SinusCalculator {
    fun calculate(angle: Float): Float
    suspend fun loadModel()
}
