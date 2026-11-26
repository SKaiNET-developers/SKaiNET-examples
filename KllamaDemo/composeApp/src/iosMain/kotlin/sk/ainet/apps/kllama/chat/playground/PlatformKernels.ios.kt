package sk.ainet.apps.kllama.chat.playground

import sk.ainet.exec.kernel.installNativeKernels

internal actual suspend fun installPlatformKernels() {
    installNativeKernels()
}
