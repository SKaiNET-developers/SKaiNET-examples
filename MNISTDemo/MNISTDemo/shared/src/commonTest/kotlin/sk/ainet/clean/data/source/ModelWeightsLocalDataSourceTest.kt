package sk.ainet.clean.data.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import sk.ainet.clean.data.io.ResourceReader
import sk.ainet.clean.domain.model.ModelId

private class RecordingResourceReader(private val map: Map<String, ByteArray>) : ResourceReader {
    var lastPath: String? = null
    override suspend fun read(path: String): ByteArray? {
        lastPath = path
        return map[path]
    }
}

class ModelWeightsLocalDataSourceTest {

    @Test
    fun reads_mlp_weights_from_expected_resource_path() = runTest {
        // Arrange: expected path and stubbed bytes
        val expectedPath = "files/mnist_mlp.gguf"
        val expectedBytes = byteArrayOf(4, 2, 0, 2, 5)

        val reader = RecordingResourceReader(mapOf(expectedPath to expectedBytes))
        val pathResolver: (ModelId) -> String = { id ->
            when (id.value) {
                ModelId.MLP_MNIST.value -> expectedPath
                else -> error("Unsupported ${id.value}")
            }
        }
        val local = ModelWeightsLocalDataSource(reader, pathResolver)

        // Act: read MLP weights
        val bytes = local.read(ModelId.MLP_MNIST)

        // Assert: path used and content
        assertEquals(expectedPath, reader.lastPath, "ResourceReader must be called with the MLP weights path")
        assertNotNull(bytes, "Bytes must be returned for existing MLP weights resource")
        assertEquals(expectedBytes.toList(), bytes!!.toList(), "Returned bytes must match resource content")
    }
}
