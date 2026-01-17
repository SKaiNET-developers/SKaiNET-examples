package sk.ainet.cli.io

import sk.ainet.clean.data.io.ResourceReader
import java.io.File

/**
 * File-system based ResourceReader for CLI application.
 * Reads model weights from a specified base directory.
 */
class FileResourceReader(private val baseDir: File) : ResourceReader {

    override suspend fun read(path: String): ByteArray? {
        val file = File(baseDir, path)
        return if (file.exists() && file.isFile) {
            file.readBytes()
        } else {
            null
        }
    }
}
