package sk.ai.net.samples.kmp.mnist.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.io.Source
import org.jetbrains.compose.ui.tooling.preview.Preview
import sk.ai.net.samples.kmp.mnist.demo.navigation.NavigationHost
import sk.ai.net.samples.kmp.mnist.demo.navigation.Screen
import sk.ai.net.samples.kmp.mnist.demo.navigation.rememberNavigationState
import sk.ai.net.samples.kmp.mnist.demo.screens.HomeScreen
import sk.ai.net.samples.kmp.mnist.demo.screens.SettingsScreen
import sk.ai.net.samples.kmp.mnist.demo.theme.AppTheme
import sk.ai.net.samples.kmp.mnist.demo.theme.AppSurface
import sk.ai.net.samples.kmp.mnist.demo.ui.LocalHandleSource
import sk.ai.net.samples.kmp.mnist.demo.ui.Orientation
import sk.ai.net.samples.kmp.mnist.demo.ui.ResponsiveLayout
import sk.ai.net.samples.kmp.mnist.demo.ui.WindowSizeClass
import sk.ai.net.samples.kmp.mnist.demo.ui.isLandscape

@Composable
@Preview
fun App(handleSource: () -> Source) {
    AppTheme {
        AppSurface {
            // Provide the handleSource function to all composables in the app
            CompositionLocalProvider(LocalHandleSource provides handleSource) {
                // Create navigation state
                val navigationState = rememberNavigationState(handleSource)

                // Use ResponsiveLayout to adapt to different screen sizes and orientations
                ResponsiveLayout { windowSizeClass, orientation ->
                    if (orientation.isLandscape() && windowSizeClass != WindowSizeClass.COMPACT) {
                        // Use side navigation for landscape on larger screens
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Side navigation
                            NavigationRail(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                                ) {
                                    NavigationRailItem(
                                        selected = navigationState.currentScreen == Screen.HOME,
                                        onClick = { navigationState.currentScreen = Screen.HOME },
                                        label = { Text("Home") },
                                        icon = { /* No icon for now */ }
                                    )

                                    NavigationRailItem(
                                        selected = navigationState.currentScreen == Screen.DRAWING,
                                        onClick = { navigationState.currentScreen = Screen.DRAWING },
                                        label = { Text("Draw") },
                                        icon = { /* No icon for now */ }
                                    )

                                    NavigationRailItem(
                                        selected = navigationState.currentScreen == Screen.SETTINGS,
                                        onClick = { navigationState.currentScreen = Screen.SETTINGS },
                                        label = { Text("Settings") },
                                        icon = { /* No icon for now */ }
                                    )
                                }
                            }

                            // Content area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                NavigationHost(
                                    navigationState = navigationState,
                                    homeScreen = { 
                                        HomeScreen(
                                            onGetStarted = { 
                                                navigationState.currentScreen = Screen.DRAWING 
                                            }
                                        ) 
                                    },
                                    drawingScreen = { source -> DrawingScreen(source) },
                                    settingsScreen = { SettingsScreen() }
                                )
                            }
                        }
                    } else {
                        // Use bottom navigation for portrait and compact screens
                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    // Home tab
                                    NavigationBarItem(
                                        selected = navigationState.currentScreen == Screen.HOME,
                                        onClick = { navigationState.currentScreen = Screen.HOME },
                                        icon = { /* No icon for now */ },
                                        label = { Text("Home") }
                                    )

                                    // Drawing tab
                                    NavigationBarItem(
                                        selected = navigationState.currentScreen == Screen.DRAWING,
                                        onClick = { navigationState.currentScreen = Screen.DRAWING },
                                        icon = { /* No icon for now */ },
                                        label = { Text("Draw") }
                                    )

                                    // Settings tab
                                    NavigationBarItem(
                                        selected = navigationState.currentScreen == Screen.SETTINGS,
                                        onClick = { navigationState.currentScreen = Screen.SETTINGS },
                                        icon = { /* No icon for now */ },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            // Display the current screen content
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                NavigationHost(
                                    navigationState = navigationState,
                                    homeScreen = { 
                                        HomeScreen(
                                            onGetStarted = { 
                                                navigationState.currentScreen = Screen.DRAWING 
                                            }
                                        ) 
                                    },
                                    drawingScreen = { source -> DrawingScreen(source) },
                                    settingsScreen = { SettingsScreen() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
