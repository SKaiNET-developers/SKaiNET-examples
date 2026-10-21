package sk.ainet.samples.layamoe.engine

/** A ready-made request: a state and its questions in the SDK shape. */
data class Preset(val title: String, val state: String, val questions: String, val note: String = "") {
    val jsonState: Boolean get() = state.trimStart().startsWith("{") || state.trimStart().startsWith("[")
}

/**
 * The seven presets of the layaMOE page (`web/app.js`, Apache-2.0), verbatim: two agent
 * guardrails and a comment for the `safety` expert, a ticket, a delivery problem and a work email
 * for `customer_ops`, and a sales lead no expert claims.
 */
object Presets {
    val ALL: List<Preset> = listOf(
        Preset(
            "Agent guardrail: drop a production table",
            "Agent plan: run `DROP TABLE invoices` on the production database. No backup has been taken and nobody has reviewed the command.",
            """
            {
              "destructive": {"type": "noul", "instructions": "The action is destructive and cannot be undone"},
              "needs_human": {"type": "noul", "instructions": "A human should approve this action before it runs"},
              "risk": {"type": "score", "instructions": "How risky is this action?", "criteria": ["Low", "Medium", "High", "Critical"]}
            }
            """.trimIndent(),
            note = "safety expert",
        ),
        Preset(
            "Agent guardrail: dry run",
            "Agent plan: run `rm -rf /srv/uploads` on the staging server with `--dry-run`, which only prints what would be removed.",
            """
            {
              "destructive": {"type": "noul", "instructions": "The action is destructive and cannot be undone"},
              "safe_without_approval": {"type": "noul", "instructions": "It is safe to run this action without a human approving it first"},
              "risk": {"type": "score", "instructions": "How risky is this action?", "criteria": ["Low", "Medium", "High", "Critical"]}
            }
            """.trimIndent(),
            note = "safety expert",
        ),
        Preset(
            "Comment moderation",
            "Honestly the author of this post is a clueless clown, no idea why anyone reads this blog.",
            """
            {
              "verdict": {"type": "choice", "instructions": "What should happen to this comment?", "criteria": {"allow": "Fine to publish", "review": "Needs a human moderator", "remove": "Breaks the rules and should be removed"}},
              "toxicity": {"type": "score", "instructions": "How toxic is the language?", "criteria": ["None", "Mild", "Strong", "Severe"]},
              "spam": {"type": "noul", "instructions": "The comment is spam or an advertisement"}
            }
            """.trimIndent(),
            note = "safety expert",
        ),
        Preset(
            "Support ticket",
            """{"ticket": {"subject": "Invoices page broken", "text": "The invoices page throws an error since this morning. Our month-end close is today and finance is blocked."}}""",
            """
            {
              "team": {"type": "choice", "instructions": "Which team should handle this?", "criteria": {"bug": "Something is broken", "how_to": "A usage question", "feature_request": "A request for a new capability", "account_access": "Login, password or account access"}},
              "urgency": {"type": "score", "instructions": "How urgent is this?", "criteria": ["Can wait", "This week", "Today", "Right now"]},
              "angry": {"type": "noul", "instructions": "The customer sounds angry"}
            }
            """.trimIndent(),
            note = "customer_ops expert, JSON state",
        ),
        Preset(
            "Delivery exception",
            "Tracking has not moved for 12 days and the customer has written three times asking where the order is.",
            """
            {
              "action": {"type": "choice", "instructions": "What should support do?", "criteria": {"reship": "Send a replacement", "notify": "Tell the customer about the delay or outcome", "fix_address": "Contact the customer to fix the delivery details", "wait": "Wait, no action needed"}},
              "urgency": {"type": "score", "instructions": "How urgent is this?", "criteria": ["Not urgent", "Soon", "Urgent", "Immediate"]},
              "upset": {"type": "noul", "instructions": "The customer is upset"}
            }
            """.trimIndent(),
            note = "customer_ops expert",
        ),
        Preset(
            "Work email",
            "Hi, my laptop won't connect to the VPN since the update. Who handles IT issues? Thanks, Grace",
            """
            {
              "action": {"type": "choice", "instructions": "What should the recipient do with this email?", "criteria": {"reply_now": "Reply today", "reply_later": "Reply within the week", "archive": "No reply needed", "delegate": "Forward it to someone else"}},
              "needs_reply": {"type": "noul", "instructions": "The sender expects a reply"}
            }
            """.trimIndent(),
            note = "customer_ops expert",
        ),
        Preset(
            "Out of domain: sales lead (general head)",
            "Hi, I'm the VP of Engineering at a 400-person logistics company. We have budget approved and want a demo next week with our security team.",
            """
            {
              "lead_quality": {"type": "score", "instructions": "How qualified is this sales lead?", "criteria": ["Not a fit", "Weak interest", "Some interest", "Strong buying signals"]},
              "next_step": {"type": "choice", "instructions": "What should sales do next?", "criteria": {"book_demo": "Schedule a demo", "nurture": "Add to a nurture campaign", "ignore": "No action needed"}}
            }
            """.trimIndent(),
            note = "no expert: the general head answers",
        ),
    )
}
