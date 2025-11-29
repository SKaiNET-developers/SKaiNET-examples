package sk.ainet.clean.domain.usecase

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.clean.domain.port.DigitClassifier

/** Use case to classify a digit image (PRD §5, §12). */
class ClassifyDigit(
    private val classifier: DigitClassifier
) {
    operator fun invoke(image: GrayScale28To28Image): Int = classifier.classify(image)
}
