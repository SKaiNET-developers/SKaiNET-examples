package sk.ainet.samples.visiondemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }
        setContent {
            App(
                AppIcons(
                    cpu = Icons.Filled.Memory,
                    activity = Icons.Filled.MonitorHeart,
                    camera = Icons.Filled.PhotoCamera,
                    images = Icons.Filled.PhotoLibrary,
                    flip = Icons.Filled.FlipCameraAndroid,
                    upload = Icons.Filled.Upload,
                    close = Icons.Filled.Close,
                    play = Icons.Filled.PlayArrow,
                    pause = Icons.Filled.Pause,
                ),
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) recreate()
    }
}
