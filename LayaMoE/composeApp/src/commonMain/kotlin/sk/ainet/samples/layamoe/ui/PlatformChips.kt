package sk.ainet.samples.layamoe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.layamoe.platform.SamplePlatform
import sk.ainet.samples.layamoe.platform.currentSamplePlatform

/** One chip per platform this sample runs on, the current one highlighted; informational only. */
@Composable
fun PlatformChips(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (platform in SamplePlatform.entries) {
            FilterChip(selected = platform == currentSamplePlatform, onClick = {}, label = { Text(platform.label) })
        }
    }
}
