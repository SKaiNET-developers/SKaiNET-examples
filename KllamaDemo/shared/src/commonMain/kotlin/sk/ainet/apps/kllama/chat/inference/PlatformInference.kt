package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.kllama.chat.domain.model.ChatSession

/**
 * Platform-specific: format a [ChatSession] with the ChatML template of SKaiNET-transformers'
 * agent module (a JVM dependency of this sample) and encode it to token ids.
 *
 * @return the prompt token ids, or null where the platform has no template
 */
expect fun platformEncodePrompt(tokenizer: Tokenizer, session: ChatSession): IntArray?
