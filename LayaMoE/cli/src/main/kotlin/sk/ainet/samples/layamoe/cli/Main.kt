package sk.ainet.samples.layamoe.cli

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.MixtureService
import sk.ainet.samples.layamoe.engine.Presets
import sk.ainet.samples.layamoe.engine.WeightLane
import sk.ainet.samples.layamoe.engine.formatFixed
import sk.ainet.samples.layamoe.model.FileModelFiles
import sk.ainet.samples.layamoe.platform.desktopModelDir

private const val USAGE = """
layamoe — domain experts on top of the Laya decision model (SKaiNET)

  ask     --state <text | @file> --questions <json | @file> [--expert auto|general|safety|customer_ops] [--compare]
  preset  [--index N] [--all] [--expert …] [--compare]          the seven examples of the layaMOE page
  bench   [--index N] [--reps 3]                                 the preset N, repeated; mean per request
  eval    [--cases N|all] [--domain <name>] [--json <file>]      the 108 labelled cases: general vs routed vs oracle

  common: --q8 (packed Q8_0 lane; default FP32)   --model-dir <dir> (default ~/.skainet-examples/layamoe or LAYAMOE_MODEL_DIR)
"""

fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] in setOf("-h", "--help", "help")) { println(USAGE.trimIndent()); return }
    val command = args[0]
    val opts = parseOptions(args.drop(1))
    val lane = if ("q8" in opts.flags) WeightLane.Q8_0 else WeightLane.FP32
    val dir = opts.values["model-dir"]?.let(::File) ?: desktopModelDir()
    val service = MixtureService(FileModelFiles(dir), lane)
    val json = Json { prettyPrint = true; prettyPrintIndent = "  " }

    fun load() {
        val t0 = System.nanoTime()
        val tier = runBlocking { service.load { line -> System.err.println("[load] $line") } }
        System.err.println("[load] ready in ${(System.nanoTime() - t0) / 1_000_000} ms on $tier (${lane.label})")
    }

    fun print(result: AskResult) {
        println(json.encodeToString(AskResult.serializer(), result))
        System.err.println("[ask] ${result.expert} answered; encoder ${result.encoderMillis} ms, heads ${result.headsMillis} ms, ${result.inputTokens} tokens, ${result.totalMillis} ms in all")
    }

    when (command) {
        "ask" -> {
            val state = opts.values["state"]?.let(::textOrFile) ?: fail("ask needs --state")
            val questions = opts.values["questions"]?.let(::textOrFile) ?: fail("ask needs --questions")
            load()
            print(service.ask(AskRequest(state, questions, opts.values["expert"], "compare" in opts.flags)))
        }
        "preset" -> {
            val indices = if ("all" in opts.flags) Presets.ALL.indices.toList() else listOf(opts.values["index"]?.toInt() ?: 0)
            load()
            for (i in indices) {
                val p = Presets.ALL[i]
                println("## $i · ${p.title} (${p.note})")
                print(service.ask(AskRequest(p.state, p.questions, opts.values["expert"], "compare" in opts.flags)))
            }
        }
        "bench" -> {
            val p = Presets.ALL[opts.values["index"]?.toInt() ?: 0]
            val reps = opts.values["reps"]?.toInt() ?: 3
            load()
            val request = AskRequest(p.state, p.questions, null, compare = false)
            service.ask(request) // warm-up
            val times = (1..reps).map { service.ask(request).totalMillis }
            println("${p.title}: ${times.joinToString(" ")} ms → mean ${times.average().toLong()} ms per request (${lane.label}, ${times.size} reps after one warm-up)")
        }
        "eval" -> {
            load()
            Eval(service).run(
                perDomain = opts.values["cases"]?.let { if (it == "all") Int.MAX_VALUE else it.toInt() } ?: Int.MAX_VALUE,
                onlyDomain = opts.values["domain"],
                jsonOut = opts.values["json"]?.let(::File),
            )
        }
        else -> fail("unknown command '$command'\n$USAGE")
    }
}

private class Options(val values: Map<String, String>, val flags: Set<String>)

private fun parseOptions(args: List<String>): Options {
    val values = HashMap<String, String>()
    val flags = HashSet<String>()
    var i = 0
    while (i < args.size) {
        val a = args[i]
        if (!a.startsWith("--")) fail("unexpected argument '$a'")
        val key = a.removePrefix("--")
        val next = args.getOrNull(i + 1)
        if (key in setOf("compare", "all", "q8") || next == null || next.startsWith("--")) { flags += key; i++ } else { values[key] = next; i += 2 }
    }
    return Options(values, flags)
}

private fun textOrFile(value: String): String = if (value.startsWith("@")) File(value.substring(1)).readText() else value

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}

/** `value` as a percentage with one decimal. */
internal fun pct(correct: Int, total: Int): String = if (total == 0) "—" else formatFixed(100.0 * correct / total, 1) + " %"
