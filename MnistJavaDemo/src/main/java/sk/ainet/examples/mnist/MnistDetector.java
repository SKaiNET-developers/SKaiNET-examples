package sk.ainet.examples.mnist;

import sk.ainet.examples.mnist.interop.KotlinBridge;
import sk.ainet.examples.mnist.pipeline.DatasetPipeline;
import sk.ainet.examples.mnist.pipeline.ImagePipeline;
import sk.ainet.examples.mnist.pipeline.InferencePipeline;
import sk.ainet.examples.mnist.pipeline.InferencePipeline.InferenceInput;
import sk.ainet.lang.tensor.Shape;

/**
 * CLI MNIST digit detector using SKaiNET.
 * <p>
 * Demonstrates Java 21 features (sealed interfaces, records, pattern matching switch,
 * text blocks) and Java → Kotlin interop with SKaiNET's ML framework.
 * <p>
 * Usage:
 * <pre>
 *   ./gradlew run --args="detect --model mnist.gguf digit.png"
 *   ./gradlew run --args="test --model mnist.gguf --count 20"
 * </pre>
 */
public final class MnistDetector {

    public static void main(String[] args) {
        var cliArgs = CliArgs.parse(args);

        // Pattern matching switch on sealed interface (Java 21)
        switch (cliArgs) {
            case CliArgs.Help _ -> printHelp();
            case CliArgs.DetectFromImage cmd -> detectFromImage(cmd);
            case CliArgs.DetectFromDataset cmd -> detectFromDataset(cmd);
        }
    }

    private static void detectFromImage(CliArgs.DetectFromImage cmd) {
        System.out.println("Loading model: " + cmd.modelPath());
        System.out.println("Processing image: " + cmd.imagePath());
        if (cmd.invert()) {
            System.out.println("(inverting colors)");
        }
        System.out.println();

        var ctx = KotlinBridge.createExecutionContext();

        // Image pipeline: file path → preprocessed MNIST tensor (via SKaiNET transforms)
        var imagePipeline = ImagePipeline.create(ctx, cmd.invert());
        var tensor = imagePipeline.execute(cmd.imagePath());

        // Convert FP16 image tensor to FP32 float array for inference
        var pixels = extractPixels(tensor);
        var input = new InferenceInput(pixels, new Shape(new int[]{1, 1, 28, 28}));

        // Inference pipeline: tensor → DetectionResult
        var inferencePipeline = InferencePipeline.create(cmd.modelPath());
        var result = inferencePipeline.execute(input);

        printResult(result);
    }

    private static void detectFromDataset(CliArgs.DetectFromDataset cmd) {
        System.out.println("Loading model: " + cmd.modelPath());
        System.out.println("Running on " + cmd.sampleCount() + " MNIST test samples");
        System.out.println();

        // Dataset pipeline: config → MNIST dataset
        var datasetPipeline = DatasetPipeline.create();
        var dataset = datasetPipeline.execute(new DatasetPipeline.DatasetConfig(cmd.sampleCount()));

        // Inference pipeline
        var inferencePipeline = InferencePipeline.create(cmd.modelPath());

        var correct = 0;
        var total = dataset.getImages().size();

        for (int i = 0; i < total; i++) {
            var image = dataset.getImages().get(i);
            var input = InferencePipeline.fromMnistImage(image);
            var result = inferencePipeline.execute(input);
            var label = image.getLabel() & 0xFF;

            var match = result.predictedDigit() == label;
            if (match) correct++;

            System.out.printf("  Sample %3d: label=%d  predicted=%d  confidence=%.1f%%  %s%n",
                    i + 1, label, result.predictedDigit(),
                    result.confidence() * 100, match ? "OK" : "MISS");
        }

        System.out.println();
        System.out.printf("Accuracy: %d/%d (%.1f%%)%n", correct, total, (correct * 100.0) / total);
    }

    @SuppressWarnings("unchecked")
    private static float[] extractPixels(
            sk.ainet.lang.tensor.Tensor<?, ?> tensor
    ) {
        // Extract float pixel values from the preprocessed tensor
        // Shape is (1, 3, 28, 28) from mnistPreprocessing; we need (1, 1, 28, 28) grayscale
        // Take the first channel as grayscale
        var pixels = new float[784];
        for (int y = 0; y < 28; y++) {
            for (int x = 0; x < 28; x++) {
                var value = ((Number) tensor.getData().get(0, 0, y, x)).floatValue();
                pixels[y * 28 + x] = value;
            }
        }
        return pixels;
    }

    private static void printResult(DetectionResult result) {
        System.out.println("Predicted digit: " + result.predictedDigit());
        System.out.printf("Confidence: %.1f%%%n%n", result.confidence() * 100);
        System.out.println("Probability distribution:");

        for (int i = 0; i < 10; i++) {
            var prob = result.probabilities()[i];
            var barLen = Math.round(prob * 40);
            var bar = "#".repeat(barLen);
            var marker = (i == result.predictedDigit()) ? " <--" : "";
            System.out.printf("  [%d] %5.1f%% %s%s%n", i, prob * 100, bar, marker);
        }
    }

    private static void printHelp() {
        System.out.println("""
                MNIST Digit Detector (Java + SKaiNET)

                Usage: mnist-java [command] [options]

                Commands:
                  detect <image>   Detect digit from image file
                  test             Run on MNIST test dataset samples

                Options:
                  --model <path>   Path to GGUF model file (required)
                  --count <n>      Number of test samples (default: 10, test mode)
                  --invert         Invert image colors (detect mode)
                  --help, -h       Show help

                Examples:
                  ./gradlew run --args="detect --model mnist.gguf digit.png"
                  ./gradlew run --args="test --model mnist.gguf --count 20"
                """);
    }
}
