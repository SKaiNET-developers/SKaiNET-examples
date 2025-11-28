package com.kkon.kmp.mnist.data.loader

import kotlinx.io.Source
import sk.ai.net.gguf.GGMLQuantizationType
import sk.ai.net.gguf.GGUFReader
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Linear
import sk.ai.net.nn.Module

/**
 * Utility functions for loading model weights and parameters.
 */

/**
 * Loads model weights from a GGUF file and applies them to a neural network.
 *
 * This function:
 * 1. Opens the GGUF file using the provided source
 * 2. Extracts the tensors
 * 3. Applies them to the neural network
 *
 * @param model The neural network to apply the weights to.
 * @param modelParamsSource The source containing the model parameters.
 */
fun loadModelWeights(model: Module, modelParamsSource: Source) {
    // Create a GGUFReader to parse the file
    val reader = GGUFReader(modelParamsSource)

    // Get the tensors from the GGUF file
    val tensors = reader.tensors

    println("Found ${tensors.size} tensors in the GGUF file")

    // Get the linear layers from the model
    val linearLayers = mutableListOf<Linear>()
    fun findLinearLayers(module: Module) {
        if (module is Linear) {
            linearLayers.add(module)
        }
        for (child in module.modules) {
            findLinearLayers(child)
        }
    }
    findLinearLayers(model)

    for (layer in linearLayers) {
        val name = layer.name
        println("Processing tensor: $name")
    }

    println("Found ${linearLayers.size} linear layers in the model")

    // Map the tensors to the linear layers
    for (tensor in tensors) {
        val name = tensor.name.substring("model.".length)
        println("Processing tensor: $name")

        // Find the corresponding linear layer and parameter
        for (layer in linearLayers) {
            for (param in layer.params) {
                if (param.name.contains(name, ignoreCase = true)) {
                    // Convert the tensor data to a DoublesTensor
                    val tensorData = when (tensor.tensorType) {
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
                    param.value = tensorData
                    println("Applied tensor $name to parameter ${param.name}")
                }
            }
        }
    }
}