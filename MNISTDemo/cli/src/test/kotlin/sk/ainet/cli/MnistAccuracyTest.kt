package sk.ainet.cli

import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.lang.model.classifyImage
import sk.ainet.lang.model.createMNISTMLP
import sk.ainet.lang.model.loadGgufWeights
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test MNIST digit classification accuracy using the pretrained GGUF model.
 * Downloads MNIST test dataset and verifies model achieves >90% accuracy.
 */
class MnistAccuracyTest {

    companion object {
        private const val MNIST_TEST_IMAGES_URL = "https://storage.googleapis.com/cvdf-datasets/mnist/t10k-images-idx3-ubyte.gz"
        private const val MNIST_TEST_LABELS_URL = "https://storage.googleapis.com/cvdf-datasets/mnist/t10k-labels-idx1-ubyte.gz"
        private const val MODEL_FILENAME = "mnist-fc-f32.gguf"
        private val MNIST_CACHE_DIR = File(System.getProperty("java.io.tmpdir"), "mnist-test-cache")

        private fun findModelFile(): File? {
            // Try common locations where the model might be
            val candidates = listOf(
                File(MODEL_FILENAME),                    // Current directory
                File("..", MODEL_FILENAME),              // Parent directory (project root)
                File("../..", MODEL_FILENAME),           // Two levels up
            )
            return candidates.firstOrNull { it.exists() }
        }
    }

    @Test
    fun `MLP model achieves over 90 percent accuracy on MNIST test set`() {
        // Find model file
        val modelFile = findModelFile()
        if (modelFile == null) {
            println("Skipping test: Model file '$MODEL_FILENAME' not found in expected locations")
            println("Working directory: ${File(".").absolutePath}")
            return
        }

        // Download MNIST test data (limit to 500 to avoid OOM in tests)
        val (images, labels) = loadMnistTestData(limit = 500)
        println("Loaded ${images.size} test images")

        // Create model and load GGUF weights directly
        val model = createMNISTMLP()
        val modelBytes = modelFile.readBytes()
        loadGgufWeights(model, modelBytes)
        println("Model loaded from ${modelFile.absolutePath}: ${modelBytes.size} bytes")

        // Classify all test images
        var correct = 0
        val total = images.size

        for (i in images.indices) {
            val image = images[i]
            val expectedLabel = labels[i]
            val predictedLabel = classifyImage(model, image)

            if (predictedLabel == expectedLabel) {
                correct++
            }

            // Periodically run GC to avoid OOM
            if (i % 100 == 99) {
                System.gc()
            }
        }

        val accuracy = correct.toDouble() / total * 100
        println("Accuracy: $correct / $total = ${"%.2f".format(accuracy)}%")

        assertTrue(accuracy >= 90.0, "Expected accuracy >= 90%, but got ${"%.2f".format(accuracy)}%")
    }

    @Test
    fun `MLP model correctly classifies sample digits`() {
        // Find model file
        val modelFile = findModelFile()
        if (modelFile == null) {
            println("Skipping test: Model file '$MODEL_FILENAME' not found in expected locations")
            return
        }

        // Load a small sample for quick verification
        val (images, labels) = loadMnistTestData(limit = 100)

        // Create model and load GGUF weights
        val model = createMNISTMLP()
        val modelBytes = modelFile.readBytes()
        loadGgufWeights(model, modelBytes)

        var correct = 0
        for (i in images.indices) {
            val predicted = classifyImage(model, images[i])
            if (predicted == labels[i]) correct++
        }

        val accuracy = correct.toDouble() / images.size * 100
        println("Sample accuracy (100 images): ${"%.2f".format(accuracy)}%")
        assertTrue(accuracy >= 85.0, "Sample accuracy should be at least 85%")
    }

    private fun loadMnistTestData(limit: Int = Int.MAX_VALUE): Pair<List<GrayScale28To28Image>, List<Int>> {
        MNIST_CACHE_DIR.mkdirs()

        val imagesFile = File(MNIST_CACHE_DIR, "t10k-images-idx3-ubyte.gz")
        val labelsFile = File(MNIST_CACHE_DIR, "t10k-labels-idx1-ubyte.gz")

        // Download if not cached
        if (!imagesFile.exists()) {
            println("Downloading MNIST test images...")
            downloadFile(MNIST_TEST_IMAGES_URL, imagesFile)
        }
        if (!labelsFile.exists()) {
            println("Downloading MNIST test labels...")
            downloadFile(MNIST_TEST_LABELS_URL, labelsFile)
        }

        // Read images
        val images = mutableListOf<GrayScale28To28Image>()
        DataInputStream(GZIPInputStream(FileInputStream(imagesFile))).use { dis ->
            val magic = dis.readInt()
            require(magic == 2051) { "Invalid image file magic number: $magic" }
            val numImages = dis.readInt()
            val rows = dis.readInt()
            val cols = dis.readInt()
            require(rows == 28 && cols == 28) { "Unexpected image dimensions: ${rows}x${cols}" }

            val count = minOf(numImages, limit)
            for (i in 0 until count) {
                val image = GrayScale28To28Image()
                for (y in 0 until 28) {
                    for (x in 0 until 28) {
                        val pixel = dis.readUnsignedByte()
                        // Normalize to 0.0-1.0
                        image.setPixel(x, y, pixel / 255.0f)
                    }
                }
                images.add(image)
            }
        }

        // Read labels
        val labels = mutableListOf<Int>()
        DataInputStream(GZIPInputStream(FileInputStream(labelsFile))).use { dis ->
            val magic = dis.readInt()
            require(magic == 2049) { "Invalid label file magic number: $magic" }
            val numLabels = dis.readInt()

            val count = minOf(numLabels, limit)
            for (i in 0 until count) {
                labels.add(dis.readUnsignedByte())
            }
        }

        return Pair(images, labels)
    }

    private fun downloadFile(urlString: String, destination: File) {
        URL(urlString).openStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
