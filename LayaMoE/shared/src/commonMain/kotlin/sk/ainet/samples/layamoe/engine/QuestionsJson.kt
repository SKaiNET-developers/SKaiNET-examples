package sk.ainet.samples.layamoe.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sk.ainet.models.laya.LayaQuestion
import sk.ainet.models.laya.LayaState
import sk.ainet.models.laya.PythonJson

/**
 * The request shape of the Laya Python SDK and the layaMOE page, as typed into the questions box:
 *
 * ```json
 * {
 *   "destructive": {"type": "noul", "instructions": "The action is destructive and cannot be undone"},
 *   "risk": {"type": "score", "instructions": "How risky is this action?", "criteria": ["Low", "Medium", "High", "Critical"]},
 *   "team": {"type": "choice", "instructions": "Which team?", "criteria": {"bug": "Something is broken", "how_to": "A usage question"}}
 * }
 * ```
 *
 * `choice` criteria are an object (key → description) or an array of keys; `score` criteria are the
 * level texts in order; `noul` may carry `criteria` `{"false": …, "true": …}` and `labels`. A
 * structured instruction is serialised the way the reference does (`PythonJson`).
 */
object QuestionsJson {
    val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true; prettyPrintIndent = "  " }

    class InvalidQuestions(message: String) : IllegalArgumentException(message)

    /** Text as given; a value that parses as a JSON object or array becomes a structured state. */
    fun parseState(raw: String): LayaState {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val element = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
            if (element != null) return LayaState.of(element)
        }
        return LayaState.Text(raw)
    }

    fun parseQuestions(raw: String): Map<String, LayaQuestion> {
        val root = try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            throw InvalidQuestions("questions are not valid JSON: ${e.message}")
        }
        val obj = root as? JsonObject ?: throw InvalidQuestions("questions must be a JSON object keyed by question id")
        if (obj.isEmpty()) throw InvalidQuestions("at least one question is needed")
        val out = LinkedHashMap<String, LayaQuestion>()
        for ((id, q) in obj) {
            val def = q as? JsonObject ?: throw InvalidQuestions("question '$id' must be an object")
            out[id] = question(id, def)
        }
        return out
    }

    fun question(id: String, q: JsonObject): LayaQuestion {
        val insElement = q["instructions"] ?: throw InvalidQuestions("question '$id' has no instructions")
        val ins = if (insElement is JsonPrimitive && insElement.isString) insElement.content else PythonJson.dumps(insElement)
        val crit = q["criteria"]
        return when (val type = q["type"]?.jsonPrimitive?.content) {
            "choice" -> when (crit) {
                is JsonArray -> LayaQuestion.Choice(ins, crit.map { it.jsonPrimitive.content to null })
                is JsonObject -> LayaQuestion.Choice(ins, crit.entries.map { (k, v) -> k to v.takeIf { it !is JsonNull } })
                else -> throw InvalidQuestions("choice question '$id' needs criteria (an object key → description, or an array of keys)")
            }
            "score" -> LayaQuestion.Score(ins, (crit as? JsonArray ?: throw InvalidQuestions("score question '$id' needs criteria (an array of level texts)")).toList())
            "noul" -> {
                val c = crit as? JsonObject
                val labels = (q["labels"] as? JsonObject)?.let { it["false"]!!.jsonPrimitive.content to it["true"]!!.jsonPrimitive.content }
                fun get(key: String): JsonElement? = c?.entries?.firstOrNull { it.key.lowercase() == key }?.value?.takeIf { it !is JsonNull }
                LayaQuestion.Noul(ins, get("false"), get("true"), labels)
            }
            null -> throw InvalidQuestions("question '$id' has no type")
            else -> throw InvalidQuestions("question '$id': unknown type '$type' (choice, score, noul)")
        }
    }

    /** The option labels the UI shows for an answer's probabilities, in option order. */
    fun optionLabels(question: LayaQuestion): List<String> = when (question) {
        is LayaQuestion.Choice -> question.keys
        is LayaQuestion.Score -> question.levels.map { LayaQuestion.renderCriterion(it) }
        is LayaQuestion.Noul -> listOf("no", "yes")
    }

    /** Pretty-prints a questions object, e.g. a preset, for the editor. */
    fun pretty(raw: String): String = runCatching { json.encodeToString(JsonElement.serializer(), json.parseToJsonElement(raw)) }.getOrDefault(raw)

    fun prettyState(raw: String): String {
        val trimmed = raw.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return raw
        return pretty(raw)
    }
}
