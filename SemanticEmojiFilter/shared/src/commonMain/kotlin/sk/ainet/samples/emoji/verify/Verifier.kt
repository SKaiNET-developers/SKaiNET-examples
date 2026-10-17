package sk.ainet.samples.emoji.verify

import kotlinx.serialization.Serializable

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import sk.ainet.samples.emoji.domain.CatalogItem

/** A decision model's verdict on one candidate item for the current query. */
@Serializable
data class Verdict(
    val itemId: String,
    /** `P(item matches query)` as the model sees it; null when the model failed on this item. */
    val probabilityTrue: Double?,
    /** The accepted decision under the policy; null when the model abstained or failed. */
    val accepted: Boolean?,
    val millis: Long,
)

sealed interface VerifierState {
    /** No verifier built in, or the device cannot run it; the reason shows in the UI. */
    data class Disabled(val reason: String) : VerifierState
    /** Built in, but waits for the user (a large download, as on the web); [Verifier.enable] starts it. */
    data class OptIn(val message: String) : VerifierState
    data object Idle : VerifierState
    data class Loading(val message: String) : VerifierState
    data class Busy(val done: Int, val total: Int) : VerifierState
    data class Error(val message: String) : VerifierState
}

/**
 * The second stage ("System 2"): a typed decision model asked, per candidate, whether the
 * item matches the query. Implementations stream one [Verdict] per item in rank order and
 * stop as soon as the collector is cancelled, which happens whenever the query changes.
 */
interface Verifier {
    val state: StateFlow<VerifierState>
    fun verify(query: String, items: List<CatalogItem>): Flow<Verdict>
    /** Starts a verifier in [VerifierState.OptIn]; a no-op for the others. */
    fun enable() {}
}

/** The web build and any build without `-PlayaVerifier=true`. */
class NoopVerifier(reason: String = "not built in") : Verifier {
    override val state: StateFlow<VerifierState> = MutableStateFlow(VerifierState.Disabled(reason))
    override fun verify(query: String, items: List<CatalogItem>): Flow<Verdict> = emptyFlow()
}
