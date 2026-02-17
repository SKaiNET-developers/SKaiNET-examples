package sk.ainet.examples.mnist;

import org.junit.Test;

import static org.junit.Assert.*;

public class CliArgsTest {

    @Test
    public void emptyArgsParsesToHelp() {
        var result = CliArgs.parse(new String[]{});
        assertTrue(result instanceof CliArgs.Help);
    }

    @Test
    public void helpFlagParsesToHelp() {
        assertTrue(CliArgs.parse(new String[]{"--help"}) instanceof CliArgs.Help);
        assertTrue(CliArgs.parse(new String[]{"-h"}) instanceof CliArgs.Help);
    }

    @Test
    public void detectParsesCorrectly() {
        var result = CliArgs.parse(new String[]{"detect", "--model", "model.gguf", "digit.png"});
        assertTrue(result instanceof CliArgs.DetectFromImage);
        var cmd = (CliArgs.DetectFromImage) result;
        assertEquals("digit.png", cmd.imagePath());
        assertEquals("model.gguf", cmd.modelPath());
        assertFalse(cmd.invert());
    }

    @Test
    public void detectWithInvertFlag() {
        var result = CliArgs.parse(new String[]{"detect", "--model", "m.gguf", "--invert", "img.png"});
        assertTrue(result instanceof CliArgs.DetectFromImage);
        var cmd = (CliArgs.DetectFromImage) result;
        assertTrue(cmd.invert());
        assertEquals("img.png", cmd.imagePath());
    }

    @Test
    public void testParsesCorrectly() {
        var result = CliArgs.parse(new String[]{"test", "--model", "model.gguf", "--count", "20"});
        assertTrue(result instanceof CliArgs.DetectFromDataset);
        var cmd = (CliArgs.DetectFromDataset) result;
        assertEquals("model.gguf", cmd.modelPath());
        assertEquals(20, cmd.sampleCount());
    }

    @Test
    public void testDefaultCount() {
        var result = CliArgs.parse(new String[]{"test", "--model", "model.gguf"});
        assertTrue(result instanceof CliArgs.DetectFromDataset);
        var cmd = (CliArgs.DetectFromDataset) result;
        assertEquals(10, cmd.sampleCount());
    }

    @Test
    public void detectMissingModelReturnsHelp() {
        var result = CliArgs.parse(new String[]{"detect", "digit.png"});
        assertTrue(result instanceof CliArgs.Help);
    }

    @Test
    public void unknownCommandReturnsHelp() {
        var result = CliArgs.parse(new String[]{"unknown"});
        assertTrue(result instanceof CliArgs.Help);
    }
}
