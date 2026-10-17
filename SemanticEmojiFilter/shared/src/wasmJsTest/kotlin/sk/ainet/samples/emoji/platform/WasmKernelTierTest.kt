package sk.ainet.samples.emoji.platform

import kotlinx.coroutines.test.runTest
import sk.ainet.exec.wasm.WasmSimd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The web build must not fall back to the scalar lane silently: the sidecar installs and the label says so. */
class WasmKernelTierTest {
    @Test
    fun sidecarInstallsAndLabelReportsIt() = runTest {
        val messages = mutableListOf<String>()
        installPlatformKernels { messages += it }
        assertTrue(WasmSimd.isInstalled, "WasmSimd.install() did not take effect")
        assertTrue(kernelTierLabel().startsWith("Wasm SIMD128"), "label: ${kernelTierLabel()}")
        assertEquals(1, messages.size, "one progress message while the sidecar loads")
    }
}
