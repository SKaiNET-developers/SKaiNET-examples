package sk.ainet.samples.layamoe.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import sk.ainet.models.laya.LayaAnswer
import sk.ainet.models.laya.LayaQuestion
import sk.ainet.models.laya.LayaQuestionType

class RouterAndDtoTest {

    @Test
    fun the_router_spec_is_the_published_one() {
        assertEquals(10, LayaMoeRouter.KINDS.size)
        assertEquals(LayaMoeRouter.KINDS.map { it.first }, LayaMoeRouter.SPEC.layaQuestion.keys)
        assertEquals(LayaQuestionType.CHOICE, LayaMoeRouter.SPEC.layaQuestion.type)
        assertEquals(0.3, LayaMoeRouter.SPEC.threshold)
        assertEquals("safety", LayaMoeRouter.expertFor("agent_action"))
        assertEquals("customer_ops", LayaMoeRouter.expertFor("work_email"))
        assertEquals("general", LayaMoeRouter.expertFor("it_alert"))
        assertEquals("safety", LayaMoeRouter.oracleExpertFor("content-moderation"))
        assertEquals("general", LayaMoeRouter.oracleExpertFor("sales-leads"))
        val r = LayaMoeRouter.SPEC.route(mapOf("agent_action" to 0.31, "other" to 0.69 - 0.69 + 0.0), setOf("safety", "customer_ops"), confidence = 0.2)
        assertEquals("safety", r.routerPick)
    }

    @Test
    fun answers_become_dtos_keyed_by_option() {
        val choice = LayaAnswer.Choice("bug", listOf("bug", "how_to"), doubleArrayOf(0.7, 0.3), 0.12)
        val cd = choice.toDto(LayaQuestion.Choice("?", mapOf("bug" to "x", "how_to" to "y")))
        assertEquals("choice", cd.type)
        assertEquals("bug", cd.choice)
        assertEquals(mapOf("bug" to 0.7, "how_to" to 0.3), cd.probabilities)
        assertEquals("bug", cd.headline())

        val score = LayaAnswer.Score(1.25, doubleArrayOf(0.1, 0.55, 0.35), 0.4)
        val sd = score.toDto(LayaQuestion.Score("?", listOf("a", "b", "c")))
        assertEquals(listOf("0", "1", "2"), sd.keys)
        assertEquals("score 1.25", sd.headline())

        val noul = LayaAnswer.Noul(0.8, doubleArrayOf(0.2, 0.8), 0.8)
        val nd = noul.toDto(LayaQuestion.Noul("?"))
        assertEquals(mapOf("no" to 0.2, "yes" to 0.8), nd.probabilities)
        assertEquals(0.8, nd.noul)
        assertNull(nd.choice)
        assertEquals("P(true) 80.0 %", nd.headline())
    }

    @Test
    fun fixed_formatting_has_no_locale() {
        assertEquals("0.50", formatFixed(0.5, 2))
        assertEquals("12.35", formatFixed(12.346, 2))
        assertEquals("3", formatFixed(2.7, 0))
        assertEquals("-0.25", formatFixed(-0.25, 2))
        assertEquals("100.0", formatFixed(99.96, 1))
    }
}
