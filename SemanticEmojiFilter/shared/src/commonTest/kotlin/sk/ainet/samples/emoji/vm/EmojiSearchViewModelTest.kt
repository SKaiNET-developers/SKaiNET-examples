package sk.ainet.samples.emoji.vm

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import sk.ainet.samples.emoji.domain.Calibration
import sk.ainet.samples.emoji.domain.CatalogItem
import sk.ainet.samples.emoji.engine.CatalogIndex
import sk.ainet.samples.emoji.engine.EmbeddingEngine
import sk.ainet.samples.emoji.verify.Verdict
import sk.ainet.samples.emoji.verify.Verifier
import sk.ainet.samples.emoji.verify.VerifierState

@OptIn(ExperimentalCoroutinesApi::class)
class EmojiSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setMain() = Dispatchers.setMain(dispatcher)
    @AfterTest fun resetMainDispatcher() = Dispatchers.resetMain()

    private val items = listOf(
        CatalogItem("A", "🍕", "pizza", "Pizza"),
        CatalogItem("B", "🥗", "salad", "Salat"),
        CatalogItem("C", "🎸", "guitar", "Gitarre"),
    )

    /** Query "food" points at A and B, "music" at C. */
    private class FakeEngine : EmbeddingEngine {
        val embedded = ArrayList<String>()
        override val dimensions = 3
        override fun embedQuery(text: String): FloatArray = embed(text) // symmetric fake, no retrieval prompt
        override fun embed(text: String): FloatArray {
            embedded += text
            return when {
                "music" in text -> floatArrayOf(0f, 0f, 1f)
                else -> floatArrayOf(0.8f, 0.6f, 0f)
            }
        }
    }

    private class RecordingVerifier : Verifier {
        val calls = ArrayList<Pair<String, List<String>>>()
        override val state: StateFlow<VerifierState> = MutableStateFlow(VerifierState.Idle)
        override fun verify(query: String, items: List<CatalogItem>): Flow<Verdict> = flow {
            calls += query to items.map { it.id }
            for (item in items) emit(Verdict(item.id, 0.9, true, 1))
        }
    }

    private val vectors = floatArrayOf(1f, 0f, 0f, 0.8f, 0.6f, 0f, 0f, 0f, 1f)

    private fun viewModel(engine: FakeEngine, verifier: RecordingVerifier) = EmojiSearchViewModel(
        loadEngine = { engine },
        loadIndex = { _, _ -> CatalogIndex(items, vectors, 3) },
        calibration = Calibration(k = 20.0, x0 = 0.7),
        verifier = verifier,
        embedDispatcher = dispatcher,
        debounceMs = 120,
        stableMs = 500,
        verifierTopK = 6,
    )

    @Test
    fun fast_keystrokes_collapse_into_one_embedding_and_the_verifier_waits_for_a_stable_query() = runTest(dispatcher) {
        val engine = FakeEngine()
        val verifier = RecordingVerifier()
        val vm = viewModel(engine, verifier)
        runCurrent()
        assertEquals(LoadState.Ready, vm.status.value)
        assertEquals(3, vm.ranked.value.size, "unranked catalog shown before typing")

        for (q in listOf("f", "fo", "foo", "food")) {
            vm.query.value = q
            advanceTimeBy(30)
        }
        advanceTimeBy(130)
        runCurrent()
        assertEquals(listOf("food"), engine.embedded, "one embedding for five fast keystrokes")
        assertEquals(listOf("B", "A", "C"), vm.ranked.value.map { it.item.id }, "salad (B) is the exact match, pizza (A) close behind")
        assertTrue(verifier.calls.isEmpty(), "verifier not called before the query is stable")

        advanceTimeBy(600)
        runCurrent()
        assertEquals(listOf("food" to listOf("B", "A")), verifier.calls, "only the TOP tier goes to the verifier, in rank order")
        assertEquals(setOf("A", "B"), vm.verdicts.value.keys)

        vm.query.value = "music"
        advanceTimeBy(130)
        runCurrent()
        assertTrue(vm.verdicts.value.isEmpty(), "verdicts cleared when the query changes")
        assertEquals("C", vm.ranked.value.first().item.id)
    }
}
