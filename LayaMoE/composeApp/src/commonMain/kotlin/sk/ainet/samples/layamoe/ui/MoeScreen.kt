package sk.ainet.samples.layamoe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.ainet.samples.layamoe.BuildInfo
import sk.ainet.samples.layamoe.engine.LayaMoeRouter
import sk.ainet.samples.layamoe.engine.Presets
import sk.ainet.samples.layamoe.engine.formatFixed
import sk.ainet.samples.layamoe.model.LayaMoeCheckpoints
import sk.ainet.samples.layamoe.platform.kernelTierLabel
import sk.ainet.samples.layamoe.vm.LoadState
import sk.ainet.samples.layamoe.vm.MoeViewModel
import sk.ainet.ui.components.PoweredBySKaiNET

/** The whole page: load on demand, a state and its typed questions, the expert controls, the result. */
@Composable
fun MoeScreen(viewModel: MoeViewModel, deviceNote: String?) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val stateText by viewModel.stateText.collectAsStateWithLifecycle()
    val questionsText by viewModel.questionsText.collectAsStateWithLifecycle()
    val expert by viewModel.expert.collectAsStateWithLifecycle()
    val compare by viewModel.compare.collectAsStateWithLifecycle()
    val threshold by viewModel.actThreshold.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val ready = status is LoadState.Ready

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(deviceNote)
            LoadCard(status, downloadBytes = viewModel.downloadBytes, onLoad = viewModel::load)

            Text("1 · State", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = stateText,
                onValueChange = { viewModel.stateText.value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Text, or a JSON object / array") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2 · Questions", style = MaterialTheme.typography.titleMedium)
                PresetPicker(onPick = viewModel::applyPreset)
            }
            OutlinedTextField(
                value = questionsText,
                onValueChange = { viewModel.questionsText.value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                label = { Text("JSON in the Python SDK shape: {\"id\": {\"type\": \"choice | score | noul\", \"instructions\": …, \"criteria\": …}}") },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            )

            Text("3 · Expert", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = expert == "auto", onClick = { viewModel.expert.value = "auto" }, label = { Text("Auto (router decides)") })
                FilterChip(selected = expert == LayaMoeRouter.GENERAL, onClick = { viewModel.expert.value = LayaMoeRouter.GENERAL }, label = { Text("general") })
                for (name in LayaMoeCheckpoints.EXPERT_NAMES) {
                    FilterChip(selected = expert == name, onClick = { viewModel.expert.value = name }, label = { Text(name) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = compare, onCheckedChange = { viewModel.compare.value = it })
                Text("Compare with the general head", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Act automatically at confidence ≥ ${formatFixed(threshold, 2)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 260.dp))
                Slider(value = threshold.toFloat(), onValueChange = { viewModel.actThreshold.value = it.toDouble() }, valueRange = 0.5f..1f, modifier = Modifier.width(220.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::ask, enabled = ready && !busy) { Text(if (busy) "Running…" else "Run") }
                if (busy) {
                    CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    Text("one encoder pass per question, plus one for the router", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!ready) Text("load the model first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            result?.let { r ->
                Spacer(Modifier.height(4.dp))
                ResultPanel(result = r, actThreshold = threshold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(deviceNote: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("LayaMoE — domain experts on top of Laya", style = MaterialTheme.typography.headlineSmall)
        Text(
            "One shared Laya encoder, small expert heads for specific domains, and a router that picks one per request. " +
                "Typed decisions — a choice, a score, a yes/no probability — about any text, computed on this device. " +
                "Sequence-level routing, one head per request: not a sparse mixture of experts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PlatformChips()
            AssistChip(onClick = {}, label = { Text("transformers ${BuildInfo.TRANSFORMERS_VERSION}") })
        }
        // Fills the width by design (the shared component centres its one line), so it gets its own row.
        PoweredBySKaiNET(BuildInfo.SKAINET_VERSION)
        if (deviceNote != null) Text(deviceNote, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LoadCard(status: LoadState, downloadBytes: Long, onLoad: () -> Unit) {
    val gb = formatFixed(downloadBytes / 1e9, 2)
    when (status) {
        is LoadState.Idle -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onLoad) { Text("Load model") }
            Text(
                "$gb GB on first use: the base checkpoint (ModernBERT-large, Apache-2.0) and two expert heads, cached afterwards. Nothing you type leaves this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is LoadState.Loading -> Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(status.message, style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        is LoadState.Ready -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = {}, label = { Text("model ready") })
            AssistChip(onClick = {}, label = { Text(status.kernelTier.ifBlank { kernelTierLabel() }) })
            AssistChip(onClick = {}, label = { Text("general + ${LayaMoeCheckpoints.EXPERT_NAMES.joinToString(" + ")}") })
        }
        is LoadState.Failed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onLoad) { Text("Retry") }
            Text("Model failed to load: ${status.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PresetPicker(onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("Choose an example…") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Presets.ALL.forEachIndexed { index, preset ->
                DropdownMenuItem(
                    text = { Column { Text(preset.title); Text(preset.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { open = false; onPick(index) },
                )
            }
        }
    }
}
