package sk.ainet.samples.layamoe.worker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import sk.ainet.samples.layamoe.engine.AnswerDto
import sk.ainet.samples.layamoe.engine.AskRequest
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.RoutingDto

class WorkerProtocolTest {

    @Test
    fun messages_round_trip_with_their_ids() {
        val request = AskRequest(state = "a: b", questions = """{"q": {"type": "noul", "instructions": "x:y"}}""", expert = "safety", compare = true)
        val (id, parsed) = WorkerProtocol.parseAsk(WorkerProtocol.ask(7, request))!!
        assertEquals(7, id)
        assertEquals(request, parsed)

        val result = AskResult(
            expert = "safety",
            routing = RoutingDto(mapOf("agent_action" to 0.9, "other" to 0.1), "agent_action", 0.9, 0.7, 0.3, "safety", "safety", false),
            answers = mapOf("q" to AnswerDto("noul", noul = 0.6, probabilities = mapOf("no" to 0.4, "yes" to 0.6), confidence = 0.6)),
            generalAnswers = null,
            optionLabels = mapOf("q" to listOf("no", "yes")),
            questionTypes = mapOf("q" to "noul"),
            encoderMillis = 1200, headsMillis = 80, totalMillis = 1300, inputTokens = 140, kernelTier = "Wasm SIMD128 (threads)",
        )
        val (rid, back) = WorkerProtocol.parseResult(WorkerProtocol.result(3, result))!!
        assertEquals(3, rid)
        assertEquals(result, back)

        assertEquals("Wasm SIMD128 (threads)", WorkerProtocol.parseReady(WorkerProtocol.ready("Wasm SIMD128 (threads)")))
        assertEquals("loading" to "downloading base/model.safetensors… 12 / 842 MB", WorkerProtocol.parseState(WorkerProtocol.loading("downloading base/model.safetensors… 12 / 842 MB")))
        assertEquals("error" to "boom", WorkerProtocol.parseState(WorkerProtocol.failed("boom")))
        assertEquals(null to "load failed: x", WorkerProtocol.parseError(WorkerProtocol.error(null, "load failed: x")))
        assertEquals(5 to "bad json", WorkerProtocol.parseError(WorkerProtocol.error(5, "bad json")))
    }

    @Test
    fun foreign_messages_parse_to_null() {
        assertNull(WorkerProtocol.parseAsk("RESULT:1:{}"))
        assertNull(WorkerProtocol.parseResult("ASK:1:{}"))
        assertNull(WorkerProtocol.parseReady("STATE:loading:x"))
        assertNull(WorkerProtocol.parseState("READY:x"))
        assertNull(WorkerProtocol.parseError("READY:x"))
        assertNull(WorkerProtocol.parseAsk("ASK:x:{}"))
    }
}
