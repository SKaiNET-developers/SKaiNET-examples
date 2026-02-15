package sk.ainet.app.samples.sinus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.Phase
import sk.ainet.lang.graph.DefaultGradientTape
import sk.ainet.lang.graph.DefaultGraphExecutionContext
import sk.ainet.lang.nn.dsl.sequential
import sk.ainet.lang.nn.dsl.training
import sk.ainet.lang.nn.loss.MSELoss
import sk.ainet.lang.nn.optim.adamw
import sk.ainet.lang.tensor.relu
import sk.ainet.lang.types.FP32
import kotlin.math.PI
import kotlin.math.sin

class SinusTrainer {
    private val baseCtx = DirectCpuExecutionContext()
    private val trainCtx = DefaultGraphExecutionContext(
        baseOps = baseCtx.ops,
        phase = Phase.TRAIN,
        createTapeFactory = { _ -> DefaultGradientTape() }
    )

    private val model = sequential<FP32, Float>(trainCtx) {
        input(1)
        dense(16, "linear-1") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(16, "linear-2") { weights { randn(std = 0.1f) } }
        activation { it.relu() }
        dense(1, "linear-3") { weights { randn(std = 0.1f) } }
    }
    
    fun getModel() = model

    fun train(epochs: Int = 1000, batchSize: Int = 64, lr: Double = 0.001): Flow<TrainingProgress> = flow {
        val xValues = FloatArray(batchSize) { i ->
            (i.toFloat() / (batchSize - 1)) * (PI.toFloat() / 2f)
        }
        val yValues = FloatArray(batchSize) { i -> sin(xValues[i].toDouble()).toFloat() }

        val inputs = baseCtx.fromFloatArray<FP32, Float>(sk.ainet.lang.tensor.Shape(batchSize, 1), FP32::class, xValues)
        val targets = baseCtx.fromFloatArray<FP32, Float>(sk.ainet.lang.tensor.Shape(batchSize, 1), FP32::class, yValues)

        val runner = training<FP32, Float> {
            model { model }
            loss { MSELoss() }
            optimizer {
                adamw(lr = lr, weightDecay = 0.01).apply {
                    model.trainableParameters().forEach {
                        addParameter(it)
                    }
                }
            }
        }

        val dataset = listOf(inputs to targets)

        for (epoch in 1..epochs) {
            var totalLoss = 0f
            for ((x, y) in dataset) {
                val lossTensor = runner.step(trainCtx, x, y)
                totalLoss += (lossTensor.data.get() as Float)
            }

            val averageLoss = totalLoss / dataset.size
            if (epoch % 5 == 0 || epoch == 1 || epoch == epochs) {
                emit(TrainingProgress(epoch, averageLoss, isCompleted = (epoch == epochs)))
                yield()
            }
        }
    }
}
