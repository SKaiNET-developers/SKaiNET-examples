package sk.ainet.cli.io

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageLoaderTest {

    @Test
    fun `convertToGrayscale28x28 produces correct dimensions`() {
        val image = createTestImage(100, 100, Color.WHITE)
        val result = ImageLoader.convertToGrayscale28x28(image)

        // Verify dimensions by checking boundary pixels
        result.getPixel(0, 0)
        result.getPixel(27, 27)

        // Should throw for out of bounds
        assertFailsWith<IllegalArgumentException> {
            result.getPixel(28, 0)
        }
    }

    @Test
    fun `convertToGrayscale28x28 converts black image without invert`() {
        // Black image input without invert keeps original (black = 0.0)
        val image = createTestImage(28, 28, Color.BLACK)
        val result = ImageLoader.convertToGrayscale28x28(image, invert = false)

        // Black input (0,0,0) -> grayscale 0.0 -> kept as 0.0
        val pixel = result.getPixel(14, 14)
        assertEquals(0.0f, pixel, 0.01f)
    }

    @Test
    fun `convertToGrayscale28x28 converts white image without invert`() {
        // White image input without invert keeps original (white = 1.0)
        val image = createTestImage(28, 28, Color.WHITE)
        val result = ImageLoader.convertToGrayscale28x28(image, invert = false)

        // White input (255,255,255) -> grayscale 1.0 -> kept as 1.0
        val pixel = result.getPixel(14, 14)
        assertEquals(1.0f, pixel, 0.01f)
    }

    @Test
    fun `convertToGrayscale28x28 with invert inverts values`() {
        // With invert=true, white becomes black (for black-on-white input)
        val image = createTestImage(28, 28, Color.WHITE)
        val result = ImageLoader.convertToGrayscale28x28(image, invert = true)

        // White input (255,255,255) -> grayscale 1.0 -> inverted = 0.0
        val pixel = result.getPixel(14, 14)
        assertEquals(0.0f, pixel, 0.01f)
    }

    @Test
    fun `convertToGrayscale28x28 handles gray colors correctly`() {
        val gray = Color(128, 128, 128)
        val image = createTestImage(28, 28, gray)
        val result = ImageLoader.convertToGrayscale28x28(image, invert = false)

        // Gray input (128,128,128) -> grayscale ~0.5 -> inverted ~0.5
        val pixel = result.getPixel(14, 14)
        assertTrue(pixel > 0.4f && pixel < 0.6f, "Expected ~0.5, got $pixel")
    }

    @Test
    fun `load reads image file successfully`() {
        val tempFile = createTempImageFile(Color.WHITE)
        try {
            val result = ImageLoader.load(tempFile, invert = false)
            // White image without invert keeps white pixels (1.0)
            val pixel = result.getPixel(14, 14)
            assertEquals(1.0f, pixel, 0.01f)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `load throws for nonexistent file`() {
        val nonexistent = File("/nonexistent/path/image.png")
        assertFailsWith<IllegalArgumentException> {
            ImageLoader.load(nonexistent)
        }
    }

    @Test
    fun `convertToGrayscale28x28 resizes larger images correctly`() {
        // Create a 100x100 image with a pattern
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, 100, 100)
        // Draw a white square in the center
        g.color = Color.WHITE
        g.fillRect(25, 25, 50, 50)
        g.dispose()

        val result = ImageLoader.convertToGrayscale28x28(image, invert = false)

        // Center should be brighter (white input -> 1.0)
        val centerPixel = result.getPixel(14, 14)
        // Corner should be darker (black input -> 0.0)
        val cornerPixel = result.getPixel(0, 0)

        assertTrue(centerPixel > cornerPixel, "Center should be brighter than corner")
    }

    private fun createTestImage(width: Int, height: Int, color: Color): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.dispose()
        return image
    }

    private fun createTempImageFile(color: Color): File {
        val image = createTestImage(28, 28, color)
        val tempFile = File.createTempFile("test-image-", ".png")
        ImageIO.write(image, "PNG", tempFile)
        return tempFile
    }
}
