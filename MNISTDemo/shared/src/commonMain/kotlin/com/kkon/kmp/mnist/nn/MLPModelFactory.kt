package com.kkon.kmp.mnist.nn

import sk.ai.net.dsl.network
import sk.ai.net.io.data.mnist.MNISTConstants
import sk.ai.net.nn.Module
import sk.ai.net.nn.activations.ReLU

/**
 * Creates an MLP network for MNIST digit classification.
 *
 * The network architecture is as follows:
 * - Input layer: 784 neurons (28x28 pixels flattened)
 * - First hidden layer: 128 neurons with ReLU activation
 * - Output layer: 10 neurons (one for each digit 0-9)
 *
 * This is a simple feedforward neural network that takes a flattened MNIST image as input
 * and outputs a probability distribution over the 10 possible digits. The digit with the
 * highest probability is the predicted digit.
 *
 * @return The MLP network.
 */
fun MLPFactory(): Module {
    // Create the MLP network using the DSL
    return network {
        input(MNISTConstants.IMAGE_PIXELS) // 784 input neurons (28x28 pixels)
        dense(128, "hidden1") { // First hidden layer with 128 neurons
            activation = ReLU()::invoke
        }
        dense(10, "output") { // Output layer with 10 neurons (one for each digit 0-9)
            // No activation for the output layer (linear)
        }
    }
}
