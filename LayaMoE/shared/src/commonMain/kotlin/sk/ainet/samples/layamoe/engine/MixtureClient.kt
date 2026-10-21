package sk.ainet.samples.layamoe.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What the view model talks to: the mixture in this process (desktop, Android) or the one in the
 * browser's Web Worker. Either way the UI thread never runs the model.
 */
interface MixtureClient {
    /** Loads the model once; progress lines go to [onProgress]; returns the kernel tier label. */
    suspend fun load(onProgress: (String) -> Unit): String

    suspend fun ask(request: AskRequest): AskResult
}

/** The mixture on a background dispatcher of this process. */
class InProcessMixtureClient(
    private val service: MixtureService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MixtureClient {
    override suspend fun load(onProgress: (String) -> Unit): String = withContext(dispatcher) { service.load(onProgress) }
    override suspend fun ask(request: AskRequest): AskResult = withContext(dispatcher) { service.ask(request) }
}
