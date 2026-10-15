package sk.ainet.samples.tda

import kotlin.random.Random

/** Axis-aligned bounding box of a point cloud. */
public data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    public val rangeX: Float get() = (maxX - minX).coerceAtLeast(1e-6f)
    public val rangeY: Float get() = (maxY - minY).coerceAtLeast(1e-6f)
}

/**
 * A 2-D point cloud stored as one flat, interleaved `[x0, y0, x1, y1, …]` array. This is the
 * row-major layout of an `[n, 2]` tensor, so it can be handed to SKaiNET without copying.
 *
 * Reading the bytes (a Compose resource, a pasted string, a file) is the caller's job; this
 * class only parses text, which keeps the library free of platform I/O.
 */
public class PointCloud(public val coords: FloatArray) {
    init {
        require(coords.size % 2 == 0) { "coords must hold x/y pairs, size was ${coords.size}" }
    }

    public val size: Int get() = coords.size / 2

    public fun x(i: Int): Float = coords[2 * i]
    public fun y(i: Int): Float = coords[2 * i + 1]

    public val bounds: Bounds by lazy {
        if (size == 0) return@lazy Bounds(0f, 1f, 0f, 1f)
        var minX = Float.POSITIVE_INFINITY; var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
        for (i in 0 until size) {
            val px = x(i); val py = y(i)
            if (px < minX) minX = px; if (px > maxX) maxX = px
            if (py < minY) minY = py; if (py > maxY) maxY = py
        }
        Bounds(minX, maxX, minY, maxY)
    }

    /**
     * The same cloud translated so its centroid is the origin. The FP32 `‖a‖² + ‖b‖² − 2a·b`
     * distance formula loses precision when coordinates are large compared to the distances,
     * so [DistanceMatrix.compute] centres the cloud first.
     */
    public fun centered(): PointCloud {
        if (size == 0) return this
        var sx = 0.0; var sy = 0.0
        for (i in 0 until size) { sx += x(i); sy += y(i) }
        val cx = (sx / size).toFloat(); val cy = (sy / size).toFloat()
        return PointCloud(FloatArray(coords.size) { k -> if (k % 2 == 0) coords[k] - cx else coords[k] - cy })
    }

    /** A random subset of at most [max] points (the whole cloud if it is already small enough). */
    public fun subsample(max: Int, seed: Int = 42): PointCloud {
        if (size <= max) return this
        val idx = (0 until size).shuffled(Random(seed)).take(max).sorted()
        val out = FloatArray(idx.size * 2)
        idx.forEachIndexed { k, i -> out[2 * k] = x(i); out[2 * k + 1] = y(i) }
        return PointCloud(out)
    }

    public companion object {
        public fun of(vararg xy: Float): PointCloud = PointCloud(xy)

        private val SEPARATORS = Regex("[,;\\t ]+")

        /**
         * Parses `x,y` rows. Accepts comma, semicolon, tab or space separators, skips blank lines,
         * a header line, and any line whose first two fields are not finite numbers.
         */
        public fun parseCsv(text: String): PointCloud {
            val out = ArrayList<Float>()
            for (raw in text.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val parts = line.split(SEPARATORS)
                if (parts.size < 2) continue
                val px = parts[0].toFloatOrNull() ?: continue
                val py = parts[1].toFloatOrNull() ?: continue
                if (!px.isFinite() || !py.isFinite()) continue
                out.add(px); out.add(py)
            }
            return PointCloud(out.toFloatArray())
        }
    }
}
