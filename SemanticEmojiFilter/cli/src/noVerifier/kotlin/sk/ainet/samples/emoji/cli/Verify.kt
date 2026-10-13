package sk.ainet.samples.emoji.cli

/** Built without `-PlayaVerifier=true`. */
internal fun verifyAvailable(): Boolean = false

internal fun verifyCommand(query: String, items: List<sk.ainet.samples.emoji.domain.CatalogItem>, flags: Map<String, String> = emptyMap()) {
    System.err.println("verify: this CLI was built without -PlayaVerifier=true")
    System.exit(2)
}
