package sk.ainet.apps.kllama.chat.inference

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator
import sk.ainet.apps.kllama.chat.domain.model.ChatMessage
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.MessageRole
import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.port.ModelLoadResult
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The chat chain end to end on the JVM: GGUF -> decoder loader -> llama network ->
 * `OptimizedLLMRuntime`, tokenizer from the GGUF header, ChatML prompt, greedy decode.
 *
 * Needs a llama-family GGUF; set `KLLAMA_CHAT_GGUF` to its path (a small instruct model such
 * as SmolLM2-135M-Instruct-Q8_0 does). Skips silently when the variable is unset.
 */
class ChatRuntimeSmokeJvmTest {

    @Test
    fun loads_a_llama_gguf_and_answers_greedily() = runBlocking {
        val path = System.getenv("KLLAMA_CHAT_GGUF")
        if (path.isNullOrBlank() || !Files.exists(Paths.get(path))) {
            println("[chat-smoke] KLLAMA_CHAT_GGUF not set or missing; skipping")
            return@runBlocking
        }

        val loader = CommonModelLoader()
        val loaded = when (val result = loader.loadFromPath(path, ModelFormat.GGUF)) {
            is ModelLoadResult.Success -> result.model
            is ModelLoadResult.Error -> fail("load failed: ${result.message}")
        }
        println("[chat-smoke] loaded ${loaded.metadata.name}: ${loaded.metadata.numLayers} layers, vocab ${loaded.metadata.vocabSize}")

        val engine = ServiceLocator.createInferenceEngine(loaded)
        assertTrue(engine.isReady, "runtime and tokenizer must be built on the JVM")

        val session = ChatSession(
            messages = listOf(ChatMessage(role = MessageRole.USER, content = "Name the capital of France in one word.")),
        )
        val tokens = engine.generate(session, InferenceConfig(maxNewTokens = 12, temperature = 0f)).toList()
        val text = tokens.joinToString("") { it.token }
        println("[chat-smoke] ${tokens.size} tokens: '$text'")

        assertTrue(tokens.isNotEmpty(), "greedy decode produced no token before EOS")
        assertTrue(text.isNotBlank(), "decoded text is blank")
        loader.unload()
    }
}
