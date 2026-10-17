package sk.ainet.samples.emoji

import sk.ainet.samples.emoji.verify.Verifier

/**
 * Built with `-PlayaVerifier=true`: Laya runs in a Web Worker (the :webVerifier module, shipped
 * under laya-worker/ next to the app). It starts only when the user taps the System 2 chip,
 * because the first run downloads the 644 MB checkpoint into the browser cache.
 */
fun createVerifier(): Verifier = WorkerLayaVerifier("./laya-worker/SemanticEmojiFilter-webVerifier.mjs")
