package com.kkon.kmp.mnist.nn

import com.kkon.kmp.mnist.demo.DigitClassifier
import kotlinx.io.Source
import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.gguf.GGMLQuantizationType
import sk.ai.net.gguf.GGUFReader
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.io.data.mnist.MNISTConstants
import sk.ai.net.nn.Linear
import sk.ai.net.nn.Module

/**
 * Converts a MNIST image to a tensor suitable for input to the MLP network.
 *
 * The MNIST image is a 28x28 grayscale image stored as a ByteArray.
 * This function:
 * 1. Normalizes the pixel values from [0, 255] to [0, 1]
 * 2. Flattens the 28x28 image into a 784-element vector
 * 3. Creates a tensor with shape [784]
 *
 * @param image The MNIST image to convert.
 * @return A tensor representing the image.
 */
fun convertImageToTensor(image: DigitClassifier.GrayScale28To28Image): Tensor {
    // Normalize pixel values to range [0, 1]
    // Create a tensor with shape [784] (flattened 28x28 image)
    val imageData = image.data
    // val elements = imageData.map { (it.toInt() and 0xFF) / 255.0 }.toDoubleArray()
    return DoublesTensor(
        Shape(MNISTConstants.IMAGE_PIXELS),
        imageData
    )
}

/**
 * Loads model weights from a GGUF file and applies them to the MLP network.
 *
 * This function:
 * 1. Opens the GGUF file
 * 2. Extracts the tensors
 * 3. Applies them to the MLP network
 *
 * @param mlp The MLP network to apply the weights to.
 * @param modelPath The path to the GGUF file.
 */
fun loadModelWeights(mlp: Module, modelParamsSource: Source) {

    // Create a GGUFReader to parse the file
    val reader = GGUFReader(modelParamsSource)

    // Get the tensors from the GGUF file
    val tensors = reader.tensors

    println("Found ${tensors.size} tensors in the GGUF file")

    // Apply the tensors to the MLP network
    // The MLP network has the following structure:
    // - Input layer
    // - Linear layer 1 (784 -> 128) + ReLU
    // - Linear layer 2 (128 -> 128) + ReLU
    // - Linear layer 3 (128 -> 10)

    // Get the linear layers from the MLP
    val linearLayers = mutableListOf<Linear>()
    fun findLinearLayers(module: Module) {
        if (module is Linear) {
            linearLayers.add(module)
        }
        for (child in module.modules) {
            findLinearLayers(child)
        }
    }
    findLinearLayers(mlp)

    for (layer in linearLayers) {
        val name = layer.name
        println("Processing tensor: $name")
    }


    println("Found ${linearLayers.size} linear layers in the MLP network")

    // Map the tensors to the linear layers
    for (tensor in tensors) {
        val name = tensor.name.substring("model.".length)
        println("Processing tensor: $name")

        // Find the corresponding linear layer and parameter
        for (layer in linearLayers) {
            for (param in layer.params) {
                if (param.name.contains(name, ignoreCase = true)) {
                    // Convert the tensor data to a DoublesTensor
                    val tensor = when (tensor.tensorType) {
                        GGMLQuantizationType.F32,
                        GGMLQuantizationType.F16 -> {
                            val floatsList = tensor.data as List<Float>
                            DoublesTensor(
                                param.value.shape,
                                floatsList.map { it.toDouble() }.toDoubleArray()
                            )
                        }

                        else -> {
                            println("Unsupported tensor data type: ${tensor.data.firstOrNull()}")
                            continue
                        }
                    }

                    // Apply the tensor to the parameter
                    param.value = tensor
                    println("Applied tensor $name to parameter ${param.name}")
                }
            }
        }
    }
}