package sk.ainet.samples.layamoe.engine

import kotlinx.serialization.Serializable
import sk.ainet.models.laya.LayaAnswer
import sk.ainet.models.laya.LayaMixtureResult
import sk.ainet.models.laya.LayaQuestion
import sk.ainet.models.laya.LayaRouting

/** One request as the UI, the CLI and the Web Worker exchange it: raw text, parsed by the service. */
@Serializable
data class AskRequest(
    /** Plain text, or a JSON object / array for a structured state. */
    val state: String,
    /** The questions object in the SDK shape (see [QuestionsJson]). */
    val questions: String,
    /** `general`, an expert name, or null for the router's choice. */
    val expert: String? = null,
    /** Also answer with the general head on the same encoder output. */
    val compare: Boolean = false,
)

/** One decoded answer; `probabilities` are keyed by option (choice keys, score level index, `no`/`yes`). */
@Serializable
data class AnswerDto(
    val type: String,
    val choice: String? = null,
    /** The expected level of a score question. */
    val score: Double? = null,
    /** `P(true)` of a noul question. */
    val noul: Double? = null,
    val probabilities: Map<String, Double>,
    val confidence: Double,
) {
    /** The option labels in order, for bars. */
    val keys: List<String> get() = probabilities.keys.toList()

    /** A one-line headline the way the original page prints it. */
    fun headline(): String = when (type) {
        "choice" -> choice ?: "?"
        "score" -> "score ${score?.let { formatFixed(it, 2) } ?: "?"}"
        else -> "P(true) ${noul?.let { formatFixed(it * 100, 1) } ?: "?"} %"
    }
}

@Serializable
data class RoutingDto(
    val probabilities: Map<String, Double>,
    val top: String,
    val topProbability: Double,
    val confidence: Double,
    val threshold: Double,
    val routerPick: String,
    val chosen: String,
    val overridden: Boolean,
) {
    /** The five most probable kinds with the expert each maps to. */
    fun topKinds(n: Int = 5): List<Triple<String, Double, String>> =
        probabilities.entries.sortedByDescending { it.value }.take(n).map { Triple(it.key, it.value, LayaMoeRouter.expertFor(it.key)) }
}

@Serializable
data class AskResult(
    /** The head that answered. */
    val expert: String,
    val routing: RoutingDto? = null,
    val answers: Map<String, AnswerDto>,
    /** The general head's answers when a comparison was asked for and an expert answered. */
    val generalAnswers: Map<String, AnswerDto>? = null,
    /** Option labels per question, in option order, so the UI can draw bars without re-parsing. */
    val optionLabels: Map<String, List<String>> = emptyMap(),
    val questionTypes: Map<String, String> = emptyMap(),
    val encoderMillis: Long,
    val headsMillis: Long,
    val totalMillis: Long,
    val inputTokens: Int,
    val kernelTier: String,
)

fun LayaAnswer.toDto(question: LayaQuestion): AnswerDto = when (this) {
    is LayaAnswer.Choice -> AnswerDto("choice", choice = choice, probabilities = keys.indices.associate { keys[it] to probabilities[it] }, confidence = confidence)
    is LayaAnswer.Score -> AnswerDto("score", score = score, probabilities = probabilities.indices.associate { it.toString() to probabilities[it] }, confidence = confidence)
    is LayaAnswer.Noul -> AnswerDto("noul", noul = noul, probabilities = mapOf("no" to probabilities[0], "yes" to probabilities[1]), confidence = confidence)
}

fun LayaRouting.toDto(): RoutingDto = RoutingDto(probabilities, top, topProbability, confidence, threshold, routerPick, chosen, overridden)

fun LayaMixtureResult.toDto(questions: Map<String, LayaQuestion>, totalMillis: Long, kernelTier: String): AskResult = AskResult(
    expert = expert,
    routing = routing?.toDto(),
    answers = answers.mapValues { (id, a) -> a.toDto(questions.getValue(id)) },
    generalAnswers = generalAnswers?.mapValues { (id, a) -> a.toDto(questions.getValue(id)) },
    optionLabels = questions.mapValues { (_, q) -> QuestionsJson.optionLabels(q) },
    questionTypes = questions.mapValues { (_, q) -> q.type.wire },
    encoderMillis = encoderMillis,
    headsMillis = headsMillis,
    totalMillis = totalMillis,
    inputTokens = inputTokens,
    kernelTier = kernelTier,
)

/** `value` with [decimals] fraction digits, without platform formatting. */
fun formatFixed(value: Double, decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val rounded = kotlin.math.round(value * factor) / factor
    val whole = rounded.toLong()
    if (decimals == 0) return whole.toString()
    val fraction = kotlin.math.abs(kotlin.math.round((rounded - whole) * factor)).toLong()
    return "${if (rounded < 0 && whole == 0L) "-" else ""}$whole.${fraction.toString().padStart(decimals, '0')}"
}
