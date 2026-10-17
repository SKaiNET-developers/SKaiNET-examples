package sk.ainet.samples.emoji.verifier

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.decisions.DecisionAdapter
import sk.ainet.exec.wasm.WasmKernels
import sk.ainet.exec.wasm.WasmSimd
import sk.ainet.models.laya.LayaCheckpointFiles
import sk.ainet.models.laya.LayaDecisionAdapter
import sk.ainet.models.laya.LayaModelLoader
import sk.ainet.models.laya.LayaWeightForms
import sk.ainet.samples.emoji.platform.logEvent
import kotlin.time.TimeSource

/**
 * The checkpoint fetched from Hugging Face into the browser cache and built on the packed Q8_0
 * lane (SKEEP-007): about 340 MB of weights in memory instead of 1.3 GB of FP32, served by the
 * SIMD128 sidecar. Runs inside the Laya Web Worker, never on the page's thread.
 */
class LayaWebSource(
    /**
     * Gets every progress line as it happens, in addition to [open]'s callback. The worker posts
     * them to the page straight away: a `StateFlow` collector on the worker's single thread
     * cannot run while the synchronous read and quantize work is going on, so it would conflate
     * eighty seconds of progress into the first and the last line.
     */
    private val report: (String) -> Unit = {},
) : DecisionAdapterSource {
    override val modelId: String = EmojiMatchSchema.MODEL_ID

    override suspend fun open(onProgress: (String) -> Unit): DecisionAdapter = openReporting { line ->
        report(line)
        onProgress(line)
    }

    private suspend fun openReporting(onProgress: (String) -> Unit): DecisionAdapter {
        onProgress("loading the SIMD128 kernel sidecar…")
        try {
            WasmSimd.install()
        } catch (e: Throwable) {
            logEvent("laya_sidecar_failed", "reason" to (e.message ?: e::class.simpleName))
        }
        logEvent("laya_kernels", "mode" to WasmKernels.mode)
        onProgress("kernels: ${WasmKernels.mode}")

        val texts = HashMap<String, String>()
        for (file in LayaCheckpoint.FILES) {
            if (file == WEIGHTS) continue
            onProgress("fetching Laya ${file.substringAfterLast('/')}…")
            texts[file] = WebFiles.text(WebFiles.fetchCached(LayaCheckpoint.url(file)) { _, _ -> })
        }
        val files = LayaCheckpointFiles(
            agentConfigJson = texts.getValue("rl_agent_config.json"),
            encoderConfigJson = texts.getValue("encoder/config.json"),
            tokenizerJson = texts.getValue("tokenizer/tokenizer.json"),
            tokenizerConfigJson = texts.getValue("tokenizer/tokenizer_config.json"),
        )

        val totalMb = (LayaCheckpoint.SIZES.getValue(WEIGHTS) / 1_000_000).toString()
        var lastMb = -1L
        val blob = WebFiles.fetchCached(LayaCheckpoint.url(WEIGHTS)) { received, _ ->
            val mb = (received / 1_000_000).toLong()
            if (mb != lastMb) {
                lastMb = mb
                onProgress("downloading Laya weights… $mb / $totalMb MB")
            }
        }
        val readStart = TimeSource.Monotonic.markNow()
        val chunks = WebFiles.chunks(blob) { done, total -> onProgress("reading Laya weights… ${done / 1_000_000} / ${total / 1_000_000} MB") }
        logEvent("laya_weights_read", "bytes" to WebFiles.size(blob), "ms" to readStart.elapsedNow().inWholeMilliseconds)

        onProgress("building $modelId (Q8_0, exact GELU)…")
        val started = TimeSource.Monotonic.markNow()
        val agent = LayaModelLoader.load(
            files = files,
            weights = { sk.ainet.io.ByteArrayRandomAccessSource(chunks) },
            ctx = DirectCpuExecutionContext(),
            exactGelu = true,
            weightFormFor = LayaWeightForms.q8_0For,
        )
        logEvent("laya_loaded", "lane" to "q8_0", "loadMs" to started.elapsedNow().inWholeMilliseconds)
        return LayaDecisionAdapter(agent, modelId)
    }

    private companion object {
        const val WEIGHTS = "model.safetensors"
    }
}
