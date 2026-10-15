package sk.ainet.samples.tda.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.ainet.samples.tda.Datasets
import sk.ainet.samples.tda.DistanceMatrix
import sk.ainet.samples.tda.Filtration
import sk.ainet.samples.tda.PersistenceDiagram
import sk.ainet.samples.tda.PointCloud
import sk.ainet.samples.tda.VietorisRips
import kotlin.time.Duration
import kotlin.time.TimeSource

/** Everything the Results screen needs for one analysed point cloud. */
public class Analysis(
    public val name: String,
    public val points: PointCloud,
    public val distances: DistanceMatrix,
    public val filtration: Filtration,
    public val diagram: PersistenceDiagram,
    public val distanceTime: Duration,
    public val persistenceTime: Duration,
) {
    /** Upper end of the ε slider: past the enclosing radius nothing changes any more. */
    public val maxEpsilon: Float get() = distances.enclosingRadius
}

public enum class Screen { DATA, RESULTS, LOGIC }

/**
 * Holds the current analysis and the interactive scale ε. The heavy work (SKaiNET distance
 * matrix, filtration, ℤ/2 reduction) runs once per dataset; moving the slider only does prefix
 * lookups and bar counting, so it stays responsive even on wasm where there is one thread.
 */
public class TdaViewModel : ViewModel() {
    public var screen: Screen by mutableStateOf(Screen.DATA)
    public var analysis: Analysis? by mutableStateOf(null)
        private set
    public var isComputing: Boolean by mutableStateOf(false)
        private set
    public var error: String? by mutableStateOf(null)
        private set
    public var epsilon: Float by mutableStateOf(0f)
    /** Point budget: H1 reduction is cubic-ish in n, so large clouds are subsampled. */
    public var maxPoints: Int by mutableStateOf(150)
    public var csvText: String by mutableStateOf("")
    public var selectedPoint: Int? by mutableStateOf(null)

    public val edgesAtEpsilon: IntArray
        get() = analysis?.filtration?.edgesUpTo(epsilon) ?: IntArray(0)

    public fun betti(dim: Int): Int = analysis?.diagram?.betti(dim, epsilon) ?: 0

    public fun loadDataset(entry: Datasets.Entry) {
        analyse(entry.name, entry.make())
    }

    public fun loadCsv() {
        val cloud = PointCloud.parseCsv(csvText)
        if (cloud.size < 3) {
            error = "Need at least 3 points; parsed ${cloud.size}. Paste one `x,y` pair per line."
            return
        }
        analyse("Pasted CSV", cloud)
    }

    private fun analyse(name: String, raw: PointCloud) {
        viewModelScope.launch {
            isComputing = true
            error = null
            selectedPoint = null
            try {
                val result = withContext(Dispatchers.Default) {
                    val points = raw.subsample(maxPoints)
                    val clock = TimeSource.Monotonic
                    val t0 = clock.markNow()
                    val d = DistanceMatrix.compute(points)
                    val tDist = t0.elapsedNow()
                    val t1 = clock.markNow()
                    val filtration = Filtration.build(d)
                    val diagram = VietorisRips.persistence(d, filtration)
                    val tPers = t1.elapsedNow()
                    Analysis(name, points, d, filtration, diagram, tDist, tPers)
                }
                analysis = result
                // Start part-way into the filtration so edges and live bars are visible at once.
                epsilon = result.maxEpsilon * 0.35f
                screen = Screen.RESULTS
            } catch (t: Throwable) {
                error = t.message ?: t.toString()
            } finally {
                isComputing = false
            }
        }
    }
}
