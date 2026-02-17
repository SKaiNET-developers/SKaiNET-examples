package sk.ainet.examples.mnist;

/**
 * Sealed interface representing parsed CLI arguments.
 * Demonstrates Java 21 sealed interfaces with records and pattern matching.
 */
public sealed interface CliArgs {

    record Help() implements CliArgs {}

    record DetectFromImage(String imagePath, String modelPath, boolean invert) implements CliArgs {}

    record DetectFromDataset(String modelPath, int sampleCount) implements CliArgs {}

    static CliArgs parse(String[] args) {
        if (args.length == 0) {
            return new Help();
        }

        return switch (args[0]) {
            case "--help", "-h" -> new Help();
            case "detect" -> parseDetect(args);
            case "test" -> parseTest(args);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                yield new Help();
            }
        };
    }

    private static CliArgs parseDetect(String[] args) {
        String modelPath = null;
        String imagePath = null;
        var invert = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--model" -> {
                    if (++i < args.length) modelPath = args[i];
                }
                case "--invert" -> invert = true;
                default -> {
                    if (imagePath == null) imagePath = args[i];
                }
            }
        }

        if (modelPath == null || imagePath == null) {
            System.err.println("detect requires --model <path> and an image path");
            return new Help();
        }
        return new DetectFromImage(imagePath, modelPath, invert);
    }

    private static CliArgs parseTest(String[] args) {
        String modelPath = null;
        var count = 10;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--model" -> {
                    if (++i < args.length) modelPath = args[i];
                }
                case "--count" -> {
                    if (++i < args.length) count = Integer.parseInt(args[i]);
                }
                default -> { }
            }
        }

        if (modelPath == null) {
            System.err.println("test requires --model <path>");
            return new Help();
        }
        return new DetectFromDataset(modelPath, count);
    }
}
