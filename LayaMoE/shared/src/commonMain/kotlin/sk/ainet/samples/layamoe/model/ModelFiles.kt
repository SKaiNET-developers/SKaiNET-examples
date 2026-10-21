package sk.ainet.samples.layamoe.model

import sk.ainet.io.RandomAccessSource

/**
 * Where the checkpoint bytes come from — the one seam that differs per platform. Desktop and
 * Android keep a directory of downloaded files (`FileModelFiles`); the browser keeps Blobs in the
 * Cache API and reads them through the engine's Blob source inside the Web Worker (`WebModelFiles`).
 * Everything above this interface — loading, routing, answering — is common code.
 */
interface ModelFiles {
    /** Makes every file available locally, downloading what is missing; progress lines go to [onProgress]. */
    suspend fun ensure(files: List<CheckpointFile>, onProgress: (String) -> Unit)

    /** The whole file as text (the JSON side files). */
    suspend fun text(file: CheckpointFile): String

    /** A fresh random-access source over a weight file, the shape the engine's SafeTensors loader takes. */
    fun weights(file: CheckpointFile): () -> RandomAccessSource
}
