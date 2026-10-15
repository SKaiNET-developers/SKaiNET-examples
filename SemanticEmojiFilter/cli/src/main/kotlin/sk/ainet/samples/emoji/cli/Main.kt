package sk.ainet.samples.emoji.cli

import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToLong
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import sk.ainet.models.bert.BertExecutionMode
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.Catalog
import sk.ainet.samples.emoji.domain.Tier
import sk.ainet.samples.emoji.engine.CatalogIndex
import sk.ainet.samples.emoji.engine.LeafSource
import sk.ainet.samples.emoji.engine.LeafEmbeddingEngine
import sk.ainet.samples.emoji.model.DesktopLeafProvider
import sk.ainet.samples.emoji.model.defaultLeafCacheDir
import sk.ainet.samples.emoji.platform.kernelTierLabel

private const val USAGE = """
Semantic Emoji Filter — JVM spike and tooling over the shared engine.

  bench         [--model DIR] [--mode direct|optimized] [--repeats N]   load, catalog, query latency, RSS
  embed-catalog [--model DIR]                                           write catalog-embeddings.bin + sha256
  calibrate     [--model DIR] [--labels FILE]                            fit calibration.json on labelled queries
  golden        [--model DIR]                                           write golden-queries.json for the ranking test
  query         [--model DIR] "<text>"                                  print the top 10
  verify        [--model DIR] [--top N] "<text>"                        Laya verdicts for the top N (needs -PlayaVerifier=true)

Options: --catalog-dir DIR (default: ../composeApp/src/commonMain/composeResources/files/catalog)
         --model DIR       (default: ${'$'}LEAF_MODEL_DIR or ~/.skainet-examples/semantic-emoji-filter/leaf;
                            missing files are downloaded)
"""

private val BENCH_QUERIES = listOf(
    "healthy things to eat", "junk food", "feeling sick", "rock band", "cute pets", "rainy weather",
    "going on holiday", "team sports", "very happy", "working late at the office",
    "gesundes Essen", "mir ist schlecht", "Rockkonzert", "Haustiere", "Urlaub am Meer",
    "Geburtstagsfeier", "winter sports", "breakfast", "something sweet", "fruit",
)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(USAGE.trimIndent())
        return
    }
    val opts = Options.parse(args.drop(1))
    when (args[0]) {
        "bench" -> bench(opts)
        "embed-catalog" -> embedCatalog(opts)
        "calibrate" -> calibrate(opts)
        "golden" -> golden(opts)
        "query" -> query(opts)
        "tokens" -> tokens(opts)
        "verify" -> verify(opts)
        else -> {
            System.err.println("unknown command '${args[0]}'")
            println(USAGE.trimIndent())
            System.exit(2)
        }
    }
}

private class Options(val flags: Map<String, String>, val positional: List<String>) {
    val modelDir: String get() = flags["model"] ?: defaultLeafCacheDir()
    val catalogDir: File get() = File(flags["catalog-dir"] ?: "../composeApp/src/commonMain/composeResources/files/catalog").canonicalFile
    val mode: BertExecutionMode get() = if (flags["mode"] == "optimized") BertExecutionMode.OPTIMIZED else BertExecutionMode.DIRECT
    val repeats: Int get() = flags["repeats"]?.toInt() ?: 5

    companion object {
        fun parse(args: List<String>): Options {
            val flags = LinkedHashMap<String, String>()
            val positional = ArrayList<String>()
            var i = 0
            while (i < args.size) {
                val a = args[i]
                if (a.startsWith("--") && i + 1 < args.size) {
                    flags[a.removePrefix("--")] = args[i + 1]
                    i += 2
                } else {
                    positional += a
                    i++
                }
            }
            return Options(flags, positional)
        }
    }
}

private fun loadCatalog(opts: Options): Catalog = Catalog.parse(File(opts.catalogDir, "catalog.json").readText())

private fun loadEngine(opts: Options): LeafEmbeddingEngine = runBlocking {
    val source = DesktopLeafProvider(opts.modelDir).resolve { System.err.println("[MODEL] $it") }
    val engine = LeafEmbeddingEngine.load(source, mode = opts.mode) { System.err.println("[MODEL] $it") }
    System.err.println("[MODEL] loaded ${(source as LeafSource.Directory).path} in ${engine.loadMillis} ms, ${engine.dimensions} dims, kernels: ${kernelTierLabel()}")
    engine
}

private fun buildIndex(catalog: Catalog, engine: LeafEmbeddingEngine, log: Boolean = true): Pair<CatalogIndex, Long> {
    val started = TimeSource.Monotonic.markNow()
    val index = CatalogIndex.build(catalog.items, engine)
    val ms = started.elapsedNow().inWholeMilliseconds
    if (log) System.err.println("[INDEX] ${catalog.items.size} items embedded in $ms ms (${"%.1f".format(ms.toDouble() / catalog.items.size)} ms/item)")
    return index to ms
}

