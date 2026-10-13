package sk.ainet.samples.emoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import sk.ainet.samples.emoji.vm.EmojiSearchViewModel

class MainActivity : ComponentActivity() {

    private val app get() = applicationContext as EmojiApp

    private val viewModel: EmojiSearchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = EmojiSearchViewModel(
                loadEngine = { onProgress -> app.loadEngine(onProgress) },
                loadIndex = { engine, onProgress -> CatalogResources.index(CatalogResources.catalog(), engine, onProgress = onProgress) },
                calibration = runBlocking { CatalogResources.calibration() },
                verifier = app.verifier,
            ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(viewModel = viewModel, skainetVersion = BuildConfig.SKAINET_VERSION)
        }
    }
}
