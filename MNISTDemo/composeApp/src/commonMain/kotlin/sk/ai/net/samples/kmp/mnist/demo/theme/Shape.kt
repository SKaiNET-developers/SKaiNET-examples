package sk.ai.net.samples.kmp.mnist.demo.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material Design 3 shape system
val AppShapes = Shapes(
    // Small components like buttons, chips, and small cards
    small = RoundedCornerShape(4.dp),
    
    // Medium components like cards, alerts dialogs
    medium = RoundedCornerShape(8.dp),
    
    // Large components like large cards, sheets, and expanded menus
    large = RoundedCornerShape(12.dp),
    
    // Extra large components like large sheets
    extraLarge = RoundedCornerShape(28.dp)
)