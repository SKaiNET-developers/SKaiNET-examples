package sk.ainet.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The one line every sample shows for the engine it runs on: "Powered by SKaiNET 0.61.0".
 * [version] is the `skainet` entry of the sample's version catalog, generated into the sample at
 * build time (`generateBuildInfo` in its build script), so the number is always the real one.
 */
@Composable
public fun PoweredBySKaiNET(version: String, modifier: Modifier = Modifier) {
    Text(
        text = "Powered by SKaiNET $version",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}
