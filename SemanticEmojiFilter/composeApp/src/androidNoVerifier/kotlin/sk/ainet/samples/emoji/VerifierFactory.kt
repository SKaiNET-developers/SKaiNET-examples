package sk.ainet.samples.emoji

import android.content.Context
import sk.ainet.samples.emoji.verify.NoopVerifier
import sk.ainet.samples.emoji.verify.Verifier

/** Built without `-PlayaVerifier=true`: the second stage is off. */
fun createVerifier(context: Context): Verifier = NoopVerifier("build with -PlayaVerifier=true")
