package sk.ainet.apps.kllama.chat.data.file

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.files.get
import kotlinx.browser.document
import kotlin.coroutines.resume

actual class FilePicker {

    actual suspend fun pickFile(extensions: List<String>): FilePickerResult? = suspendCancellableCoroutine { cont ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = extensions.joinToString(",") { ".$it" }

        input.onchange = { event ->
            val file = input.files?.get(0)
            if (file != null) {
                // For Wasm, similar to JS - we get the file name but path is sandboxed
                // file.size is a JsNumber in WasmJs, convert via toInt() for typical file sizes
                val sizeBytes = file.size.toInt().toLong()
                cont.resume(FilePickerResult(
                    path = file.name,
                    name = file.name,
                    sizeBytes = sizeBytes
                ))
            } else {
                cont.resume(null)
            }
        }

        input.click()
    }
}
