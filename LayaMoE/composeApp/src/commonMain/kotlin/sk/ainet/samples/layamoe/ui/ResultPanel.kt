package sk.ainet.samples.layamoe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import sk.ainet.samples.layamoe.engine.AnswerDto
import sk.ainet.samples.layamoe.engine.AskResult
import sk.ainet.samples.layamoe.engine.LayaMoeRouter
import sk.ainet.samples.layamoe.engine.formatFixed

private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

/** The result the way the original page shows it: who answered and what it cost, the routing, one card per question, the raw JSON. */
@Composable
fun ResultPanel(result: AskResult, actThreshold: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Result", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = {}, label = { Text("answered by ${result.expert}") })
            AssistChip(onClick = {}, label = { Text("encoder ${result.encoderMillis} ms") })
            AssistChip(onClick = {}, label = { Text("router + heads ${result.headsMillis} ms") })
            AssistChip(onClick = {}, label = { Text("${result.inputTokens} input tokens") })
            AssistChip(onClick = {}, label = { Text(result.kernelTier) })
        }

        result.routing?.let { routing ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Routing — \"${LayaMoeRouter.QUESTION}\"", style = MaterialTheme.typography.titleSmall)
                    for ((kind, p, mapped) in routing.topKinds()) {
                        Bar(label = "$kind → $mapped", value = p, highlighted = kind == routing.top)
                    }
                    val note = when {
                        routing.overridden -> "You chose ${routing.chosen}; the router would have picked ${routing.routerPick}."
                        routing.routerPick == LayaMoeRouter.GENERAL ->
                            if (LayaMoeRouter.expertFor(routing.top) == LayaMoeRouter.GENERAL) "\"${routing.top}\" has no expert, so the general head answered."
                            else "\"${routing.top}\" at ${formatFixed(routing.topProbability, 3)} is under the threshold ${formatFixed(routing.threshold, 2)}, so the general head answered."
                        else -> "\"${routing.top}\" at ${formatFixed(routing.topProbability, 3)} → the ${routing.chosen} head answered."
                    }
                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        for ((id, answer) in result.answers) {
            val general = result.generalAnswers?.get(id)
            val labels = result.optionLabels[id] ?: answer.keys
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(id, style = MaterialTheme.typography.titleSmall)
                        Text(answer.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(answer.headline(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    AnswerBars(answer, labels, muted = false)
                    if (general != null) {
                        Text("general head on the same encoder output", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnswerBars(general, labels, muted = true)
                    }
                    val conf = formatFixed(answer.confidence, 3)
                    val act = if (answer.confidence >= actThreshold) "confidence $conf ≥ ${formatFixed(actThreshold, 2)}: the code would act on this automatically"
                    else "confidence $conf < ${formatFixed(actThreshold, 2)}: the code would send this to a person"
                    Text(act, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        var raw by remember { mutableStateOf(false) }
        TextButton(onClick = { raw = !raw }) { Text(if (raw) "Hide raw JSON" else "Raw JSON") }
        if (raw) {
            Text(prettyJson.encodeToString(AskResult.serializer(), result), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AnswerBars(answer: AnswerDto, labels: List<String>, muted: Boolean) {
    val keys = answer.keys
    for ((i, key) in keys.withIndex()) {
        val label = when (answer.type) {
            "score" -> "$key · ${labels.getOrNull(i) ?: ""}"
            else -> labels.getOrNull(i) ?: key
        }
        val top = when (answer.type) {
            "choice" -> key == answer.choice
            "noul" -> (key == "yes") == ((answer.noul ?: 0.0) >= 0.5)
            else -> answer.probabilities.values.max() == answer.probabilities[key]
        }
        Bar(label = label, value = answer.probabilities[key] ?: 0.0, highlighted = top && !muted, muted = muted)
    }
}

@Composable
private fun Bar(label: String, value: Double, highlighted: Boolean, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            modifier = Modifier.widthIn(min = 180.dp, max = 260.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        LinearProgressIndicator(
            progress = { value.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.width(220.dp),
            color = if (muted) MaterialTheme.colorScheme.outline else if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Text("${formatFixed(value * 100, 1)} %", style = MaterialTheme.typography.bodySmall)
    }
}
