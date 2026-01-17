package sk.ainet.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArgsParserTest {

    @Test
    fun `parseArgs with image path only uses defaults`() {
        val config = ArgsParser.parse(arrayOf("image.png"))

        assertEquals("mlp", config.model)
        assertNull(config.weightsDir)
        assertNull(config.modelFile)
        assertEquals("image.png", config.imagePath)
        assertFalse(config.invert)
        assertFalse(config.debug)
        assertFalse(config.help)
    }

    @Test
    fun `parseArgs with model option`() {
        val config = ArgsParser.parse(arrayOf("--model", "cnn", "image.png"))

        assertEquals("cnn", config.model)
        assertEquals("image.png", config.imagePath)
    }

    @Test
    fun `parseArgs with weights-dir option`() {
        val config = ArgsParser.parse(arrayOf("--weights-dir", "/path/to/weights", "image.png"))

        assertEquals("/path/to/weights", config.weightsDir)
        assertEquals("image.png", config.imagePath)
    }

    @Test
    fun `parseArgs with model-file option`() {
        val config = ArgsParser.parse(arrayOf("--model-file", "/path/to/model.gguf", "image.png"))

        assertEquals("/path/to/model.gguf", config.modelFile)
        assertEquals("image.png", config.imagePath)
    }

    @Test
    fun `parseArgs with invert flag`() {
        val config = ArgsParser.parse(arrayOf("--invert", "image.png"))

        assertTrue(config.invert)
        assertEquals("image.png", config.imagePath)
    }

    @Test
    fun `parseArgs with debug flag`() {
        val config = ArgsParser.parse(arrayOf("--debug", "image.png"))

        assertTrue(config.debug)
        assertEquals("image.png", config.imagePath)
    }

    @Test
    fun `parseArgs with help flag`() {
        val config = ArgsParser.parse(arrayOf("--help"))

        assertTrue(config.help)
    }

    @Test
    fun `parseArgs with short help flag`() {
        val config = ArgsParser.parse(arrayOf("-h"))

        assertTrue(config.help)
    }

    @Test
    fun `parseArgs with all options using weights-dir`() {
        val config = ArgsParser.parse(arrayOf(
            "--model", "cnn",
            "--weights-dir", "/custom/weights",
            "--invert",
            "--debug",
            "test.png"
        ))

        assertEquals("cnn", config.model)
        assertEquals("/custom/weights", config.weightsDir)
        assertNull(config.modelFile)
        assertEquals("test.png", config.imagePath)
        assertTrue(config.invert)
        assertTrue(config.debug)
        assertFalse(config.help)
    }

    @Test
    fun `parseArgs with all options using model-file`() {
        val config = ArgsParser.parse(arrayOf(
            "--model", "mlp",
            "--model-file", "/path/to/custom.gguf",
            "--invert",
            "--debug",
            "test.png"
        ))

        assertEquals("mlp", config.model)
        assertNull(config.weightsDir)
        assertEquals("/path/to/custom.gguf", config.modelFile)
        assertEquals("test.png", config.imagePath)
        assertTrue(config.invert)
        assertTrue(config.debug)
        assertFalse(config.help)
    }

    @Test
    fun `parseArgs with no arguments returns null imagePath`() {
        val config = ArgsParser.parse(arrayOf())

        assertEquals(null, config.imagePath)
    }

    @Test
    fun `parseArgs handles image path at different positions`() {
        // Image path first
        val config1 = ArgsParser.parse(arrayOf("image.png", "--model", "mlp"))
        assertEquals("image.png", config1.imagePath)

        // Image path in the middle
        val config2 = ArgsParser.parse(arrayOf("--model", "cnn", "image.png", "--debug"))
        assertEquals("image.png", config2.imagePath)

        // Image path last
        val config3 = ArgsParser.parse(arrayOf("--invert", "--debug", "image.png"))
        assertEquals("image.png", config3.imagePath)
    }
}
