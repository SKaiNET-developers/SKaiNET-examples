package sk.ainet.samples.emoji.cli

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.verifier.LayaVerifier
import sk.ainet.samples.emoji.verifier.layaDesktopSource

internal fun verifyAvailable(): Boolean = true

/** Runs the Laya verifier over [items] for [query] and prints every verdict with its timing. */
internal fun verifyCommand(query: String, items: List<CatalogItem>, flags: Map<String, String> = emptyMap()) {
    val schema = sk.ainet.samples.emoji.verifier.EmojiMatchSchema.schema(
        statement = flags["statement"] ?: sk.ainet.samples.emoji.verifier.EmojiMatchSchema.STATEMENT,
        whenFalse = flags["no"] ?: sk.ainet.samples.emoji.verifier.EmojiMatchSchema.WHEN_FALSE,
        whenTrue = flags["yes"] ?: sk.ainet.samples.emoji.verifier.EmojiMatchSchema.WHEN_TRUE,
    )
    val verifier = LayaVerifier(layaDesktopSource(), schema)
    val started = System.nanoTime()
    var loadMs = -1L
    runBlocking {
        verifier.verify(query, items).collect { v ->
            if (loadMs < 0) loadMs = (System.nanoTime() - started) / 1_000_000 - v.millis
            val item = items.first { it.id == v.itemId }
            val p = v.probabilityTrue?.let { "%.2f".format(it) } ?: "fail"
            val verdict = when (v.accepted) { true -> "YES"; false -> "no "; null -> "?  " }
            println("  ${item.emoji}  $verdict  P(match)=$p  ${v.millis} ms  ${item.nameEn}")
        }
    }
    println("Laya load ${loadMs} ms, ${items.size} decisions in ${(System.nanoTime() - started) / 1_000_000 - loadMs} ms")
    val rss = java.io.File("/proc/self/status").takeIf { it.isFile }?.readLines()?.firstOrNull { it.startsWith("VmRSS:") }?.split(Regex("\\s+"))?.get(1)?.toLong()?.div(1024)
    if (rss != null) println("RSS: $rss MB")
    verifier.close()
}
