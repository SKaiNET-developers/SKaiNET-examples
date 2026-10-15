package sk.ainet.samples.emoji

import android.app.ActivityManager
import android.content.Context
import sk.ainet.samples.emoji.verify.NoopVerifier
import sk.ainet.samples.emoji.verify.Verifier
import sk.ainet.samples.emoji.verifier.LayaVerifier
import sk.ainet.samples.emoji.verifier.layaAndroidSource

/**
 * Built with `-PlayaVerifier=true`. The checkpoint is 644 MB of F16 weights and about 1.3 GB
 * once up-cast to FP32 on the ART heap, so the verifier only switches on when the device grants
 * a large heap that can hold it; otherwise the UI says why it is off.
 */
fun createVerifier(context: Context): Verifier {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val largeHeapMb = am.largeMemoryClass
    if (am.isLowRamDevice || largeHeapMb < 2048) {
        return NoopVerifier("needs a ${2048} MB heap, device grants $largeHeapMb MB")
    }
    return LayaVerifier(layaAndroidSource(context))
}
