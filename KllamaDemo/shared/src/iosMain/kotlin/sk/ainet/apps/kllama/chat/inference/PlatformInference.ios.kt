package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.llm.Tokenizer
import sk.ainet.apps.kllama.chat.domain.model.ChatSession

actual fun platformEncodePrompt(tokenizer: Tokenizer, session: ChatSession): IntArray? = null
