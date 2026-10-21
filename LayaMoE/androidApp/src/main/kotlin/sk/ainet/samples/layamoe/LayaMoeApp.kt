package sk.ainet.samples.layamoe

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import java.io.File
import sk.ainet.samples.layamoe.engine.InProcessMixtureClient
import sk.ainet.samples.layamoe.engine.MixtureClient
import sk.ainet.samples.layamoe.engine.MixtureService
import sk.ainet.samples.layamoe.engine.WeightLane
import sk.ainet.samples.layamoe.model.FileModelFiles

/**
 * Application-scoped client: the checkpoints live under `filesDir/layamoe`, the model runs
 * in-process on the packed Q8_0 lane (about 450 MB of weights instead of 1.6 GB of FP32), on the
 * JNI NEON kernels.
 */
class LayaMoeApp : Application() {
    val client: MixtureClient by lazy {
        InProcessMixtureClient(MixtureService(FileModelFiles(File(filesDir, "layamoe")), WeightLane.Q8_0))
    }

    /** A warning for devices that report too little memory per app; loading is still allowed. */
    fun deviceNote(): String? {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mb = am.largeMemoryClass
        return when {
            am.isLowRamDevice -> "This is a low-RAM device: the model needs about 1 GB and may not load."
            mb < 1536 -> "This device grants $mb MB per app; the model needs about 1 GB and may not load."
            else -> null
        }
    }
}
