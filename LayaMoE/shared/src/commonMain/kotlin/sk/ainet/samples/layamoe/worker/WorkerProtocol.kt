package sk.ainet.samples.layamoe.worker

import kotlinx.serialization.json.Json
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult

/**
 * The string protocol between the web page and the mixture's Web Worker (`webWorker/.../Main.kt`).
 * Page to worker: `LOAD`, `ASK:<id>:<AskRequest json>`. Worker to page: `STATE:<kind>:<text>`
 * while loading (`loading`, `error`), `READY:<kernel tier>`, `RESULT:<id>:<AskResult json>`,
 * `ERROR:<id or ->:<message>`. Ids let the page match answers to requests.
 */
object WorkerProtocol {
    const val LOAD = "LOAD"
    private val json = Json { ignoreUnknownKeys = true }

    fun ask(id: Int, request: AskRequest): String = "ASK:$id:" + json.encodeToString(AskRequest.serializer(), request)
    fun result(id: Int, result: AskResult): String = "RESULT:$id:" + json.encodeToString(AskResult.serializer(), result)
    fun ready(kernelTier: String): String = "READY:$kernelTier"
    fun error(id: Int?, message: String): String = "ERROR:${id ?: "-"}:$message"
    fun loading(message: String): String = "STATE:loading:$message"
    fun failed(message: String): String = "STATE:error:$message"

    fun parseAsk(raw: String): Pair<Int, AskRequest>? = parseTagged(raw, "ASK:")?.let { (id, body) -> id to json.decodeFromString(AskRequest.serializer(), body) }
    fun parseResult(raw: String): Pair<Int, AskResult>? = parseTagged(raw, "RESULT:")?.let { (id, body) -> id to json.decodeFromString(AskResult.serializer(), body) }
    fun parseReady(raw: String): String? = if (raw.startsWith("READY:")) raw.removePrefix("READY:") else null

    /** `(id or null, message)` of an `ERROR` message, or null for anything else. */
    fun parseError(raw: String): Pair<Int?, String>? {
        if (!raw.startsWith("ERROR:")) return null
        val rest = raw.removePrefix("ERROR:")
        val colon = rest.indexOf(':')
        if (colon < 0) return null to rest
        return rest.substring(0, colon).toIntOrNull() to rest.substring(colon + 1)
    }

    /** `(kind, text)` of a `STATE` message, or null for anything else. */
    fun parseState(raw: String): Pair<String, String>? {
        if (!raw.startsWith("STATE:")) return null
        val rest = raw.removePrefix("STATE:")
        val colon = rest.indexOf(':')
        return if (colon < 0) rest to "" else rest.substring(0, colon) to rest.substring(colon + 1)
    }

    private fun parseTagged(raw: String, tag: String): Pair<Int, String>? {
        if (!raw.startsWith(tag)) return null
        val rest = raw.removePrefix(tag)
        val colon = rest.indexOf(':')
        if (colon < 0) return null
        val id = rest.substring(0, colon).toIntOrNull() ?: return null
        return id to rest.substring(colon + 1)
    }
}
