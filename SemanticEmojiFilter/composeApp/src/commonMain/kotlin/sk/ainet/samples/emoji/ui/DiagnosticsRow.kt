package sk.ainet.samples.emoji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.emoji.verify.VerifierState
import sk.ainet.samples.emoji.vm.SearchStats

/** Timings and the verifier state, so the demo shows its numbers instead of claiming them. */
@Composable
fun DiagnosticsRow(stats: SearchStats, verifierState: VerifierState, kernel: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(onClick = {}, label = { Text(kernel) })
        AssistChip(onClick = {}, label = { Text("load ${stats.modelLoadMs} ms · index ${stats.indexBuildMs} ms") })
        if (stats.queries > 0) {
            AssistChip(onClick = {}, label = { Text("query ${stats.lastQueryMs} ms · p50 ${stats.p50QueryMs} ms") })
        }
        VerifierChip(verifierState)
    }
}

@Composable
private fun VerifierChip(state: VerifierState) {
    val label = when (state) {
        is VerifierState.Disabled -> "System 2: off (${state.reason})"
        VerifierState.Idle -> "System 2: Laya ready"
        is VerifierState.Loading -> "System 2: ${state.message}"
        is VerifierState.Busy -> "System 2: ${state.done}/${state.total}"
        is VerifierState.Error -> "System 2: error"
    }
    AssistChip(
        onClick = {},
        label = { Text(label, color = if (state is VerifierState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
        leadingIcon = {
            if (state is VerifierState.Loading || state is VerifierState.Busy) {
                CircularProgressIndicator(Modifier.width(14.dp).height(14.dp), strokeWidth = 2.dp)
            }
        },
    )
}
