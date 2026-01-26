package sk.ainet.apps.kllama.chat.inference

/**
 * Interface for text tokenization and decoding.
 */
interface Tokenizer {
    /**
     * Encode text to token IDs.
     */
    fun encode(text: String): List<Int>

    /**
     * Decode token IDs to text.
     */
    fun decode(tokenIds: List<Int>): String

    /**
     * Decode a single token ID to text.
     */
    fun decodeToken(tokenId: Int): String

    /**
     * Get the vocabulary size.
     */
    val vocabSize: Int

    /**
     * Get the beginning-of-sequence token ID.
     */
    val bosToken: Int

    /**
     * Get the end-of-sequence token ID.
     */
    val eosToken: Int

    /**
     * Get the padding token ID.
     */
    val padToken: Int
}

/**
 * Simple byte-level tokenizer for demonstration purposes.
 * In production, use the model's actual tokenizer (BPE, SentencePiece, etc.)
 */
class SimpleByteTokenizer : Tokenizer {
    override val vocabSize: Int = 256
    override val bosToken: Int = 1
    override val eosToken: Int = 2
    override val padToken: Int = 0

    override fun encode(text: String): List<Int> {
        return listOf(bosToken) + text.encodeToByteArray().map { it.toInt() and 0xFF }
    }

    override fun decode(tokenIds: List<Int>): String {
        val filtered = tokenIds.filter { it != bosToken && it != eosToken && it != padToken }
        return filtered.map { it.toByte() }.toByteArray().decodeToString()
    }

    override fun decodeToken(tokenId: Int): String {
        return when (tokenId) {
            bosToken, eosToken, padToken -> ""
            else -> byteArrayOf(tokenId.toByte()).decodeToString()
        }
    }
}
