package sk.ainet.clean.domain.factory

import sk.ainet.clean.domain.model.ModelId
import sk.ainet.clean.domain.port.DigitClassifier

/**
 * Factory to obtain a DigitClassifier strategy for a given [ModelId] (PRD §3).
 * Concrete implementation will be provided in later steps wiring specific strategies.
 */
interface DigitClassifierFactory {
    fun create(modelId: ModelId): DigitClassifier
}
