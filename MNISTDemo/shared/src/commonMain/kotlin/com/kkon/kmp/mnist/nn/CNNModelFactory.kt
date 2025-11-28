package com.kkon.kmp.mnist.nn

import sk.ai.net.dsl.network
import sk.ai.net.nn.Module
import sk.ai.net.nn.activations.ReLU
import sk.ai.net.nn.activations.Softmax


/**
 * Constructs a Convolutional Neural Network (CNN) tailored for the MNIST dataset using a DSL-based network builder.
 *
 * This model consists of two convolutional blocks followed by a flattening stage and two dense (fully connected) layers.
 * It is designed to classify handwritten digits (0–9) from grayscale 28x28 pixel images.
 *
 * The architecture is as follows:
 *
 * - **Stage: "conv1"**
 *   - 2D Convolution with:
 *     - 16 output channels
 *     - 5x5 kernel
 *     - stride of 1
 *     - padding of 2
 *   - ReLU activation
 *   - 2x2 MaxPooling with stride of 2
 *
 * - **Stage: "conv2"**
 *   - 2D Convolution with:
 *     - 32 output channels
 *     - 5x5 kernel
 *     - stride of 1
 *     - padding of 2
 *   - ReLU activation
 *   - 2x2 MaxPooling with stride of 2
 *
 * - **Stage: "flatten"**
 *   - Flattens the tensor for dense layer input
 *
 * - **Stage: "dense"**
 *   - Fully connected layer with 128 units
 *   - ReLU activation
 *
 * - **Stage: "output"**
 *   - Fully connected layer with 10 output units (for 10 MNIST classes)
 *   - Softmax activation over dimension 1 to produce class probabilities
 *
 * @return A [Module] representing the constructed CNN model
 */
fun createMNISTCNN(): Module = network {
    sequential {
        stage("conv1") {
            conv2d {
                outChannels = 16
                kernelSize = 5
                stride = 1
                padding = 2
            }
            activation("relu", ReLU()::forward)
            maxPool2d {
                kernelSize = 2
                stride = 2
            }
        }
        stage("conv2") {
            conv2d {
                outChannels = 32
                kernelSize = 5
                stride = 1
                padding = 2
            }
            activation("relu", ReLU()::forward)
            maxPool2d {
                kernelSize = 2
                stride = 2
            }
        }
        stage("flatten") {
            flatten()
        }
        stage("dense") {
            dense {
                units = 128
            }
            activation("relu", ReLU()::forward)
        }
        stage("output") {
            dense {
                units = 10
            }
            activation("softmax", Softmax(1)::forward)
        }
    }
}
