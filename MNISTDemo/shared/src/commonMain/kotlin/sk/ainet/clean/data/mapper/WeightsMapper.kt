package sk.ainet.clean.data.mapper

/**
 * Placeholder for translating raw bytes into the exact form needed by InferenceModule.load (PRD §4, §7).
 * Currently a pass-through; adjust when the inference engine expects a different layout/format.
 */
object WeightsMapper {
    fun toInferenceBytes(bytes: ByteArray): ByteArray = bytes
}
