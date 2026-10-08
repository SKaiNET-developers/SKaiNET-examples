package sk.ainet.samples.tda

import kotlin.math.abs
import kotlin.math.roundToLong

/** Fixed-point formatting without `String.format`, which does not exist on wasm/JS. */
public fun Float.format(decimals: Int = 2): String {
    if (isNaN()) return "NaN"
    if (isInfinite()) return if (this > 0) "∞" else "-∞"
    var scale = 1L; repeat(decimals) { scale *= 10 }
    val scaled = (abs(this).toDouble() * scale).roundToLong()
    val whole = scaled / scale
    val frac = (scaled % scale).toString().padStart(decimals, '0')
    val sign = if (this < 0 && scaled != 0L) "-" else ""
    return if (decimals == 0) "$sign$whole" else "$sign$whole.$frac"
}
