package com.kkon.kmp.mnist.demo


interface DigitClassifier {
    fun classify(image: GrayScale28To28Image): Int

    suspend fun loadModel()

    class GrayScale28To28Image {
        private val width = 28
        private val height = 28
        private val pixels: Array<FloatArray> = Array(height) { FloatArray(width) }

        val data: DoubleArray
            get() =
                DoubleArray(height * width) { idx ->
                    val i = idx / width
                    val j = idx % width
                    pixels[j][i].toDouble()
                }


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
