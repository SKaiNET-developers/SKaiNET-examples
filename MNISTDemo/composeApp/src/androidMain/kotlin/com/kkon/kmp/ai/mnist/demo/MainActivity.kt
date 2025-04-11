package com.kkon.kmp.ai.mnist.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.io.asSource
import sk.ai.net.samples.kmp.mnist.demo.App
import java.io.InputStream
import kotlinx.io.buffered



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Suppose we have a file to read from:
        val inputStream: InputStream? = assets.open("mnist.json");

        setContent {
            inputStream?.let {
                App {
                    it.asSource().buffered()
                }
            }
        }
    }
}
