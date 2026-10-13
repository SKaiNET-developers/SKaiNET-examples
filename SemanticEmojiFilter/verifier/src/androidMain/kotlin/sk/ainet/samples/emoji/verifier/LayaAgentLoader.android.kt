package sk.ainet.samples.emoji.verifier

import android.content.Context
import java.io.File
import kotlinx.coroutines.runBlocking
import sk.ainet.context.ExecutionContext
import sk.ainet.io.AndroidRandomAccessSource
import sk.ainet.io.safetensors.SafeTensorsParametersLoader
import sk.ainet.io.weights.WeightTensor
import sk.ainet.lang.types.FP32
import sk.ainet.models.laya.LayaAgent
import sk.ainet.models.laya.LayaCheckpointFiles
import sk.ainet.models.laya.LayaDecisionModel
import sk.ainet.models.laya.LayaPreprocessor
import sk.ainet.models.laya.LayaWeightPreparation

/**
 * The library's `LayaModelLoader` is JVM-only (java.nio.file); this is the same six steps over
 * Android's file source. F16 weights are up-cast to FP32 on load, as the reference does on CPU.
 */
actual fun loadLayaAgent(directory: String, ctx: ExecutionContext): LayaAgent<FP32> {
    val dir = File(directory)
    fun text(path: String) = File(dir, path).readText()
    val files = LayaCheckpointFiles(
        agentConfigJson = text("rl_agent_config.json"),
        encoderConfigJson = text("encoder/config.json"),
        tokenizerJson = text("tokenizer/tokenizer.json"),
        tokenizerConfigJson = text("tokenizer/tokenizer_config.json"),
    )
    val preprocessor = LayaPreprocessor.fromFiles(files)
    val weights = File(dir, "model.safetensors")
    require(weights.isFile) { "no model.safetensors in $directory" }
    val raw = ArrayList<WeightTensor<FP32, Float>>()
    runBlocking {
        SafeTensorsParametersLoader(sourceProvider = { AndroidRandomAccessSource.open(weights) })
            .load<FP32, Float>(ctx, FP32::class) { name, tensor ->
                raw += WeightTensor(name, tensor.shape.dimensions.toList(), tensor)
            }
    }
    val prepared = LayaWeightPreparation.prepare(raw, preprocessor.encoderConfig, preprocessor.agentConfig, ctx, FP32::class)
    val model = LayaDecisionModel.build(preprocessor.encoderConfig, preprocessor.agentConfig, prepared, ctx, FP32::class, exactGelu = true)
    return LayaAgent(preprocessor, model)
}

/** Android: the checkpoint lands in `filesDir/laya-multilingual`. */
fun layaAndroidSource(context: Context): DecisionAdapterSource = LayaFileSource(File(context.filesDir, "laya-multilingual"))
