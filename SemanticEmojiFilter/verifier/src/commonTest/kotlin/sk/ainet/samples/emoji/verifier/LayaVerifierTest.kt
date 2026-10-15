package sk.ainet.samples.emoji.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import sk.ainet.decisions.AdapterCapabilities
import sk.ainet.decisions.AdapterOutcome
import sk.ainet.decisions.CalibrationStatus
import sk.ainet.decisions.Confidence
import sk.ainet.decisions.ConfidenceDefinition
import sk.ainet.decisions.DecisionAdapter
import sk.ainet.decisions.DecisionQuestion
import sk.ainet.decisions.DecisionState
import sk.ainet.decisions.InferencePolicy
import sk.ainet.decisions.LanguageSupport
import sk.ainet.decisions.Prediction
import sk.ainet.decisions.Primitive
import sk.ainet.decisions.QuestionTrace
import sk.ainet.samples.emoji.domain.CatalogItem

/** Reads the answer off the state text: pizza is a confident "no", salad a confident "yes", apple undecided. */
private class ScriptedAdapter : DecisionAdapter {
    val states = ArrayList<String>()
    var closed = false

    override val capabilities = AdapterCapabilities(
        modelId = EmojiMatchSchema.MODEL_ID, primitives = setOf(Primitive.CHOICE, Primitive.SCORE, Primitive.BOOLEAN_PROBABILITY),
        languages = LanguageSupport.Unrestricted, maxContextTokens = 512, maxOptionTokens = 192, maxOptions = 44,
        maxQuestionsPerRequest = 64, batchSupport = false, backend = "test", precision = "fp32",
        calibration = CalibrationStatus.UNVERIFIED, calibrationIdentity = "none", preprocessingVersion = "test/1",
    )

    override fun predict(state: DecisionState, questions: List<DecisionQuestion>, inference: InferencePolicy): List<AdapterOutcome> {
        val text = (state as DecisionState.Text).text
        states += text
        val p = when {
            "pizza" in text -> 0.12
            "salad" in text -> 0.91
            else -> 0.5
        }
        return questions.map { q ->
            AdapterOutcome.Candidate(q.id, Prediction.BooleanProbability(p, Confidence(maxOf(p, 1 - p), ConfidenceDefinition.TWO_WAY_MAX)), QuestionTrace(q.id, 40, 20, 0))
        }
    }

    override fun close() { closed = true }
}

private class ScriptedSource(val adapter: ScriptedAdapter = ScriptedAdapter()) : DecisionAdapterSource {
    var opened = 0
    override val modelId = EmojiMatchSchema.MODEL_ID
    override suspend fun open(onProgress: (String) -> Unit): sk.ainet.decisions.DecisionAdapter { opened++; return adapter }
}

class LayaVerifierTest {
    private val pizza = CatalogItem("1F355", "🍕", "pizza", "Pizza", listOf("cheese", "food", "pepperoni", "pizza", "slice"))
    private val salad = CatalogItem("1F957", "🥗", "green salad", "Salat", listOf("food", "green", "salad"))
    private val apple = CatalogItem("1F34E", "🍎", "red apple", "roter Apfel", listOf("apple", "fruit", "red"))

    @Test
    fun one_request_per_item_in_rank_order_with_typed_verdicts() = runTest {
        val source = ScriptedSource()
        val verifier = LayaVerifier(source, dispatcher = Dispatchers.Unconfined)
        val verdicts = verifier.verify("healthy things to eat", listOf(salad, pizza, apple)).toList()

        assertEquals(listOf("1F957", "1F355", "1F34E"), verdicts.map { it.itemId })
        assertEquals(true, verdicts[0].accepted)
        assertEquals(0.91, verdicts[0].probabilityTrue)
        assertEquals(false, verdicts[1].accepted)
        assertNull(verdicts[2].accepted, "0.5 lies inside the abstention band (${EmojiMatchSchema.REJECT_BELOW}, ${EmojiMatchSchema.ACCEPT_FROM})")
        assertEquals(0.5, verdicts[2].probabilityTrue)
        assertEquals(3, source.adapter.states.size, "one request per item")
        assertTrue(source.adapter.states[0].startsWith("User search: \"healthy things to eat\"\nEmoji: 🥗 green salad — food, green, salad"), source.adapter.states[0])
        assertEquals(1, source.opened, "the model loads once")
    }

    @Test
    fun cancellation_stops_after_the_current_item_and_repeats_hit_the_cache() = runTest {
        val source = ScriptedSource()
        val verifier = LayaVerifier(source, dispatcher = Dispatchers.Unconfined)
        val first = verifier.verify("snack", listOf(pizza, salad, apple)).first()
        assertEquals("1F355", first.itemId)
        assertEquals(1, source.adapter.states.size, "the collector stopped after the first verdict")

        verifier.verify("snack", listOf(pizza, salad)).take(2).toList()
        assertEquals(2, source.adapter.states.size, "pizza came from the cache, only salad was asked")
    }
}
