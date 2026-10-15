package sk.ainet.samples.emoji.verifier

import sk.ainet.decisions.dsl.DecisionSchema
import sk.ainet.decisions.dsl.decisionSchema
import sk.ainet.samples.emoji.domain.CatalogItem

/** The one question the verifier asks per candidate, and the policy that turns a probability into a verdict. */
object EmojiMatchSchema {
    const val MODEL_ID = "laya-multilingual"
    const val QUESTION = "match"

    const val STATEMENT = "The emoji described in the context is a good answer to the user's search."
    const val WHEN_FALSE = "no, it does not fit the search"
    const val WHEN_TRUE = "yes, it fits the search"

    val schema: DecisionSchema = schema()

    /** The schema with a different wording, for experiments from the CLI (`verify --statement ...`). */
    /**
     * Acceptance band, measured on the base checkpoint (see README): it is conservative, so a
     * candidate is rejected only below 0.2 and accepted from 0.6; in between the badge shows "?".
     */
    const val REJECT_BELOW = 0.2
    const val ACCEPT_FROM = 0.6

    fun schema(statement: String = STATEMENT, whenFalse: String = WHEN_FALSE, whenTrue: String = WHEN_TRUE, lower: Double = REJECT_BELOW, upper: Double = ACCEPT_FROM): DecisionSchema =
        decisionSchema("emoji-match") {
            model(MODEL_ID)
            probability(QUESTION, statement) {
                this.whenFalse = whenFalse
                this.whenTrue = whenTrue
            }
            policy(version = "1") {
                // P(true) <= lower rejects, >= upper accepts, in between the model abstains and the badge shows "?".
                boolean { abstainBetween(lower, upper) }
            }
        }

    /** What the decision model reads: the query and one item with its English name and keywords. */
    fun state(query: String, item: CatalogItem): String = buildString {
        append("User search: \"").append(query.trim()).append("\"\n")
        append("Emoji: ").append(item.emoji).append(' ').append(item.nameEn)
        if (item.keywordsEn.isNotEmpty()) append(" — ").append(item.keywordsEn.take(6).joinToString(", "))
        if (item.nameDe.isNotBlank() && item.nameDe != item.nameEn) append(" (").append(item.nameDe).append(')')
    }
}
