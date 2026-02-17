package sk.ainet.examples.mnist.pipeline;

import sk.ainet.context.ExecutionContext;
import sk.ainet.data.transform.ImageTransformDslKt;
import sk.ainet.data.transform.ScaleAndShift;
import sk.ainet.data.transform.Transform;
import sk.ainet.lang.tensor.Tensor;
import sk.ainet.lang.types.FP16;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Image processing pipeline that uses SKaiNET's transform API.
 * <p>
 * Demonstrates Java to Kotlin interop with SKaiNET's {@code Transform<I,O>}
 * pipeline, including {@code mnistPreprocessing()} which chains:
 * ImageResize(28,28) -> ImageToTensor -> Rescale(255) -> Normalize(mean, std).
 * <p>
 * On JVM, {@code PlatformBitmapImage} is a Kotlin typealias for {@code BufferedImage},
 * so standard Java {@code ImageIO} works directly with SKaiNET transforms.
 */
public final class ImagePipeline {

    private ImagePipeline() {}

    /**
     * Creates a pipeline that loads an image from a file path and preprocesses it
     * for MNIST classification using SKaiNET's built-in transforms.
     *
     * @param ctx    the SKaiNET execution context for tensor operations
     * @param invert whether to invert image colors (for black-on-white input)
     * @return a pipeline from file path to preprocessed MNIST tensor
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Pipeline<String, Tensor> create(ExecutionContext ctx, boolean invert) {
        // SKaiNET's mnistPreprocessing: resize(28,28) -> toTensor -> rescale(255) -> normalize(MNIST mean/std)
        // mnistPreprocessing is a top-level Kotlin function -> ImageTransformDslKt.mnistPreprocessing(ctx)
        Transform transform = ImageTransformDslKt.mnistPreprocessing(ctx);

        // Optionally invert colors for black-digit-on-white-background images
        if (invert) {
            var scaleAndShift = new ScaleAndShift<FP16, Float>(ctx, -1f, 1f);
            transform = transform.then(scaleAndShift);
        }

        // PlatformBitmapImage is a Kotlin typealias for BufferedImage on JVM,
        // so we use BufferedImage directly. Using raw Transform type because
        // Kotlin typealiases are invisible to Java.
        final Transform finalTransform = transform;

        return Pipeline.<String>builder("image-pipeline")
                .addStep("load-image", path -> {
                    try {
                        var file = new File(path);
                        if (!file.exists()) {
                            throw new IllegalArgumentException("Image file not found: " + path);
                        }
                        BufferedImage image = ImageIO.read(file);
                        if (image == null) {
                            throw new IllegalArgumentException("Cannot read image: " + path);
                        }
                        // BufferedImage IS PlatformBitmapImage on JVM (typealias)
                        return (Object) image;
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to load image: " + path, e);
                    }
                })
                .addStep("preprocess", img -> (Tensor) finalTransform.apply(img))
                .build();
    }
}
