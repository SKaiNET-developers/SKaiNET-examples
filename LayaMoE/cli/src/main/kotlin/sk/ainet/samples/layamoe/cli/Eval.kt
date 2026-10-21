package sk.ainet.samples.layamoe.cli

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import sk.ainet.samples.layamoe.engine.AnswerDto
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.LayaMoeRouter
import sk.ainet.samples.layamoe.engine.MixtureService

/**
 * The 108 hand-labelled cases of layaMOE (the JSON files under `src/main/resources/eval`, Apache-2.0, see THIRD_PARTY_LICENSES/NOTICE):
 * every case through the general head, the routed head and the oracle head (the expert of the
 * case's domain), scored as the published evaluation does — a choice is right when the key
 * matches, a score when the most probable level is the labelled one, a yes/no when `P(true) ≥ 0.5`
 * agrees with the label.
 */
class Eval(private val service: MixtureService) {
    private val json = Json { ignoreUnknownKeys = true }

    private class Domain(val name: String, val questions: String, val cases: List<JsonObject>)

    private fun resource(path: String): String =
        Eval::class.java.getResourceAsStream("/eval/$path")?.use { it.readBytes().decodeToString() } ?: error("missing resource eval/$path")

    private fun domains(): List<Domain> {
        val index = json.parseToJsonElement(resource("index.json")).jsonObject
        return index["domains"]!!.jsonArray.map { d ->
            val file = json.parseToJsonElement(resource(d.jsonObject["file"]!!.jsonPrimitive.content)).jsonObject
            Domain(file["domain"]!!.jsonPrimitive.content, file["questions"]!!.toString(), file["cases"]!!.jsonArray.map { it.jsonObject })
        }
    }

    private class Tally(var answers: Int = 0, var general: Int = 0, var routed: Int = 0, var oracle: Int = 0, var cases: Int = 0, var millis: Long = 0, val routedTo: MutableMap<String, Int> = LinkedHashMap())

    fun run(perDomain: Int, onlyDomain: String?, jsonOut: File?) {
        val tallies = LinkedHashMap<String, Tally>()
        val total = Tally()
        val records = ArrayList<JsonElement>()
        for (domain in domains()) {
            if (onlyDomain != null && domain.name != onlyDomain) continue
            val tally = tallies.getOrPut(domain.name) { Tally() }
            val oracleExpert = LayaMoeRouter.oracleExpertFor(domain.name)
            for (case in domain.cases.take(perDomain)) {
                val id = case["id"]!!.jsonPrimitive.content
                val state = case["state"]!!.let { if (it is JsonPrimitive && it.isString) it.content else it.toString() }
                val expected = case["expected"]!!.jsonObject
                val routed = service.ask(AskRequest(state, domain.questions, expert = null, compare = true))
                val general = routed.generalAnswers ?: routed.answers
                val oracle = if (routed.expert == oracleExpert) routed.answers else service.ask(AskRequest(state, domain.questions, expert = oracleExpert, compare = false)).answers
                for ((qid, label) in expected) {
                    val g = isCorrect(general.getValue(qid), label)
                    val r = isCorrect(routed.answers.getValue(qid), label)
                    val o = isCorrect(oracle.getValue(qid), label)
                    for (t in listOf(tally, total)) { t.answers++; if (g) t.general++; if (r) t.routed++; if (o) t.oracle++ }
                    records += buildJsonObject {
                        put("domain", domain.name); put("id", id); put("question", qid); put("expected", label)
                        put("routed_to", routed.expert); put("router_top", routed.routing?.top ?: "")
                        put("general", g); put("routed", r); put("oracle", o)
                    }
                }
                for (t in listOf(tally, total)) { t.cases++; t.millis += routed.totalMillis; t.routedTo[routed.expert] = (t.routedTo[routed.expert] ?: 0) + 1 }
                System.err.println("[eval] ${domain.name}/$id → ${routed.expert} (router ${routed.routing?.top}) in ${routed.totalMillis} ms")
            }
        }
        println()
        println("| domain | cases | answers | general | routed | oracle | routed to | ms/case |")
        println("|---|---|---|---|---|---|---|---|")
        for ((name, t) in tallies) println(row(name, t))
        println(row("**all**", total))
        val withExpert = tallies.filterKeys { LayaMoeRouter.oracleExpertFor(it) != LayaMoeRouter.GENERAL }.values
        val without = tallies.filterKeys { LayaMoeRouter.oracleExpertFor(it) == LayaMoeRouter.GENERAL }.values
        fun sum(ts: Collection<Tally>, f: (Tally) -> Int) = ts.sumOf(f)
        println()
        println("with an expert: general ${pct(sum(withExpert) { it.general }, sum(withExpert) { it.answers })}, routed ${pct(sum(withExpert) { it.routed }, sum(withExpert) { it.answers })}, oracle ${pct(sum(withExpert) { it.oracle }, sum(withExpert) { it.answers })}")
        println("without:        general ${pct(sum(without) { it.general }, sum(without) { it.answers })}, routed ${pct(sum(without) { it.routed }, sum(without) { it.answers })}")
        jsonOut?.writeText(Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), JsonArray(records)))
    }

    private fun row(name: String, t: Tally): String =
        "| $name | ${t.cases} | ${t.answers} | ${pct(t.general, t.answers)} | ${pct(t.routed, t.answers)} | ${pct(t.oracle, t.answers)} | ${t.routedTo.entries.joinToString(", ") { "${it.key} ${it.value}" }} | ${if (t.cases == 0) "—" else t.millis / t.cases} |"

    private fun isCorrect(answer: AnswerDto, label: JsonElement): Boolean = when (answer.type) {
        "choice" -> answer.choice == label.jsonPrimitive.content
        "score" -> answer.probabilities.entries.maxBy { it.value }.key == label.jsonPrimitive.content
        else -> ((answer.noul ?: 0.0) >= 0.5) == (label.jsonPrimitive.booleanOrNull ?: false)
    }
}
