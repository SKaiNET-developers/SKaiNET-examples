package sk.ainet.samples.tda.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.tda.Datasets
import sk.ainet.ui.theme.SKaiNETTheme
import sk.ainet.ui.theme.ThemeController

/**
 * TDA Lab: pick or paste a 2-D point cloud, grow the Vietoris-Rips radius ε with a slider and
 * watch edges, Betti numbers and the persistence barcode change. The distance matrix is a
 * SKaiNET tensor expression; the homology is plain Kotlin in the `:tda` library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun App() {
    val themeController = remember { ThemeController() }

    SKaiNETTheme(themeController = themeController) {
        val vm = remember { TdaViewModel() }
        // Open on a real result rather than an empty screen: the first bundled dataset.
        LaunchedEffect(Unit) { if (vm.analysis == null) vm.loadDataset(Datasets.all.first()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TDA Lab") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    actions = {
                        IconButton(onClick = { themeController.toggleTheme() }) {
                            Text(if (themeController.isDarkTheme) "Light" else "Dark")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = vm.screen == Screen.DATA,
                        onClick = { vm.screen = Screen.DATA },
                        icon = { DotsIcon() },
                        label = { Text("Data") },
                    )
                    NavigationBarItem(
                        selected = vm.screen == Screen.RESULTS,
                        onClick = { vm.screen = Screen.RESULTS },
                        enabled = vm.analysis != null,
                        icon = { RingIcon() },
                        label = { Text("Results") },
                    )
                    NavigationBarItem(
                        selected = vm.screen == Screen.LOGIC,
                        onClick = { vm.screen = Screen.LOGIC },
                        icon = { Text("∂") },
                        label = { Text("How it works") },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                when (vm.screen) {
                    Screen.DATA -> DataScreen(vm)
                    Screen.RESULTS -> ResultsScreen(vm)
                    Screen.LOGIC -> LogicScreen()
                }
            }
        }
    }
}

/** Three points: the point-cloud tab. Drawn, not a glyph, so it renders identically on wasm. */
@Composable
private fun DotsIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val r = size.minDimension / 7f
        drawCircle(color, r, Offset(size.width * 0.25f, size.height * 0.7f))
        drawCircle(color, r, Offset(size.width * 0.55f, size.height * 0.3f))
        drawCircle(color, r, Offset(size.width * 0.8f, size.height * 0.65f))
    }
}

/** A loop: the results tab. */
@Composable
private fun RingIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        drawCircle(color, size.minDimension / 2.6f, style = Stroke(width = size.minDimension / 7f))
    }
}
