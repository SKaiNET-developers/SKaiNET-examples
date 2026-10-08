package sk.ainet.samples.tda

import kotlin.time.TimeSource

/** `./gradlew :tda:runDemo` — barcodes of the bundled datasets, on the JVM. */
fun main() {
    for (entry in Datasets.all) {
        val points = entry.make()
        val clock = TimeSource.Monotonic
        val t0 = clock.markNow()
        val d = DistanceMatrix.compute(points)
        val tDist = t0.elapsedNow()
        val t1 = clock.markNow()
        val diagram = VietorisRips.persistence(d)
        val tPers = t1.elapsedNow()
        println("== ${entry.name}: ${points.size} points (expected ${entry.expected})")
        println("   distance matrix (SKaiNET) $tDist, persistence $tPers, enclosing radius ${d.enclosingRadius.format(3)}")
        for (dim in 0..1) {
            val bars = diagram.bars(dim).take(6)
            println("   H$dim: " + bars.joinToString { "[${it.birth.format(3)}, ${it.death.format(3)})" })
        }
    }
}
