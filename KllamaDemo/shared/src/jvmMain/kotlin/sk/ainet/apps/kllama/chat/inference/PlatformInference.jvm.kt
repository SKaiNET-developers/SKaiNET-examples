package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.ChatMLTemplate
import sk.ainet.apps.kllama.chat.ChatMessage
import sk.ainet.apps.kllama.chat.ChatRole
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.MessageRole
import sk.ainet.apps.llm.Tokenizer

actual fun platformEncodePrompt(tokenizer: Tokenizer, session: ChatSession): IntArray? {
    val prompt = ChatMLTemplate().apply(buildSKaiNETMessages(session), emptyList(), true)
    return tokenizer.encode(prompt)
}

private fun buildSKaiNETMessages(session: ChatSession): List<ChatMessage> {
    val messages = mutableListOf<ChatMessage>()

    if (session.systemPrompt.isNotBlank()) {
        messages.add(ChatMessage(role = ChatRole.SYSTEM, content = session.systemPrompt))
    }

    for (msg in session.messages) {
        if (msg.content.isBlank() && msg.isStreaming) continue
        val role = when (msg.role) {
            MessageRole.USER -> ChatRole.USER
            MessageRole.ASSISTANT -> ChatRole.ASSISTANT
            MessageRole.SYSTEM -> ChatRole.SYSTEM
        }
        messages.add(ChatMessage(role = role, content = msg.content))
    }

    return messages
}
