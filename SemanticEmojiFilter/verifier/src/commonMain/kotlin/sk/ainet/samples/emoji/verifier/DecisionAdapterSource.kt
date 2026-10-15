package sk.ainet.samples.emoji.verifier

import sk.ainet.decisions.DecisionAdapter

/**
 * Where the decision model comes from. This is the seam a SKaiNET decision cartridge replaces
 * later: today the Laya checkpoint is downloaded file by file and loaded through the
 * transformers adapter; a cartridge-backed source would open a bundled or locally imported
 * cartridge instead. Nothing above this interface knows the difference.
 */
interface DecisionAdapterSource {
    val modelId: String

    /** Loads the model; called once, on the first verification, off the UI thread. */
    suspend fun open(onProgress: (String) -> Unit): DecisionAdapter
}
