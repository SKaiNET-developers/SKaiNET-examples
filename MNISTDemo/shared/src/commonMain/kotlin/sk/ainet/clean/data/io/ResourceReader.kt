package sk.ainet.clean.data.io

/**
 * Platform-agnostic resource reader abstraction (PRD §7).
 * Platform code will provide an implementation (e.g., Android assets, Desktop resources, Wasm bundled bytes).
 */
interface ResourceReader {
    /**
     * Read the resource at [path] into memory. Returns null if not found.
     */
    suspend fun read(path: String): ByteArray?
}
