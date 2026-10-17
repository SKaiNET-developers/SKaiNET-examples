package sk.ainet.samples.emoji.verify

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sk.ainet.samples.emoji.domain.CatalogItem

/** One verification request, as the web build sends it to its Laya worker. */
@Serializable
data class DecideRequest(val query: String, val item: CatalogItem)

/**
 * The string protocol between the web app and the Laya Web Worker (see `webVerifier/.../Main.kt`).
 * Main thread to worker: `LOAD`, `DECIDE:<id>:<DecideRequest json>`. Worker to main thread:
 * `STATE:<kind>:<text>` while loading (`loading`, `error`), `READY`, `VERDICT:<id>:<Verdict json>`,
 * `ERROR:<id or ->:<message>`. The id lets the main thread drop a verdict for a request it has
 * cancelled meanwhile.
 */
object WorkerProtocol {
    const val LOAD = "LOAD"
    const val READY = "READY"
    private val json = Json { ignoreUnknownKeys = true }

    fun decide(id: Int, request: DecideRequest): String = "DECIDE:$id:" + json.encodeToString(DecideRequest.serializer(), request)
    fun verdict(id: Int, verdict: Verdict): String = "VERDICT:$id:" + json.encodeToString(Verdict.serializer(), verdict)
    fun error(id: Int?, message: String): String = "ERROR:${id ?: "-"}:$message"
    fun loading(message: String): String = "STATE:loading:$message"
    fun failed(message: String): String = "STATE:error:$message"

    /** `(id, request)` of a `DECIDE` message, or null for anything else. */
    fun parseDecide(raw: String): Pair<Int, DecideRequest>? = parseTagged(raw, "DECIDE:")?.let { (id, body) -> id to json.decodeFromString(DecideRequest.serializer(), body) }
    fun parseVerdict(raw: String): Pair<Int, Verdict>? = parseTagged(raw, "VERDICT:")?.let { (id, body) -> id to json.decodeFromString(Verdict.serializer(), body) }
    /** `(id or null, message)` of an `ERROR` message, or null for anything else. */
    fun parseError(raw: String): Pair<Int?, String>? {
        if (!raw.startsWith("ERROR:")) return null
        val rest = raw.removePrefix("ERROR:")
        val colon = rest.indexOf(':')
        if (colon < 0) return null to rest
        return rest.substring(0, colon).toIntOrNull() to rest.substring(colon + 1)
    }
    /** The [VerifierState] a `STATE` message carries, or null for anything else. */
    fun parseState(raw: String): VerifierState? {
        if (!raw.startsWith("STATE:")) return null
        val rest = raw.removePrefix("STATE:")
        val colon = rest.indexOf(':')
        val kind = if (colon < 0) rest else rest.substring(0, colon)
        val text = if (colon < 0) "" else rest.substring(colon + 1)
        return when (kind) {
            "loading" -> VerifierState.Loading(text)
            "error" -> VerifierState.Error(text)
            else -> null
        }
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
