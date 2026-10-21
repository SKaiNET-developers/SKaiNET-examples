package sk.ainet.samples.layamoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sk.ainet.samples.layamoe.vm.MoeViewModel

class MainActivity : ComponentActivity() {

    private val app get() = applicationContext as LayaMoeApp

    private val viewModel: MoeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MoeViewModel(app.client) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(viewModel = viewModel, deviceNote = app.deviceNote())
        }
    }
}
