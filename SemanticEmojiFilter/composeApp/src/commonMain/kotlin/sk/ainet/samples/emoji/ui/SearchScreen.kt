package sk.ainet.samples.emoji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.ainet.samples.emoji.platform.kernelTierLabel
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel
import sk.ainet.samples.emoji.vm.LoadState

@Composable
fun SearchScreen(viewModel: EmojiSearchViewModel, skainetVersion: String) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val ranked by viewModel.ranked.collectAsStateWithLifecycle()
    val verdicts by viewModel.verdicts.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val verifierState by viewModel.verifier.state.collectAsStateWithLifecycle()
    var german by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Header(skainetVersion, german, onLanguage = { german = it })
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = status is LoadState.Ready,
                label = { Text(if (german) "Beschreibe, was du meinst…" else "Describe what you mean…") },
                placeholder = { Text(if (german) "gesundes Essen · mir ist schlecht · Rockband" else "healthy things to eat · feeling sick · rock band") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.height(8.dp))

            when (val s = status) {
                is LoadState.Idle, is LoadState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text((s as? LoadState.Loading)?.message ?: "Starting…", style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                is LoadState.Failed -> Text("Model failed to load: ${s.message}", color = MaterialTheme.colorScheme.error)
                LoadState.Ready -> {
                    DiagnosticsRow(stats = stats, verifierState = verifierState, kernel = kernelTierLabel(), onEnableVerifier = viewModel.verifier::enable)
                    Spacer(Modifier.height(8.dp))
                    EmojiGrid(ranked = ranked, verdicts = verdicts, german = german, queryActive = query.isNotBlank(), modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Header(skainetVersion: String, german: Boolean, onLanguage: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Semantic Emoji Filter", style = MaterialTheme.typography.headlineSmall)
            Text(
                "On-device sentence embeddings rank the catalog as you type; a decision model double-checks the top hits. SKaiNET $skainetVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = !german, onClick = { onLanguage(false) }, label = { Text("EN") })
            FilterChip(selected = german, onClick = { onLanguage(true) }, label = { Text("DE") })
        }
    }
    Spacer(Modifier.height(8.dp))
    PlatformChips()
}
