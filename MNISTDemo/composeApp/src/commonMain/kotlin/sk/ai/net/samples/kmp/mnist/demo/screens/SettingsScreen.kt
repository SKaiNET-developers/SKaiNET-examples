package sk.ai.net.samples.kmp.mnist.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sk.ai.net.samples.kmp.mnist.demo.settings.AppSettings
import androidx.compose.runtime.collectAsState
import sk.ainet.clean.domain.model.ModelId

/**
 * Settings screen for app configuration
 */
@Composable
fun SettingsScreen() {
    val scrollState = rememberScrollState()
    
    // State for settings
    var brushSize by remember { mutableStateOf(10f) }
    var darkMode by remember { mutableStateOf(false) }
    var autoClassify by remember { mutableStateOf(false) }
    var showProbabilities by remember { mutableStateOf(false) }
    val selectedModel by AppSettings.selectedModelId.collectAsState(initial = ModelId.CNN_MNIST)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        // Drawing Settings
        SettingsSection(title = "Drawing Settings") {
            // Brush Size Slider
            SettingItem(title = "Brush Size") {
                Column {
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 5f..20f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${brushSize.toInt()} px",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        
        // Appearance Settings
        SettingsSection(title = "Appearance") {
            // Dark Mode Switch
            SettingItem(title = "Dark Mode") {
                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }
        }
        
        // Recognition Settings
        SettingsSection(title = "Recognition Settings") {
            // Model selector (radio buttons)
            Text(
                text = "Model",
                style = MaterialTheme.typography.bodyLarge
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // CNN option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CNN (MNIST)", style = MaterialTheme.typography.bodyMedium)
                    RadioButton(
                        selected = selectedModel == ModelId.CNN_MNIST,
                        onClick = { AppSettings.setSelectedModel(ModelId.CNN_MNIST) }
                    )
                }
                // MLP option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("MLP (MNIST)", style = MaterialTheme.typography.bodyMedium)
                    RadioButton(
                        selected = selectedModel == ModelId.MLP_MNIST,
                        onClick = { AppSettings.setSelectedModel(ModelId.MLP_MNIST) }
                    )
                }
            }
            // Auto Classify Switch
            SettingItem(title = "Auto Classify") {
                Switch(
                    checked = autoClassify,
                    onCheckedChange = { autoClassify = it }
                )
            }
            
            // Show Probabilities Switch
            SettingItem(title = "Show Probabilities") {
                Switch(
                    checked = showProbabilities,
                    onCheckedChange = { showProbabilities = it }
                )
            }
        }
        
        // About Section
        SettingsSection(title = "About") {
            SettingItem(title = "Version") {
                Text(
                    text = "1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            SettingItem(title = "Model Version") {
                Text(
                    text = "MNIST v1.0",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Reset Button
        Button(
            onClick = {
                // Reset settings to defaults
                brushSize = 10f
                darkMode = false
                autoClassify = false
                showProbabilities = false
                AppSettings.setSelectedModel(ModelId.CNN_MNIST)
            },
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(0.7f)
        ) {
            Text("Reset to Defaults")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        
        content()
    }
}