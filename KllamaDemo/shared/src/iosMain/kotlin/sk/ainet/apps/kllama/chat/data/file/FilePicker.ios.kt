package sk.ainet.apps.kllama.chat.data.file

/**
 * iOS file picker - requires UIDocumentPickerViewController.
 * The actual file picking is handled in the UI layer via Swift interop.
 */
actual class FilePicker {

    /**
     * On iOS, file picking must be triggered from the UI layer using UIDocumentPickerViewController.
     * This method returns null - actual implementation uses the iOS UI layer.
     */
    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? {
        // iOS requires UIDocumentPickerViewController.
        // Return null here; actual implementation uses native iOS APIs via UI layer.
        return null
    }
}
