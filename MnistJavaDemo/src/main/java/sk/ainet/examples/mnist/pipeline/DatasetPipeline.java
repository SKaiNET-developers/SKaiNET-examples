package sk.ainet.examples.mnist.pipeline;

import kotlin.coroutines.Continuation;
import sk.ainet.data.mnist.MNIST;
import sk.ainet.data.mnist.MNISTDataset;
import sk.ainet.data.mnist.MNISTLoaderConfig;
import sk.ainet.examples.mnist.interop.KotlinBridge;

/**
 * Pipeline for downloading and loading the MNIST test dataset via SKaiNET.
 * <p>
 * Demonstrates Java → Kotlin suspend function interop: {@code MNIST.loadTest()}
 * is a Kotlin suspend function, bridged to Java via {@code runBlocking}.
 */
public final class DatasetPipeline {

    private DatasetPipeline() {}

    /**
     * Configuration for dataset loading.
     *
     * @param sampleCount number of samples to use from the test set
     */
    public record DatasetConfig(int sampleCount) {}

    /**
     * Creates a pipeline that downloads (if needed) and loads MNIST test data.
     *
     * @return a pipeline from DatasetConfig to MNISTDataset
     */
    public static Pipeline<DatasetConfig, MNISTDataset> create() {
        return Pipeline.<DatasetConfig>builder("dataset-pipeline")
                .addStep("download-and-parse", config -> {
                    // MNIST.loadTest() is a Kotlin suspend function.
                    // From Java, we bridge via runBlocking with explicit Continuation.
                    MNISTDataset fullDataset = KotlinBridge.runBlocking(
                            (scope, cont) -> MNIST.INSTANCE.loadTest(new MNISTLoaderConfig(), cont)
                    );

                    // Take only the requested number of samples
                    int count = Math.min(config.sampleCount(), fullDataset.getImages().size());
                    return fullDataset.subset(0, count);
                })
                .build();
    }
}
