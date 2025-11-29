package sk.ainet.clean.domain.factory

import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier
import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.domain.port.ModelWeightsRepository
import sk.ainet.clean.domain.strategy.CnnDigitClassifier
import sk.ainet.clean.domain.strategy.MlpDigitClassifier

/**
 * Concrete implementation of DigitClassifierFactory (PRD §3).
 * Uses providers to obtain proper InferenceModule per strategy and wires repository.
 */
class DigitClassifierFactoryImpl(
    private val repository: ModelWeightsRepository,
    private val cnnModuleProvider: () -> InferenceModule,
    private val mlpModuleProvider: () -> InferenceModule,
) : DigitClassifierFactory {

    override fun create(modelId: ModelId): DigitClassifier = when (modelId.value) {
        ModelId.CNN_MNIST.value -> CnnDigitClassifier(cnnModuleProvider(), repository)
        ModelId.MLP_MNIST.value -> MlpDigitClassifier(mlpModuleProvider(), repository)
        else -> error("Unsupported ModelId: ${modelId.value}")
    }
}
