package sk.ainet.cli.io

import sk.ainet.clean.data.image.GrayScale28To28Image
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Loads and converts images to GrayScale28To28Image format for MNIST classification.
 */
object ImageLoader {

    /**
     * Loads an image from file and converts it to a 28x28 grayscale image.
     *
     * @param file The image file (PNG, JPG, etc.)
     * @param invert If true, inverts colors (useful for white-on-black MNIST style images)
     * @return A 28x28 grayscale image ready for classification
     */
    fun load(file: File, invert: Boolean = false): GrayScale28To28Image {
        require(file.exists()) { "Image file does not exist: ${file.absolutePath}" }

        val originalImage = ImageIO.read(file)
            ?: throw IllegalArgumentException("Cannot read image file: ${file.absolutePath}")

        return convertToGrayscale28x28(originalImage, invert)
    }

    /**
     * Converts a BufferedImage to a 28x28 grayscale image.
     */
    fun convertToGrayscale28x28(image: BufferedImage, invert: Boolean = false): GrayScale28To28Image {
        // Resize to 28x28
        val resized = resizeImage(image, 28, 28)

        val result = GrayScale28To28Image()
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                val rgb = resized.getRGB(x, y)
                val color = Color(rgb)

                // Convert to grayscale using luminance formula
                val gray = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255.0

                // MNIST expects white digit on black background (1.0 = digit, 0.0 = background)
                // Default: assume input is already white-on-black (MNIST style), keep as-is
                // With invert: for black digit on white background, invert colors
                val value = if (invert) (1.0 - gray) else gray

                result.setPixel(x, y, value.toFloat().coerceIn(0f, 1f))
            }
        }
        return result
    }

    private fun resizeImage(original: BufferedImage, width: Int, height: Int): BufferedImage {
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(original, 0, 0, width, height, null)
        g.dispose()
        return resized
    }
}
