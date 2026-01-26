package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.domain.model.ChatMessage
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.MessageRole

/**
 * Formats chat sessions into prompts for different model formats.
 */
object ChatFormatter {

    /**
     * Format a chat session into a Llama 2 style prompt.
     */
    fun formatLlama2(session: ChatSession): String {
        val sb = StringBuilder()

        // Add system prompt
        if (session.systemPrompt.isNotBlank()) {
            sb.append("[INST] <<SYS>>\n")
            sb.append(session.systemPrompt)
            sb.append("\n<</SYS>>\n\n")
        }

        // Add conversation history
        var isFirstUser = true
        session.messages.forEach { message ->
            when (message.role) {
                MessageRole.USER -> {
                    if (isFirstUser && session.systemPrompt.isNotBlank()) {
                        sb.append(message.content)
                        sb.append(" [/INST] ")
                    } else {
                        sb.append("[INST] ")
                        sb.append(message.content)
                        sb.append(" [/INST] ")
                    }
                    isFirstUser = false
                }
                MessageRole.ASSISTANT -> {
                    sb.append(message.content)
                    sb.append(" </s><s>")
                }
                MessageRole.SYSTEM -> {
                    // System messages are already handled above
                }
            }
        }

        return sb.toString()
    }

    /**
     * Format a chat session into a ChatML style prompt.
     */
    fun formatChatML(session: ChatSession): String {
        val sb = StringBuilder()

        // Add system prompt
        if (session.systemPrompt.isNotBlank()) {
            sb.append("<|im_start|>system\n")
            sb.append(session.systemPrompt)
            sb.append("<|im_end|>\n")
        }

        // Add conversation history
        session.messages.forEach { message ->
            val role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            sb.append("<|im_start|>$role\n")
            sb.append(message.content)
            sb.append("<|im_end|>\n")
        }

        // Add assistant prompt prefix
        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }

    /**
     * Format a chat session into a simple user/assistant format.
     */
    fun formatSimple(session: ChatSession): String {
        val sb = StringBuilder()

        // Add system prompt
        if (session.systemPrompt.isNotBlank()) {
            sb.append("System: ")
            sb.append(session.systemPrompt)
            sb.append("\n\n")
        }

        // Add conversation history
        session.messages.forEach { message ->
            val prefix = when (message.role) {
                MessageRole.USER -> "User"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }
            sb.append("$prefix: ")
            sb.append(message.content)
            sb.append("\n\n")
        }

        // Add assistant prompt prefix
        sb.append("Assistant: ")

        return sb.toString()
    }
}
