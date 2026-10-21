package sk.ainet.samples.layamoe.vm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import sk.ainet.samples.layamoe.engine.AnswerDto
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.MixtureClient
import sk.ainet.samples.layamoe.engine.Presets

@OptIn(ExperimentalCoroutinesApi::class)
class MoeViewModelTest {

    private class FakeClient : MixtureClient {
        val requests = ArrayList<AskRequest>()
        var fail = false
        override suspend fun load(onProgress: (String) -> Unit): String {
            onProgress("downloading…")
            if (fail) throw IllegalStateException("no network")
            return "fake tier"
        }
        override suspend fun ask(request: AskRequest): AskResult {
            requests += request
            return AskResult(
                expert = request.expert ?: "general", answers = mapOf("q" to AnswerDto("noul", noul = 0.5, probabilities = mapOf("no" to 0.5, "yes" to 0.5), confidence = 0.5)),
                encoderMillis = 1, headsMillis = 1, totalMillis = 2, inputTokens = 3, kernelTier = "fake tier",
            )
        }
    }

    private fun <T> withMain(block: () -> T): T {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            return block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loads_on_demand_then_answers_with_the_chosen_expert() = withMain {
        runTest {
            val client = FakeClient()
            val vm = MoeViewModel(client)
            assertIs<LoadState.Idle>(vm.status.value)
            vm.ask()
            assertTrue(client.requests.isEmpty(), "no request before the model is loaded")

            vm.load()
            advanceUntilIdle()
            assertEquals(LoadState.Ready("fake tier"), vm.status.value)

            vm.applyPreset(3)
            assertTrue(vm.stateText.value.contains("Invoices page broken"))
            vm.expert.value = "customer_ops"
            vm.compare.value = true
            vm.ask()
            advanceUntilIdle()
            val request = client.requests.single()
            assertEquals("customer_ops", request.expert)
            assertTrue(request.compare)
            assertEquals("customer_ops", assertNotNull(vm.result.value).expert)
            assertNull(vm.error.value)
        }
    }

    @Test
    fun invalid_questions_are_reported_without_a_request() = withMain {
        runTest {
            val client = FakeClient()
            val vm = MoeViewModel(client)
            vm.load()
            advanceUntilIdle()
            vm.questionsText.value = "{ nope"
            vm.ask()
            advanceUntilIdle()
            assertTrue(client.requests.isEmpty())
            assertNotNull(vm.error.value)
            vm.applyPreset(0)
            assertNull(vm.error.value)
            assertEquals(Presets.ALL[0].state, vm.stateText.value)
        }
    }

    @Test
    fun a_failed_load_can_be_retried() = withMain {
        runTest {
            val client = FakeClient().apply { fail = true }
            val vm = MoeViewModel(client)
            vm.load()
            advanceUntilIdle()
            assertIs<LoadState.Failed>(vm.status.value)
            client.fail = false
            vm.load()
            advanceUntilIdle()
            assertIs<LoadState.Ready>(vm.status.value)
        }
    }
}
