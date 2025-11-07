package com.kkon.kmp.ai.sinus.approximator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.io.Buffer
import sk.ai.net.samples.kmp.sinus.approximator.App
import sk.ai.net.samples.kmp.sinus.approximator.LoadingState
import sk.ai.net.samples.kmp.sinus.approximator.ResourceUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val loadingState by ResourceUtils.loadingState.collectAsState()

            // Only show the app when the resource is loaded
            if (loadingState == LoadingState.Success) {
                App {
                    // Provide a Source for the sinus.json file
                    ResourceUtils.getSourceFromResource("files/sinus.json") ?: Buffer()
                }
            } else if (loadingState is LoadingState.Error) {
                // Show error message
                val errorMessage = (loadingState as LoadingState.Error).message
                Text("Error loading resource: $errorMessage")
            } else {
                // Show loading indicator
                CircularProgressIndicator()
            }
        }
    }
}
