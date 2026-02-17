package sk.ainet.examples.mnist.pipeline;

import sk.ainet.data.mnist.MNISTImage;
import sk.ainet.examples.mnist.DetectionResult;
import sk.ainet.examples.mnist.ModelFactoryKt;
import sk.ainet.lang.nn.Module;
import sk.ainet.lang.tensor.Shape;
import sk.ainet.lang.tensor.Tensor;
import sk.ainet.lang.tensor.TensorExtensionsKt;
import sk.ainet.lang.types.FP32;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pipeline for MNIST model inference using SKaiNET.
 * <p>
 * Loads a GGUF model, runs forward pass, and extracts predictions.
 * Demonstrates Java interop with SKaiNET's neural network and tensor APIs.
 * <p>
 * Java interop patterns used:
 * <ul>
 *   <li>Top-level Kotlin functions via {@code ModelFactoryKt.createMnistCnn()}</li>
 *   <li>Extension functions via {@code TensorExtensionsKt.softmax(tensor, dim)}</li>
 *   <li>Kotlin default params bridged via helper functions in ModelFactory.kt</li>
 * </ul>
 */
public final class InferencePipeline {

    private InferencePipeline() {}

    /**
     * Inference input: preprocessed pixel data with tensor shape.
     *
     * @param pixels the pixel data (normalized, 784 floats for 28x28)
     * @param shape  the tensor shape for the model input
     */
    public record InferenceInput(float[] pixels, Shape shape) {}

    /**
     * Creates a pipeline that loads a GGUF model and runs inference on MNIST images.
     *
     * @param modelPath path to the GGUF model file
     * @return a pipeline from InferenceInput to DetectionResult
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Pipeline<InferenceInput, DetectionResult> create(String modelPath) {
        // Create CNN model and load GGUF weights (via Kotlin bridge)
        var model = createAndLoadModel(modelPath);
        var evalCtx = ModelFactoryKt.getEvalContext();

        return Pipeline.<InferenceInput>builder("inference-pipeline")
                .addStep("forward", input -> {
                    // Create input tensor via Kotlin bridge (handles KClass<FP32>)
                    var inputTensor = ModelFactoryKt.createInputTensor(input.shape(), input.pixels());
                    // Run forward pass through the CNN
                    return (Tensor) model.forward(inputTensor, evalCtx);
                })
                .addStep("argmax", (Tensor output) -> {
                    // Apply softmax: Kotlin extension function -> static method call
                    var probs = TensorExtensionsKt.softmax(output, -1);

                    var probabilities = new float[10];
                    var maxIdx = 0;
                    var maxVal = Float.NEGATIVE_INFINITY;

                    for (int j = 0; j < 10; j++) {
                        var value = ((Number) probs.getData().get(0, j)).floatValue();
                        probabilities[j] = value;
                        if (value > maxVal) {
                            maxVal = value;
                            maxIdx = j;
                        }
                    }

                    return new DetectionResult(maxIdx, maxVal, probabilities);
                })
                .build();
    }

    /**
     * Converts an MNISTImage (raw bytes + label) to an InferenceInput.
     * The CNN expects shape (1, 1, 28, 28) with float values in [0, 1].
     */
    public static InferenceInput fromMnistImage(MNISTImage image) {
        var pixels = new float[784];
        var bytes = image.getImage();
        for (int i = 0; i < 784; i++) {
            // MNIST bytes are unsigned 0-255, normalize to [0, 1]
            pixels[i] = (bytes[i] & 0xFF) / 255.0f;
        }
        return new InferenceInput(pixels, new Shape(new int[]{1, 1, 28, 28}));
    }

    private static Module<FP32, Float> createAndLoadModel(String modelPath) {
        byte[] modelBytes;
        try {
            modelBytes = Files.readAllBytes(Path.of(modelPath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read model file: " + modelPath, e);
        }

        // Model creation uses Kotlin's sequential DSL (inline reified),
        // bridged via ModelFactory.kt -> ModelFactoryKt.createMnistCnn()
        var model = ModelFactoryKt.createMnistCnn();

        // Load GGUF weights into the model
        ModelFactoryKt.loadGgufWeights(model, modelBytes);

        return model;
    }
}
