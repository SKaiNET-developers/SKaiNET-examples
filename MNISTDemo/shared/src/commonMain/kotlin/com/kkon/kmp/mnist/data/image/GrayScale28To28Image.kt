package com.kkon.kmp.mnist.data.image

/**
 * Represents a 28x28 grayscale image for MNIST digit classification.
 * 
 * This class provides methods to set and get pixel values, as well as
 * to convert the image to a flattened array for neural network input.
 */
class GrayScale28To28Image {
    private val width = 28
    private val height = 28
    private val pixels: Array<FloatArray> = Array(height) { FloatArray(width) }

    /**
     * Returns the image data as a flattened DoubleArray.
     * This is useful for feeding the image into neural networks.
     */
    val data: DoubleArray
        get() =
            DoubleArray(height * width) { idx ->
                val i = idx / width
                val j = idx % width
                pixels[j][i].toDouble()
            }

    /**
     * Sets a pixel value at the specified coordinates.
     * 
     * @param x The x-coordinate (0-27)
     * @param y The y-coordinate (0-27)
     * @param value The pixel value (normalized between 0 and 1)
     */
    fun setPixel(x: Int, y: Int, value: Float) {
        require(x in 0 until width && y in 0 until height) { "Pixel coordinates out of bounds: x=$x, y=$y" }
        require(value in 0.0..1.0) { "Pixel value must be between 0 and 1: value=$value" }
        pixels[x][y] = value
    }

    /**
     * Gets the pixel value at the specified coordinates.
     * 
     * @param x The x-coordinate (0-27)
     * @param y The y-coordinate (0-27)
     * @return The pixel value (normalized between 0 and 1)
     */
    fun getPixel(x: Int, y: Int): Float {
        require(x in 0 until width && y in 0 until height) { "Pixel coordinates out of bounds: x=$x, y=$y" }
        return pixels[x][y]
    }

    /**
     * Prints a text representation of the image to the console.
     * Useful for debugging.
     */
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