private fun rssMb(): Long? = runCatching {
    File("/proc/self/status").readLines().firstOrNull { it.startsWith("VmRSS:") }
        ?.split(Regex("\\s+"))?.get(1)?.toLong()?.div(1024)
}.getOrNull()

private fun percentile(sorted: List<Long>, p: Double): Long = sorted[((sorted.size - 1) * p).roundToLong().toInt()]

private fun bench(opts: Options) {
    val catalog = loadCatalog(opts)
    val engine = loadEngine(opts)
    val (index, indexMs) = buildIndex(catalog, engine)
    val calibration = loadCalibration(opts)

    // Warm-up, then timed repeats per query.
    BENCH_QUERIES.take(3).forEach { engine.embedQuery(it) }
    val embedTimes = ArrayList<Long>()
    val sweepTimes = ArrayList<Long>()
    repeat(opts.repeats) {
        for (q in BENCH_QUERIES) {
            val t0 = System.nanoTime()
            val v = engine.embedQuery(q)
            val t1 = System.nanoTime()
            index.rank(v, calibration)
            val t2 = System.nanoTime()
            embedTimes += (t1 - t0) / 1_000_000
            sweepTimes += (t2 - t1) / 1_000
        }
    }
    val e = embedTimes.sorted()
    val s = sweepTimes.sorted()
    println("mode=${opts.mode} kernels=${kernelTierLabel()} cores=${Runtime.getRuntime().availableProcessors()}")
    println("model load:        ${engine.loadMillis} ms")
    println("catalog embed:     $indexMs ms for ${catalog.items.size} items (${"%.1f".format(indexMs.toDouble() / catalog.items.size)} ms/item)")
    println("query embed:       p50 ${percentile(e, 0.5)} ms, p95 ${percentile(e, 0.95)} ms, max ${e.last()} ms (${e.size} runs, ${BENCH_QUERIES.size} queries x ${opts.repeats})")
    println("cosine sweep:      p50 ${percentile(s, 0.5)} us, p95 ${percentile(s, 0.95)} us")
    println("RSS:               ${rssMb()?.let { "$it MB" } ?: "n/a"}")
    println()
    for (q in listOf("healthy things to eat", "feeling sick", "rock band")) {
        val top = index.rank(engine.embedQuery(q), calibration).take(8)
        println("$q -> " + top.joinToString(" ") { "${it.item.emoji}${"%.2f".format(it.cosine)}" })
    }
}

private fun embedCatalog(opts: Options) {
    val catalog = loadCatalog(opts)
    val engine = loadEngine(opts)
    val (index, _) = buildIndex(catalog, engine)
    val bytes = index.toBytes()
    val out = File(opts.catalogDir, "catalog-embeddings.bin")
    out.writeBytes(bytes)
    val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val updated = catalog.copy(embeddingsSha256 = sha)
    File(opts.catalogDir, "catalog.json").writeText(Catalog.encode(updated) + "\n")
    println("wrote ${out.path} (${bytes.size} bytes, ${index.dimensions} dims), sha256 $sha recorded in catalog.json")
}

@Serializable
private data class LabelledQuery(val query: String, val matches: List<String>)

private fun loadCalibration(opts: Options): Calibration =
    File(opts.catalogDir, "calibration.json").takeIf { it.isFile }?.let { Calibration.parse(it.readText()) } ?: Calibration.DEFAULT

private fun calibrate(opts: Options) {
    val catalog = loadCatalog(opts)
    val labelsFile = File(opts.flags["labels"] ?: File(opts.catalogDir, "calibration-queries.json").path)
    val labels = Json { ignoreUnknownKeys = true }.decodeFromString<List<LabelledQuery>>(labelsFile.readText())
    val engine = loadEngine(opts)
    val (index, _) = buildIndex(catalog, engine)
    val samples = ArrayList<Pair<Double, Boolean>>()
    val unknown = labels.flatMap { it.matches }.filter { catalog.item(it) == null }
    require(unknown.isEmpty()) { "labels name unknown ids: $unknown" }
    for (label in labels) {
        val cos = index.cosines(engine.embedQuery(label.query))
        catalog.items.forEachIndexed { i, item -> samples += cos[i].toDouble() to (item.id in label.matches) }
    }
    val fitted = Calibration.fit(samples).copy(queries = labels.size)
    var tp = 0; var fp = 0; var fn = 0
    for ((x, y) in samples) {
        val predicted = fitted.isTop(x)
        if (predicted && y) tp++ else if (predicted && !y) fp++ else if (!predicted && y) fn++
    }
    val precision = if (tp + fp == 0) 0.0 else tp.toDouble() / (tp + fp)
    val recall = if (tp + fn == 0) 0.0 else tp.toDouble() / (tp + fn)
    File(opts.catalogDir, "calibration.json").writeText(Calibration.encode(fitted) + "\n")
    println("k=${"%.3f".format(fitted.k)} x0=${"%.4f".format(fitted.x0)} threshold=${fitted.threshold} (cosine ${"%.3f".format(fitted.cosineAtThreshold)}), top tier capped at ${fitted.maxTop}")
    val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
    println("${labels.size} queries, ${samples.count { it.second }} positives of ${samples.size}; accuracy ${"%.3f".format(fitted.accuracy)}, precision ${"%.2f".format(precision)}, recall ${"%.2f".format(recall)}, F1 ${"%.2f".format(f1)}")
}

