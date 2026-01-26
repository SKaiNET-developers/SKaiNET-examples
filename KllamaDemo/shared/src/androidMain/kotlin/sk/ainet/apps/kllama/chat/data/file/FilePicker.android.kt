package sk.ainet.apps.kllama.chat.data.file

/**
 * Android file picker - requires Activity context for SAF integration.
 * The actual file picking is handled in the UI layer via ActivityResultContracts.
 * This class provides a placeholder that will be wired up with actual Activity integration.
 */
actual class FilePicker {

    /**
     * On Android, file picking must be triggered from the UI layer using ActivityResultContracts.
     * This method returns null - actual picking is handled via FilePickerLauncher in the UI.
     */
    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? {
        // Android requires Activity-based document picker.
        // Return null here; actual implementation uses DocumentProvider via UI layer.
        return null
    }
}
