package sk.ainet.cli.io

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FileResourceReaderTest {

    @Test
    fun `read returns file contents when file exists`() = runTest {
        val tempDir = createTempDirectory()
        try {
            val testContent = byteArrayOf(1, 2, 3, 4, 5)
            val testFile = File(tempDir, "test.bin")
            testFile.writeBytes(testContent)

            val reader = FileResourceReader(tempDir)
            val result = reader.read("test.bin")

            assertNotNull(result)
            assertEquals(testContent.toList(), result.toList())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `read returns file contents from nested path`() = runTest {
        val tempDir = createTempDirectory()
        try {
            val testContent = byteArrayOf(10, 20, 30)
            val nestedDir = File(tempDir, "files")
            nestedDir.mkdirs()
            val testFile = File(nestedDir, "model.gguf")
            testFile.writeBytes(testContent)

            val reader = FileResourceReader(tempDir)
            val result = reader.read("files/model.gguf")

            assertNotNull(result)
            assertEquals(testContent.toList(), result.toList())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `read returns null when file does not exist`() = runTest {
        val tempDir = createTempDirectory()
        try {
            val reader = FileResourceReader(tempDir)
            val result = reader.read("nonexistent.bin")

            assertNull(result)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `read returns null when path is a directory`() = runTest {
        val tempDir = createTempDirectory()
        try {
            val subDir = File(tempDir, "subdir")
            subDir.mkdirs()

            val reader = FileResourceReader(tempDir)
            val result = reader.read("subdir")

            assertNull(result)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun createTempDirectory(): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        return tempDir
    }
}
