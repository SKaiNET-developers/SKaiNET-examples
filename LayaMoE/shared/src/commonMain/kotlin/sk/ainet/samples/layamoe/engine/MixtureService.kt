package sk.ainet.samples.layamoe.engine

import kotlin.time.TimeSource
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.lang.memory.plan.WeightForm
import sk.ainet.lang.types.FP32
import sk.ainet.models.laya.LayaCheckpointFiles
import sk.ainet.models.laya.LayaExpert
import sk.ainet.models.laya.LayaExpertLoader
import sk.ainet.models.laya.LayaMixtureAgent
import sk.ainet.models.laya.LayaModelLoader
import sk.ainet.models.laya.LayaWeightForms
import sk.ainet.samples.layamoe.model.LayaMoeCheckpoints
import sk.ainet.samples.layamoe.model.ModelFiles
import sk.ainet.samples.layamoe.platform.installPlatformKernels
import sk.ainet.samples.layamoe.platform.kernelTierLabel
import sk.ainet.samples.layamoe.platform.logEvent

/** How the weights are held: FP32 is the reference lane; Q8_0 packs the projections at load (a quarter of the bytes). */
enum class WeightLane(val label: String) {
    FP32("FP32"),
    Q8_0("Q8_0");

    val weightFormFor: ((String) -> WeightForm?)? get() = if (this == Q8_0) LayaWeightForms.q8_0For else null
}

/**
 * The mixture on this process: loads the base Laya agent and the expert heads from [files] onto
 * [lane], then answers [AskRequest]s through `LayaMixtureAgent`. Same code on desktop, Android and
 * inside the browser's Web Worker.
 */
class MixtureService(
    private val files: ModelFiles,
    val lane: WeightLane,
    private val newContext: () -> ExecutionContext = { DirectCpuExecutionContext() },
) {
    var mixture: LayaMixtureAgent<FP32>? = null
        private set

    val isLoaded: Boolean get() = mixture != null

    /** Downloads what is missing, builds the base agent and the experts; returns the kernel tier label. */
    suspend fun load(onProgress: (String) -> Unit): String {
        mixture?.let { return kernelTierLabel() }
        installPlatformKernels(onProgress)
        files.ensure(LayaMoeCheckpoints.allFiles, onProgress)

        val checkpoint = LayaCheckpointFiles(
            agentConfigJson = files.text(LayaMoeCheckpoints.baseAgentConfig),
            encoderConfigJson = files.text(LayaMoeCheckpoints.baseEncoderConfig),
            tokenizerJson = files.text(LayaMoeCheckpoints.baseTokenizer),
            tokenizerConfigJson = files.text(LayaMoeCheckpoints.baseTokenizerConfig),
        )
        val ctx = newContext()
        onProgress("building the base model (${lane.label}, exact GELU)…")
        val started = TimeSource.Monotonic.markNow()
        val base = LayaModelLoader.load(
            files = checkpoint,
            weights = files.weights(LayaMoeCheckpoints.baseWeights),
            ctx = ctx,
            exactGelu = true,
            weightFormFor = lane.weightFormFor,
        )
        val baseMs = started.elapsedNow().inWholeMilliseconds
        val experts = ArrayList<LayaExpert<FP32>>()
        for (name in LayaMoeCheckpoints.EXPERT_NAMES) {
            onProgress("binding the $name head…")
            experts += LayaExpertLoader.load(
                metaJson = files.text(LayaMoeCheckpoints.expertMeta(name)),
                weights = files.weights(LayaMoeCheckpoints.expertWeights(name)),
                base = base,
                ctx = ctx,
                weightFormFor = lane.weightFormFor,
            )
        }
        val agent = LayaMixtureAgent(base, experts, LayaMoeRouter.SPEC)
        mixture = agent
        logEvent("mixture_loaded", "lane" to lane.label, "baseMs" to baseMs, "totalMs" to started.elapsedNow().inWholeMilliseconds, "experts" to agent.experts.keys.joinToString("+"), "tier" to kernelTierLabel())
        return kernelTierLabel()
    }

    /** Answers one request; the questions and the state are parsed here (an invalid request throws). */
    fun ask(request: AskRequest): AskResult {
        val agent = mixture ?: error("the mixture is not loaded")
        val questions = QuestionsJson.parseQuestions(request.questions)
        val state = QuestionsJson.parseState(request.state)
        val expert = request.expert?.takeIf { it.isNotBlank() && it != "auto" }
        val started = TimeSource.Monotonic.markNow()
        val result = agent.decide(state, questions, expert = expert, compare = request.compare)
        val total = started.elapsedNow().inWholeMilliseconds
        logEvent("mixture_ask", "expert" to result.expert, "routerTop" to result.routing?.top, "questions" to questions.size, "encoderMs" to result.encoderMillis, "headsMs" to result.headsMillis, "totalMs" to total, "tokens" to result.inputTokens)
        return result.toDto(questions, total, kernelTierLabel())
    }
}
