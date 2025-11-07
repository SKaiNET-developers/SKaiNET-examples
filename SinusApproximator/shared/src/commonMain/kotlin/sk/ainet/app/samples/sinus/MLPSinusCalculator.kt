package sk.ainet.app.samples.sinus

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.data
import sk.ainet.execute.context.computation
import sk.ainet.lang.model.dnn.mlp.SinusApproximator
import sk.ainet.lang.tensor.dsl.tensor
import sk.ainet.lang.types.FP32

class MLPSinusCalculator() : SinusCalculator {
    private val ctx = DirectCpuExecutionContext()
    val model = SinusApproximator().model(ctx)


    override fun calculate(angle: Float): Float =
        computation<Float>(ctx) { computation ->
            // Create a simple input tensor compatible with the model's expected input size (1)
            val inputTensor = data<FP32, Float>(ctx) {
                tensor<FP32, Float>() {
                    // Using shape(1, 1) to represent a single scalar input in 2D form
                    shape(1, 1) {
                        fromArray(
                            floatArrayOf(angle)
                        )
                    }
                }
            }
            model(inputTensor).data[0, 0]
        }


    override suspend fun loadModel() {
        // TODO model has pretrained weights as a part of model
    }
}