@Serializable
private data class GoldenQuery(val query: String, val vector: List<Float>, val expectedTop: List<String>)

private fun golden(opts: Options) {
    val catalog = loadCatalog(opts)
    val engine = loadEngine(opts)
    val (index, _) = buildIndex(catalog, engine)
    val calibration = loadCalibration(opts)
    val goldens = listOf("healthy things to eat", "feeling sick", "rock band", "cute pets", "beach holiday").map { q ->
        val v = engine.embedQuery(q)
        GoldenQuery(q, v.toList(), index.rank(v, calibration).take(3).map { it.item.id })
    }
    val out = File(opts.catalogDir, "golden-queries.json")
    out.writeText(Json.encodeToString(goldens) + "\n")
    println("wrote ${out.path}: " + goldens.joinToString("; ") { g -> "${g.query} -> ${g.expectedTop.joinToString(",") { catalog.item(it)!!.emoji }}" })
}

/** System 2 on the CLI: embed, rank, then ask Laya about the top items. */
private fun verify(opts: Options) {
    val text = opts.positional.joinToString(" ").ifBlank { error("query text missing") }
    val catalog = loadCatalog(opts)
    val engine = loadEngine(opts)
    val calibration = loadCalibration(opts)
    val embeddings = File(opts.catalogDir, "catalog-embeddings.bin")
    val index = if (embeddings.isFile) CatalogIndex.fromBytes(catalog.items, embeddings.readBytes(), engine.dimensions) else buildIndex(catalog, engine).first
    val n = opts.flags["top"]?.toInt() ?: 6
    val ranked = index.rank(engine.embedQuery(text), calibration)
    val candidates = ranked.filter { it.tier == Tier.TOP }.take(n).ifEmpty { ranked.take(3) }
    println("'$text' -> asking Laya about " + candidates.joinToString(" ") { "${it.item.emoji}(${"%.2f".format(it.cosine)})" })
    verifyCommand(text, candidates.map { it.item }, opts.flags)
}

/** Debug: token ids of a text through tokenizer.json and vocab.txt, to catch normalisation differences. */
private fun tokens(opts: Options) {
    val text = opts.positional.joinToString(" ").ifBlank { error("text missing") }
    val dir = File(opts.modelDir)
    val fromJson = sk.ainet.frontend.tokenizer.TextFrontEnds.fromTokenizerJson(File(dir, "tokenizer.json").readText(), File(dir, "tokenizer_config.json").takeIf { it.isFile }?.readText()) as sk.ainet.frontend.text.BatchTokenizer
    val fromVocab = sk.ainet.frontend.tokenizer.TextFrontEnds.fromVocabTxt(File(dir, "vocab.txt").readText())
    val a = fromJson.encodeWithMetadata(text)
    val b = fromVocab.encodeWithMetadata(text)
    println("tokenizer.json: ${a.ids.toList()}")
    println("vocab.txt:      ${b.ids.toList()}")
    println("decoded(json):  ${fromJson.decode(a.ids)}")
}

private fun query(opts: Options) {
    val text = opts.positional.joinToString(" ").ifBlank { error("query text missing") }
    val catalog = loadCatalog(opts)
    val engine = loadEngine(opts)
    val calibration = loadCalibration(opts)
    val embeddings = File(opts.catalogDir, "catalog-embeddings.bin")
    val index = if (embeddings.isFile) CatalogIndex.fromBytes(catalog.items, embeddings.readBytes(), engine.dimensions) else buildIndex(catalog, engine).first
    val t0 = System.nanoTime()
    val v = engine.embedQuery(text)
    val ranked = index.rank(v, calibration)
    println("'$text' embedded in ${(System.nanoTime() - t0) / 1_000_000} ms; ${ranked.count { it.tier == Tier.TOP }} above threshold")
    for (r in ranked.take(10)) {
        println("  ${r.item.emoji}  cos ${"%.3f".format(r.cosine)}  conf ${"%.2f".format(r.confidence)}  ${if (r.tier == Tier.TOP) "TOP " else "    "} ${r.item.nameEn} / ${r.item.nameDe}")
    }
}
