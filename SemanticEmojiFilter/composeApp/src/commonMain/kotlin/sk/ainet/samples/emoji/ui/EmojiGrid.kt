package sk.ainet.samples.emoji.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.ainet.samples.emoji.domain.RankedItem
import sk.ainet.samples.emoji.domain.Tier
import sk.ainet.samples.emoji.verify.Verdict

/**
 * Two tiers: the items above the calibrated threshold first, with their confidence badge and,
 * when the verifier has spoken, its probability next to it; everything else below in rank order.
 */
@Composable
fun EmojiGrid(ranked: List<RankedItem>, verdicts: Map<String, Verdict>, german: Boolean, queryActive: Boolean, modifier: Modifier = Modifier) {
    val top = if (queryActive) ranked.filter { it.tier == Tier.TOP } else emptyList()
    val rest = if (queryActive) ranked.filter { it.tier == Tier.REST } else ranked
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 112.dp), modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (queryActive) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel(if (german) "Treffer (${top.size})" else "Matches (${top.size})")
            }
            if (top.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(if (german) "Nichts über der Schwelle — die nächsten Kandidaten stehen unten." else "Nothing above the threshold; the closest candidates are below.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(top, key = { it.item.id }) { EmojiCard(it, verdicts[it.item.id], german, highlighted = true, pending = queryActive && verdicts[it.item.id] == null) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel(if (german) "Alle anderen" else "Everything else")
            }
        }
        items(rest, key = { it.item.id }) { EmojiCard(it, verdicts[it.item.id], german, highlighted = false, pending = false) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
fun EmojiCard(ranked: RankedItem, verdict: Verdict?, german: Boolean, highlighted: Boolean, pending: Boolean) {
    val container by animateColorAsState(
        when {
            verdict?.accepted == true -> MaterialTheme.colorScheme.primaryContainer
            verdict?.accepted == false -> MaterialTheme.colorScheme.errorContainer
            highlighted -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    )
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ranked.item.emoji, fontSize = 34.sp, fontFamily = LocalEmojiFontFamily.current)
            Spacer(Modifier.height(4.dp))
            Text(ranked.item.name(german), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (highlighted || ranked.cosine != 0f) {
                Spacer(Modifier.height(4.dp))
                ConfidenceBadge(ranked, verdict, pending)
            }
        }
    }
}

/** `cos 0.71 · Laya 0.12`; the vector score and the decision model side by side. */
@Composable
fun ConfidenceBadge(ranked: RankedItem, verdict: Verdict?, pending: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${(ranked.confidence * 100).toInt()} %", style = MaterialTheme.typography.labelSmall)
        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
            verdict != null -> {
                val text = verdict.probabilityTrue?.let { "Laya ${(it * 100).toInt()} %" + (if (verdict.accepted == null) " ?" else "") } ?: "Laya !"
                val color = when (verdict.accepted) {
                    true -> Color(0xFF2E7D32)
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text, style = MaterialTheme.typography.labelSmall, color = color)
            }
            pending -> Box { CircularProgressIndicator(Modifier.width(10.dp).height(10.dp), strokeWidth = 1.5.dp) }
            else -> Text("cos ${formatCosine(ranked.cosine)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Two decimals without `String.format`, which the wasm target does not have. */
private fun formatCosine(value: Float): String {
    val hundredths = (value * 100).toInt()
    val sign = if (hundredths < 0) "-" else ""
    val abs = kotlin.math.abs(hundredths)
    return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
}
