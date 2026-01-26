package sk.ainet.apps.kllama.chat.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sk.ainet.apps.kllama.chat.ui.ChatInputBar
import sk.ainet.apps.kllama.chat.ui.ClearIcon
import sk.ainet.apps.kllama.chat.ui.MenuIcon
import sk.ainet.apps.kllama.chat.ui.MessageList
import sk.ainet.apps.kllama.chat.ui.ModelInfoCard
import sk.ainet.apps.kllama.chat.ui.StatisticsPanel
import sk.ainet.apps.kllama.chat.viewmodel.ChatUiState
import sk.ainet.apps.kllama.chat.viewmodel.ChatViewModel

/**
 * Main chat screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModelPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatDrawerContent(
                    uiState = uiState,
                    onLoadModel = {
                        scope.launch { drawerState.close() }
                        onNavigateToModelPicker()
                    },
                    onUnloadModel = {
                        viewModel.unloadModel()
                    },
                    onClearChat = {
                        viewModel.clearChat()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                TopAppBar(
                    title = { Text("KLlama Chat") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(MenuIcon, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (uiState.session.messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearChat() }) {
                                Icon(ClearIcon, contentDescription = "Clear chat")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    onSendMessage = { viewModel.sendMessage(it) },
                    onStopGeneration = { viewModel.stopGeneration() },
                    isGenerating = uiState.isGenerating,
                    enabled = true // Allow demo mode even without model
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main chat area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Model info card
                    AnimatedVisibility(
                        visible = uiState.isModelLoaded || uiState.isLoadingModel
                    ) {
                        ModelInfoCard(
                            metadata = uiState.modelMetadata,
                            isLoading = uiState.isLoadingModel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onClick = onNavigateToModelPicker
                        )
                    }

                    // Message list
                    Box(modifier = Modifier.weight(1f)) {
                        MessageList(
                            messages = uiState.session.messages
                        )
                    }
                }

                // Side statistics panel (visible on wider screens when generating)
                AnimatedVisibility(
                    visible = uiState.isGenerating || uiState.statistics.tokensGenerated > 0,
                    enter = slideInVertically(),
                    exit = slideOutVertically()
                ) {
                    StatisticsPanel(
                        statistics = uiState.statistics,
                        modelMetadata = uiState.modelMetadata,
                        isGenerating = uiState.isGenerating,
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Drawer content for the chat screen.
 */
@Composable
private fun ChatDrawerContent(
    uiState: ChatUiState,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    onClearChat: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "KLlama Chat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ModelInfoCard(
            metadata = uiState.modelMetadata,
            isLoading = uiState.isLoadingModel,
            onClick = onLoadModel,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.isModelLoaded) {
            DrawerMenuItem(
                text = "Unload Model",
                onClick = onUnloadModel
            )
        }

        if (uiState.session.messages.isNotEmpty()) {
            DrawerMenuItem(
                text = "Clear Chat",
                onClick = onClearChat
            )
        }

        DrawerMenuItem(
            text = "Load Model",
            onClick = onLoadModel
        )
    }
}

@Composable
private fun DrawerMenuItem(
    text: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(text)
    }
}
