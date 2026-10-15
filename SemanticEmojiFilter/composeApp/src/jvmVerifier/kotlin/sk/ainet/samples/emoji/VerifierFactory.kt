package sk.ainet.samples.emoji

import sk.ainet.samples.emoji.verify.Verifier
import sk.ainet.samples.emoji.verifier.LayaVerifier
import sk.ainet.samples.emoji.verifier.layaDesktopSource

/** Built with `-PlayaVerifier=true`: Laya loads lazily on the first stable query. */
fun createVerifier(): Verifier = LayaVerifier(layaDesktopSource())
