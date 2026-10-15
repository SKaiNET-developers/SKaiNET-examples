package sk.ainet.samples.tda.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.ainet.samples.tda.Datasets
import sk.ainet.samples.tda.format
import kotlin.time.Duration
import sk.ainet.samples.tda.app.plots.BarcodePlot
import sk.ainet.samples.tda.app.plots.PersistenceDiagramPlot
import sk.ainet.samples.tda.app.plots.ScatterPlot
import sk.ainet.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DataScreen(vm: TdaViewModel) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("Choose a point cloud", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Synthetic clouds with known topology, generated in the app. Expected Betti numbers in brackets.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Datasets.all.forEach { entry ->
                FilterChip(
                    selected = vm.analysis?.name == entry.name,
                    onClick = { vm.loadDataset(entry) },
                    enabled = !vm.isComputing,
                    label = { Text("${entry.name}  (${entry.expected})") },
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Point budget: ${vm.maxPoints}", fontWeight = FontWeight.Medium)
        Text(
            "Larger clouds are randomly subsampled to this many points. H1 needs every triangle, " +
                "which grows fast with the point count; 150 is instant, 400 takes a few seconds in the browser.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = vm.maxPoints.toFloat(),
            onValueChange = { vm.maxPoints = it.toInt() },
            valueRange = 30f..400f,
            steps = 36,
            enabled = !vm.isComputing,
        )

        Spacer(Modifier.height(20.dp))
        Text("...or paste your own CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.csvText,
            onValueChange = { vm.csvText = it },
            label = { Text("x,y per line (header optional)") },
            placeholder = { Text("x,y\n0.0,1.0\n0.7,0.7\n...") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.loadCsv() }, enabled = !vm.isComputing && vm.csvText.isNotBlank()) {
                Text("Analyze CSV")
            }
            Spacer(Modifier.width(16.dp))
            if (vm.isComputing) {
                LoadingIndicator(size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text("Computing...")
            }
        }
        vm.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun ResultsScreen(vm: TdaViewModel) {
    val a = vm.analysis
    if (a == null) {
        Text("Pick a dataset on the Data tab first.")
        return
    }
    val edges = vm.edgesAtEpsilon
    val beta0 = vm.betti(0)
    val beta1 = vm.betti(1)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(a.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${a.points.size} points · ${a.filtration.edgeCount} edges · distance matrix (SKaiNET) " +
                "${a.distanceTime.millis()} · persistence ${a.persistenceTime.millis()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        Text("Filtration radius ε = ${vm.epsilon.format(3)}   (${edges.size / 2} edges)", fontWeight = FontWeight.Medium)
        Slider(
            value = vm.epsilon,
            onValueChange = { vm.epsilon = it },
            valueRange = 0f..a.maxEpsilon,
        )

        PlotCard("Vietoris-Rips 1-skeleton at ε") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ScatterPlot(
                    points = a.points,
                    edges = edges,
                    selected = vm.selectedPoint,
                    onPointSelected = { vm.selectedPoint = it },
                    // Square canvas with equal axis scaling, so a circle looks like a circle. No
                    // fillMaxWidth here: a fixed min width would override the aspect ratio.
                    modifier = Modifier.heightIn(max = 440.dp).aspectRatio(1f),
                )
            }
        }
        vm.selectedPoint?.let { i ->
            Text(
                "Selected point $i: (${a.points.x(i).format(3)}, ${a.points.y(i).format(3)})",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Spacer(Modifier.height(16.dp))
        BettiCard(beta0, beta1, a.diagram.bars(1).count { it.persistence > 0f })

        Spacer(Modifier.height(16.dp))
        PlotCard("Persistence barcode (H0 red, H1 blue; ε marker dashed)") {
            BarcodePlot(a.diagram, vm.epsilon, a.maxEpsilon, Modifier.fillMaxWidth().height(260.dp))
        }

        Spacer(Modifier.height(16.dp))
        PlotCard("Persistence diagram (birth vs. death)") {
            PersistenceDiagramPlot(a.diagram, vm.epsilon, a.maxEpsilon, Modifier.fillMaxWidth().height(280.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlotCard(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(8.dp)) { content() } }
}

@Composable
private fun BettiCard(beta0: Int, beta1: Int, loopsFound: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Homology at ε", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            HomologyRow(0, beta0, "connected components")
            HomologyRow(1, beta1, "independent loops")
            Spacer(Modifier.height(8.dp))
            Text(
                "β0 = $beta0, β1 = $beta1  ·  $loopsFound loop${if (loopsFound == 1) "" else "s"} with non-zero persistence in the whole filtration",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
    }
}

/**
 * `H1 = Z + Z` style notation for a free abelian group of the given rank. Plain glyphs on
 * purpose: the font bundled with Compose for Web has no ℤ, ⊕, ≅ or subscript digits.
 */
@Composable
private fun HomologyRow(dim: Int, rank: Int, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) { append("H") }
            withStyle(SpanStyle(fontSize = 13.sp, baselineShift = BaselineShift.Subscript)) { append(dim.toString()) }
            append("  =  ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)) {
                when {
                    rank == 0 -> append("0")
                    rank <= 3 -> append(List(rank) { "Z" }.joinToString(" + "))
                    else -> {
                        append("Z")
                        withStyle(SpanStyle(fontSize = 13.sp, baselineShift = BaselineShift.Superscript)) { append(rank.toString()) }
                    }
                }
            }
        }
        Text(text)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun LogicScreen() {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text("How it works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Step(
            "1. Distance matrix: a SKaiNET tensor expression",
            "With X the [n, 2] coordinate tensor and s = rowSum(X * X):  D² = s[:,None] + s[None,:] - 2 · X · X^T.  " +
                "One matmul, two broadcasts, clamp and sqrt; the n×n work runs in the backend's vectorised kernels " +
                "(Vector API on the JVM, NEON on Android, SIMD tier on wasm). The cloud is centred first because " +
                "the formula cancels catastrophically in FP32 far from the origin.",
        )
        Step(
            "2. Vietoris-Rips filtration",
            "Every pair of points is an edge with weight = its distance; a triangle appears when all three of its " +
                "edges are present. Sorting the edges once gives the whole filtration: the 1-skeleton at radius ε " +
                "is just a prefix of that list, which is why the slider is instant.",
        )
        Step(
            "3. H0 by union-find",
            "Walk the sorted edges; an edge that joins two components kills the younger one at its length. " +
                "The surviving component is the infinite bar. Edges that close a cycle are 'positive': each one " +
                "gives birth to an H1 class.",
        )
        Step(
            "4. H1 by boundary-matrix reduction over Z/2",
            "Rows are edges, each triangle is a column with three 1s. Columns are reduced in filtration order by " +
                "XOR-ing in the stored column that owns the same lowest row (the pivot). A reduced column with pivot e " +
                "pairs the loop born at edge e with the triangle that fills it; positive edges that never become a " +
                "pivot are loops that survive to the end. This is the standard persistence algorithm, as in Ripser, " +
                "without its emergent-pair and cohomology tricks, and it is plain Kotlin on IntArrays: sparse Z/2 " +
                "column work is not a tensor operation.",
        )
        Step(
            "5. Betti numbers and barcode",
            "β_k(ε) counts the bars of dimension k with birth <= ε < death. Long bars are structure, short bars are " +
                "noise. The filtration stops at the enclosing radius min_i max_j d(i,j): past it one ball covers " +
                "everything and the complex is a cone, so nothing can change.",
        )
        Step(
            "What SKaiNET is, and is not, used for",
            "SKaiNET carries the dense numeric part (the distance matrix) on every platform. The combinatorial " +
                "part needs sorting, union-find and sparse Z/2 columns, none of which are tensor operations, " +
                "so it lives next to the tensors as ordinary Kotlin. Same split as the GloVe example.",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Step(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun Duration.millis(): String = if (inWholeMilliseconds == 0L) "<1 ms" else "$inWholeMilliseconds ms"
