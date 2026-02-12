package sk.ainet.apps.kllama.chat.runtime

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import sk.ainet.context.ExecutionContext
import sk.ainet.io.gguf.llama.LlamaLayerWeights
import sk.ainet.io.gguf.llama.LlamaRuntimeWeights
import sk.ainet.lang.nn.layers.Embedding
import sk.ainet.lang.tensor.*
import sk.ainet.lang.tensor.data.FloatArrayTensorData
import sk.ainet.lang.tensor.operators.*
import sk.ainet.lang.types.FP32

/**
 * Minimal runtime that executes a LLaMA decoder using SKaiNET tensors and a CPU execution context.
 * Supports single-token autoregressive generation with a KV cache.
 *
 * Based on SKaiNET's skainet-kllama module.
 */
class LlamaRuntime(
    private val ctx: ExecutionContext,
    val weights: LlamaRuntimeWeights<FP32>,
    private val kvCache: KvCache? = null,
    private val ropeFreqBase: Float = 10000f,
    private val eps: Float = 1e-5f,
    private val random: Random = Random.Default
) {
    private companion object {
        const val BOS_TOKEN: Int = 1
    }

    private val dim = weights.metadata.embeddingLength
    private val seqLen = weights.metadata.contextLength
    private val nLayers = weights.metadata.blockCount
    private val nHeads = weights.metadata.headCount
    private val nKvHeads = weights.metadata.kvHeadCount
    private val headSize = dim / nHeads
    private val kvDim = nKvHeads * headSize
    private val vocabSize = weights.metadata.vocabSize
    private val ropeDim = weights.metadata.ropeDimensionCount ?: headSize
    private val nHeadsPerKv = nHeads / nKvHeads

    private val cache: KvCache = kvCache ?: HeapKvCache(nLayers, seqLen, kvDim)

    private var position: Int = 0

    private val embedding = Embedding(
        numEmbeddings = vocabSize,
        embeddingDim = dim,
        initWeight = weights.tokenEmbedding,
        name = "token_embd"
    )

    val currentPosition: Int
        get() = position

    fun reset() {
        cache.reset()
        position = 0
    }

    fun forward(tokenId: Int): Tensor<FP32, Float> {
        require(position < seqLen) { "Context length exceeded: pos=$position seqLen=$seqLen" }

        var x: Tensor<FP32, Float> = embedding.forward(intArrayOf(tokenId), ctx)

        weights.layers.forEachIndexed { layerIdx, layer ->
            x = runLayer(layerIdx, layer, x)
        }

        val norm = rmsNorm(x, weights.outputNorm)
        val logits = matmulNoBias(norm, weights.outputWeight)
        position++
        return logits
    }

    fun generate(prompt: IntArray, steps: Int, temperature: Float = 1.0f, onToken: (Int) -> Unit) {
        require(steps > 0) { "steps must be > 0" }

        var token = BOS_TOKEN
        var pos = 0
        while (pos < steps) {
            val logits = forward(token)
            val next = if (pos < prompt.size) prompt[pos] else sample(logits, temperature)
            onToken(next)
            token = next
            pos++
        }
    }

    private fun runLayer(layerIdx: Int, layer: LlamaLayerWeights<FP32>, input: Tensor<FP32, Float>): Tensor<FP32, Float> {
        var x = input

        val attnNorm = rmsNorm(x, layer.attnNorm)
        val q = matmulNoBias(attnNorm, layer.wq)
        val k = matmulNoBias(attnNorm, layer.wk)
        val v = matmulNoBias(attnNorm, layer.wv)

        val qHeads = q.reshape(Shape(nHeads, headSize))
        val kHeads = k.reshape(Shape(nKvHeads, headSize))
        val vHeads = v.reshape(Shape(nKvHeads, headSize))

        applyRopeGqa(qHeads, kHeads, position)
        cacheKvGqa(layerIdx, kHeads, vHeads, position)

        val attnOut = attentionGqa(layerIdx, qHeads, position)
        val attnTensor = ctx.fromFloatArray<FP32, Float>(Shape(1, dim), FP32::class, attnOut)
        x = x + attnTensor

        val ffnNorm = rmsNorm(x, layer.ffnNorm)
        val gate = matmulNoBias(ffnNorm, layer.ffnGate).silu()
        val up = matmulNoBias(ffnNorm, layer.ffnUp)
        val fused = gate * up
        val ffnOut = matmulNoBias(fused, layer.ffnDown)

        return x + ffnOut
    }

    private fun rmsNorm(x: Tensor<FP32, Float>, weight: Tensor<FP32, Float>): Tensor<FP32, Float> {
        val squared = x * x
        val mean = squared.mean(dim = squared.rank - 1)
        val invRms = (mean + eps).sqrt()
        val scaled = x / invRms
        val w = if (weight.rank == 1) weight.reshape(Shape(1, weight.shape[0])) else weight
        return scaled * w
    }

    private fun matmulNoBias(input: Tensor<FP32, Float>, weight: Tensor<FP32, Float>): Tensor<FP32, Float> {
        val inDim = input.shape[input.rank - 1]
        val w0 = weight.shape[0]
        return if (w0 == inDim) {
            input.matmul(weight)
        } else {
            input.matmul(weight.t())
        }
    }

    private fun applyRopeGqa(q: Tensor<FP32, Float>, k: Tensor<FP32, Float>, pos: Int) {
        val qBuf = q.expectFloatBuffer()
        val kBuf = k.expectFloatBuffer()
        val ropeReal = weights.ropeFreqReal?.expectFloatBuffer()
        val ropeImag = weights.ropeFreqImag?.expectFloatBuffer()
        val ropeStride = headSize / 2

        require(headSize % 2 == 0) { "RoPE requires even head size; got $headSize" }

        for (h in 0 until nHeads) {
            val headOffset = h * headSize
            for (pair in 0 until ropeDim / 2) {
                val i = pair * 2
                val fcr = ropeReal?.get(pos * ropeStride + pair) ?: ropeCosFallback(pair, pos)
                val fci = ropeImag?.get(pos * ropeStride + pair) ?: ropeSinFallback(pair, pos)

                val q0 = qBuf[headOffset + i]
                val q1 = qBuf[headOffset + i + 1]
                qBuf[headOffset + i] = q0 * fcr - q1 * fci
                qBuf[headOffset + i + 1] = q0 * fci + q1 * fcr
            }
        }

        for (h in 0 until nKvHeads) {
            val headOffset = h * headSize
            for (pair in 0 until ropeDim / 2) {
                val i = pair * 2
                val fcr = ropeReal?.get(pos * ropeStride + pair) ?: ropeCosFallback(pair, pos)
                val fci = ropeImag?.get(pos * ropeStride + pair) ?: ropeSinFallback(pair, pos)

                val k0 = kBuf[headOffset + i]
                val k1 = kBuf[headOffset + i + 1]
                kBuf[headOffset + i] = k0 * fcr - k1 * fci
                kBuf[headOffset + i + 1] = k0 * fci + k1 * fcr
            }
        }
    }

    private fun ropeCosFallback(pair: Int, pos: Int): Float {
        val freq = ropeFrequency(pair, pos)
        return cos(freq)
    }

    private fun ropeSinFallback(pair: Int, pos: Int): Float {
        val freq = ropeFrequency(pair, pos)
        return sin(freq)
    }

    private fun ropeFrequency(pair: Int, pos: Int): Float {
        val exponent = (2f * pair) / ropeDim
        return pos / ropeFreqBase.pow(exponent)
    }

    private fun cacheKvGqa(layerIdx: Int, k: Tensor<FP32, Float>, v: Tensor<FP32, Float>, pos: Int) {
        val kBuf = k.expectFloatBuffer()
        val vBuf = v.expectFloatBuffer()
        cache.store(layerIdx, pos, kBuf, 0, vBuf, 0)
    }

    private fun attentionGqa(layerIdx: Int, q: Tensor<FP32, Float>, pos: Int): FloatArray {
        val qBuf = q.expectFloatBuffer()
        val out = FloatArray(dim)
        val scale = 1f / sqrt(headSize.toDouble()).toFloat()

        for (h in 0 until nHeads) {
            val qHeadOffset = h * headSize
            val kvHeadIdx = h / nHeadsPerKv
            val kvHeadOffset = kvHeadIdx * headSize
            val scores = FloatArray(pos + 1)

            for (t in 0..pos) {
                var score = 0f
                for (i in 0 until headSize) {
                    score += qBuf[qHeadOffset + i] * cache.getKey(layerIdx, t, kvHeadOffset, i)
                }
                scores[t] = score * scale
            }

            softmaxInPlace(scores)

            for (t in 0..pos) {
                val weight = scores[t]
                for (i in 0 until headSize) {
                    out[qHeadOffset + i] += weight * cache.getValue(layerIdx, t, kvHeadOffset, i)
                }
            }
        }
        return out
    }

    private fun softmaxInPlace(values: FloatArray) {
        var maxVal = values.fold(Float.NEGATIVE_INFINITY) { acc, v -> max(acc, v) }
        if (maxVal.isInfinite()) maxVal = 0f
        var sum = 0f
        for (i in values.indices) {
            val e = exp((values[i] - maxVal).toDouble()).toFloat()
            values[i] = e
            sum += e
        }
        if (sum == 0f) return
        val inv = 1f / sum
        for (i in values.indices) {
            values[i] *= inv
        }
    }

    private fun sample(logits: Tensor<FP32, Float>, temperature: Float): Int {
        val buf = logits.expectFloatBuffer()
        if (temperature <= 1e-6f) {
            var best = 0
            var bestVal = buf[0]
            for (i in 1 until buf.size) {
                if (buf[i] > bestVal) {
                    bestVal = buf[i]
                    best = i
                }
            }
            return best
        }

        val scaled = FloatArray(buf.size)
        var maxLogit = Float.NEGATIVE_INFINITY
        for (i in buf.indices) {
            val v = buf[i] / temperature
            scaled[i] = v
            if (v > maxLogit) maxLogit = v
        }
        var sum = 0f
        for (i in scaled.indices) {
            val e = exp((scaled[i] - maxLogit).toDouble()).toFloat()
            scaled[i] = e
            sum += e
        }
        val r = random.nextFloat() * sum
        var acc = 0f
        for (i in scaled.indices) {
            acc += scaled[i]
            if (acc >= r) return i
        }
        return scaled.lastIndex
    }

    private fun Tensor<FP32, Float>.expectFloatBuffer(): FloatArray {
        val data = this.data
        if (data is FloatArrayTensorData<*>) return data.buffer
        val copied = FloatArray(this.volume) { idx ->
            val v = data[idx]
            (v as Number).toFloat()
        }
        return copied
    }
}
