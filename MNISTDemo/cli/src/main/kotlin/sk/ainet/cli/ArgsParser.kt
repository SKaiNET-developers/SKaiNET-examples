package sk.ainet.cli

/**
 * CLI argument parser for MNIST classifier.
 */
object ArgsParser {

    /**
     * Parses command line arguments into a CliConfig.
     */
    fun parse(args: Array<String>): CliConfig {
        var model = "mlp"
        var weightsDir: String? = null
        var modelFile: String? = null
        var imagePath: String? = null
        var invert = false
        var debug = false
        var help = false

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--model" -> {
                    i++
                    if (i < args.size) {
                        model = args[i]
                    }
                }
                "--weights-dir" -> {
                    i++
                    if (i < args.size) {
                        weightsDir = args[i]
                    }
                }
                "--model-file" -> {
                    i++
                    if (i < args.size) {
                        modelFile = args[i]
                    }
                }
                "--invert" -> invert = true
                "--debug" -> debug = true
                "--help", "-h" -> help = true
                else -> {
                    if (!args[i].startsWith("-")) {
                        imagePath = args[i]
                    }
                }
            }
            i++
        }

        return CliConfig(model, weightsDir, modelFile, imagePath, invert, debug, help)
    }
}

/**
 * Configuration parsed from CLI arguments.
 */
data class CliConfig(
    val model: String = "mlp",
    val weightsDir: String? = null,
    val modelFile: String? = null,
    val imagePath: String? = null,
    val invert: Boolean = false,
    val debug: Boolean = false,
    val help: Boolean = false,
)
