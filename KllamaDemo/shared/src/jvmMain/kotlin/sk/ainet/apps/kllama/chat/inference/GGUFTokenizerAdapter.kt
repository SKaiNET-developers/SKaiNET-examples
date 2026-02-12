package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.GGUFTokenizer

/**
 * Adapter bridging SKaiNET's [GGUFTokenizer] to the local [Tokenizer] interface.
 * Provides proper BPE/SentencePiece tokenization from GGUF model vocabulary.
 */
class GGUFTokenizerAdapter(private val ggufTokenizer: GGUFTokenizer) : Tokenizer {

    override val vocabSize: Int
        get() = ggufTokenizer.vocabSize

    override val bosToken: Int
        get() = ggufTokenizer.bosId

    override val eosToken: Int
        get() = ggufTokenizer.eosId

    override val padToken: Int
        get() = 0 // GGUF models typically don't have a pad token

    override fun encode(text: String): List<Int> = ggufTokenizer.encode(text).toList()

    override fun decode(tokenIds: List<Int>): String = ggufTokenizer.decode(tokenIds.toIntArray())

    override fun decodeToken(tokenId: Int): String = ggufTokenizer.decode(tokenId)
}
