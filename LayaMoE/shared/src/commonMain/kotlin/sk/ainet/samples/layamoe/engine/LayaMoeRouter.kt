package sk.ainet.samples.layamoe.engine

import sk.ainet.models.laya.LayaRouterSpec

/**
 * The prompted router of layaMOE (`laya_moe/moe.py`): the base head answers "what kind of text is
 * this?" over ten fine-grained kinds, each with a description, and five of them map to an expert.
 * Anything else — or a top probability under the threshold — stays with the original head.
 */
object LayaMoeRouter {
    const val QUESTION = "What kind of text is this?"
    const val THRESHOLD = 0.3

    val KINDS: List<Pair<String, String>> = listOf(
        "agent_action" to "A plan or command an AI agent wants to run on software, data or infrastructure",
        "public_comment" to "A comment posted by a reader on a blog, forum or social media",
        "support_ticket" to "A customer asking a software company for help with its product",
        "delivery_issue" to "A problem with shipping or delivering a parcel",
        "work_email" to "An email between colleagues at work",
        "it_alert" to "A monitoring alert or report about an IT system incident",
        "patient_message" to "A message from a patient to a doctor or clinic",
        "product_review" to "A customer review of a product they bought",
        "sales_inquiry" to "A potential customer contacting a sales team",
        "other" to "Something else",
    )

    val EXPERTS: Map<String, String> = mapOf(
        "agent_action" to "safety",
        "public_comment" to "safety",
        "support_ticket" to "customer_ops",
        "delivery_issue" to "customer_ops",
        "work_email" to "customer_ops",
    )

    val SPEC: LayaRouterSpec = LayaRouterSpec(QUESTION, KINDS, EXPERTS, THRESHOLD)

    /** The expert a kind maps to, or `general`. */
    fun expertFor(kind: String): String = EXPERTS[kind] ?: GENERAL

    /** The domains of the published evaluation an expert was trained for (`general` covers the rest). */
    val DOMAINS_BY_EXPERT: Map<String, List<String>> = mapOf(
        "safety" to listOf("agent-guardrails", "content-moderation"),
        "customer_ops" to listOf("support-tickets", "delivery-exceptions", "email-triage"),
    )

    /** The expert the evaluation's oracle routing would pick for a domain. */
    fun oracleExpertFor(domain: String): String = DOMAINS_BY_EXPERT.entries.firstOrNull { domain in it.value }?.key ?: GENERAL

    const val GENERAL = "general"
}
