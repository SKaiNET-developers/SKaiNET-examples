package sk.ainet.cli

import sk.ainet.clean.domain.port.InferenceModule
import sk.ainet.clean.framework.inference.CnnInferenceModuleAdapter
import sk.ainet.clean.framework.inference.MlpInferenceModuleAdapter
import sk.ainet.cli.io.ImageLoader
import java.io.File
import kotlin.system.exitProcess

/**
 * CLI application for MNIST digit classification.
 *
 * Usage: mnist-cli [options] <image-path>
 *
 * Options:
 *   --model <mlp|cnn>       Model type to use for classification (default: mlp)
 *   --model-file <file>     Path to model weights file (.gguf)
 *   --weights-dir <dir>     Directory containing model weights (alternative to --model-file)
 *   --invert                Invert image colors (use for black-on-white images)
 *   --debug                 Print debug information including image preview
 *   --help                  Show this help message
 */
fun main(args: Array<String>) {
    val config = ArgsParser.parse(args)

    if (config.help) {
        printHelp()
        exitProcess(0)
    }

    if (config.imagePath == null) {
        System.err.println("Error: Image path is required")
        printHelp()
        exitProcess(1)
    }

    val imageFile = File(config.imagePath)
    if (!imageFile.exists()) {
        System.err.println("Error: Image file not found: ${config.imagePath}")
        exitProcess(1)
    }

    // Determine model type
    val modelType = when (config.model.lowercase()) {
        "mlp" -> ModelType.MLP
        "cnn" -> ModelType.CNN
        else -> {
            System.err.println("Error: Unknown model type '${config.model}'. Use 'mlp' or 'cnn'.")
            exitProcess(1)
        }
    }

    // Determine model weights source
    val modelWeights: ByteArray = when {
        config.modelFile != null -> {
            val modelFile = File(config.modelFile)
            if (!modelFile.exists()) {
                System.err.println("Error: Model file not found: ${config.modelFile}")
                exitProcess(1)
            }
            modelFile.readBytes()
        }
        config.weightsDir != null -> {
            val weightsDir = File(config.weightsDir)
            if (!weightsDir.exists()) {
                System.err.println("Error: Weights directory not found: ${config.weightsDir}")
                exitProcess(1)
            }
            val modelFileName = when (modelType) {
                ModelType.MLP -> "files/mnist_mlp.gguf"
                ModelType.CNN -> "files/mnist_cnn.gguf"
            }
            val modelFile = File(weightsDir, modelFileName)
            if (!modelFile.exists()) {
                System.err.println("Error: Model file not found: ${modelFile.absolutePath}")
                exitProcess(1)
            }
            modelFile.readBytes()
        }
        else -> {
            System.err.println("Error: Either --model-file or --weights-dir is required")
            printHelp()
            exitProcess(1)
        }
    }

    classify(imageFile, modelType, modelWeights, config.invert, config.debug)
}

private enum class ModelType { MLP, CNN }

private fun classify(
    imageFile: File,
    modelType: ModelType,
    modelWeights: ByteArray,
    invert: Boolean,
    debug: Boolean,
) {
    // Create inference module based on model type
    val inferenceModule: InferenceModule = when (modelType) {
        ModelType.MLP -> MlpInferenceModuleAdapter.create()
        ModelType.CNN -> CnnInferenceModuleAdapter.create()
    }

    if (debug) {
        println("Loading model: ${modelType.name.lowercase()}")
    }

    // Load weights directly into inference module
    inferenceModule.load(modelWeights)

    if (debug) {
        println("Model loaded successfully (${modelWeights.size} bytes)")
    }

    // Load and convert image
    val image = ImageLoader.load(imageFile, invert)

    if (debug) {
        println("Image loaded: ${imageFile.absolutePath}")
        println("Image preview:")
        image.debugPrintInConsoleOutput()
    }

    // Classify
    val digit = inferenceModule.infer(image)

    println(digit)
}

private fun printHelp() {
    println("""
        MNIST Digit Classifier CLI

        Usage: mnist-cli [options] <image-path>

        Arguments:
          <image-path>            Path to the image file (PNG, JPG, etc.)

        Options:
          --model <mlp|cnn>       Model type to use for classification (default: mlp)
          --model-file <file>     Path to model weights file (.gguf)
          --weights-dir <dir>     Directory containing model weights (alternative to --model-file)
                                  Expects files: files/mnist_mlp.gguf or files/mnist_cnn.gguf
          --invert                Invert image colors (use for black-on-white images,
                                  as MNIST expects white digit on black background)
          --debug                 Print debug information including image preview
          --help, -h              Show this help message

        Examples:
          # Using explicit model file
          mnist-cli --model mlp --model-file ./mnist_mlp.gguf digit.png

          # Using weights directory
          mnist-cli --model cnn --weights-dir ./models digit.png

          # With debug output
          mnist-cli --debug --model-file ./model.gguf test_image.png
    """.trimIndent())
}
