package sk.ainet.apps.kllama.chat.runtime

/**
 * Interface for KV cache implementations.
 *
 * KV caches store key and value vectors for each transformer layer across sequence positions,
 * enabling efficient autoregressive inference by avoiding recomputation of previous tokens.
 */
interface KvCache {
    val nLayers: Int
    val seqLen: Int
    val kvDim: Int

    fun store(
        layerIdx: Int,
        position: Int,
        keys: FloatArray,
        keysOffset: Int,
        values: FloatArray,
        valuesOffset: Int
    )

    fun getKey(layerIdx: Int, position: Int, headOffset: Int, elementIdx: Int): Float
    fun getValue(layerIdx: Int, position: Int, headOffset: Int, elementIdx: Int): Float
    fun reset()
}

/**
 * Simple heap-based KV cache implementation using FloatArrays.
 * Memory layout: [nLayers * seqLen * kvDim]
 */
class HeapKvCache(
    override val nLayers: Int,
    override val seqLen: Int,
    override val kvDim: Int
) : KvCache {

    private val keyCache = FloatArray(nLayers * seqLen * kvDim)
    private val valueCache = FloatArray(nLayers * seqLen * kvDim)

    override fun store(
        layerIdx: Int,
        position: Int,
        keys: FloatArray,
        keysOffset: Int,
        values: FloatArray,
        valuesOffset: Int
    ) {
        val base = (layerIdx * seqLen + position) * kvDim
        keys.copyInto(keyCache, base, keysOffset, keysOffset + kvDim)
        values.copyInto(valueCache, base, valuesOffset, valuesOffset + kvDim)
    }

    override fun getKey(layerIdx: Int, position: Int, headOffset: Int, elementIdx: Int): Float {
        val index = (layerIdx * seqLen + position) * kvDim + headOffset + elementIdx
        return keyCache[index]
    }

    override fun getValue(layerIdx: Int, position: Int, headOffset: Int, elementIdx: Int): Float {
        val index = (layerIdx * seqLen + position) * kvDim + headOffset + elementIdx
        return valueCache[index]
    }

    override fun reset() {
        keyCache.fill(0f)
        valueCache.fill(0f)
    }
}
