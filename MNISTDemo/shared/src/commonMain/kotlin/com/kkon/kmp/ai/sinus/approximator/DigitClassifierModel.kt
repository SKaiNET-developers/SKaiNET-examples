package com.kkon.kmp.ai.sinus.approximator

import sk.ai.net.Shape
import sk.ai.net.Tensor
import sk.ai.net.dsl.network
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Module


interface DigitClassifier {
    fun classify(image: GrayScale28To28Image): Int
    suspend fun loadModel()

    class GrayScale28To28Image {
        private val width = 28
        private val height = 28
        private val pixels: Array<FloatArray> = Array(height) { FloatArray(width) }

        // Set pixel value (normalized between 0 and 1)
        fun setPixel(x: Int, y: Int, value: Float) {
            require(x in 0 until width && y in 0 until height) { "Pixel coordinates out of bounds: x=$x, y=$y" }
            require(value in 0.0..1.0) { "Pixel value must be between 0 and 1: value=$value" }
            pixels[x][y] = value
        }

        fun getPixel(x: Int, y: Int): Float {
            require(x in 0 until width && y in 0 until height) { "Pixel coordinates out of bounds: x=$x, y=$y" }
            return pixels[x][y]
        }

        fun debugPrintInConsoleOutput() {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    print(
                        when (pixels[x][y]) {
                            in 0.0..0.3 -> " "
                            in 0.3..0.6 -> "."
                            in 0.6..1.0 -> ":"
                            else -> "|"
                        }
                    )
                }
                println()
            }
        }
    }
}


class DigitClassifierNN(override val name: String = "digit_classifier") : Module() {

    private val module = network {
        //Pseudo code
//        input(1, 28, 28) // 1 channel (grayscale), 28x28 image size
//
//        conv2d(32, 3) { // 32 filters, 3x3 kernel
//            activation = ReLU()::forward
//        }
//
//        maxPool2d(2) // 2x2 pooling
//
//        conv2d(64, 3) { // 64 filters, 3x3 kernel
//            activation = ReLU()::forward
//        }
//
//        maxPool2d(2) // 2x2 pooling
//
//        flatten() // Flatten to feed into dense layers
//
//        dense(128) {
//            activation = ReLU()::forward
//        }
//
//        dense(10) { // 10 output neurons (digits 0-9)
//            activation = Softmax()::forward
//        }
    }

    override val modules: List<Module>
        get() = module.modules

    override fun forward(input: Tensor): Tensor =
        module.forward(input)
}

fun DigitClassifierNN.of(angle: Double): Tensor =
    this.forward(DoublesTensor(Shape(1), listOf(angle).toDoubleArray()))