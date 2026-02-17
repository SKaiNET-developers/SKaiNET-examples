package sk.ainet.examples.mnist;

/**
 * Result of MNIST digit detection.
 *
 * @param predictedDigit the digit with highest probability (0-9)
 * @param confidence     the probability of the predicted digit
 * @param probabilities  probability distribution over all 10 digits
 */
public record DetectionResult(int predictedDigit, float confidence, float[] probabilities) {}
