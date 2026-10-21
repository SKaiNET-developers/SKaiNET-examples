package sk.ainet.samples.layamoe.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import sk.ainet.models.laya.LayaQuestion
import sk.ainet.models.laya.LayaQuestionType
import sk.ainet.models.laya.LayaState

class QuestionsJsonTest {

    @Test
    fun every_preset_parses_into_typed_questions() {
        assertEquals(7, Presets.ALL.size)
        for (preset in Presets.ALL) {
            val questions = QuestionsJson.parseQuestions(preset.questions)
            assertTrue(questions.isNotEmpty(), preset.title)
            for ((id, q) in questions) assertTrue(q.optionCount >= 2, "${preset.title}/$id has ${q.optionCount} options")
            assertEquals(preset.jsonState, QuestionsJson.parseState(preset.state) is LayaState.Structured, preset.title)
        }
    }

    @Test
    fun the_three_types_render_as_the_reference_does() {
        val q = QuestionsJson.parseQuestions(
            """
            {
              "team": {"type": "choice", "instructions": "Which team?", "criteria": {"bug": "Something is broken", "how_to": null}},
              "kinds": {"type": "choice", "instructions": "Kind?", "criteria": ["a", "b"]},
              "urgency": {"type": "score", "instructions": "How urgent?", "criteria": ["Can wait", "Now"]},
              "angry": {"type": "noul", "instructions": "The customer sounds angry"},
              "spam": {"type": "noul", "instructions": "Spam?", "criteria": {"false": "fine", "true": "spam"}, "labels": {"false": "no", "true": "yes"}}
            }
            """.trimIndent(),
        )
        assertEquals(listOf("team", "kinds", "urgency", "angry", "spam"), q.keys.toList())
        val team = assertIs<LayaQuestion.Choice>(q["team"])
        assertEquals(listOf("bug: Something is broken", "how_to"), team.renderOptions())
        assertEquals(listOf("a", "b"), assertIs<LayaQuestion.Choice>(q["kinds"]).renderOptions())
        assertEquals(LayaQuestionType.SCORE, q["urgency"]!!.type)
        assertEquals(listOf("level 0: Can wait", "level 1: Now"), q["urgency"]!!.renderOptions())
        assertEquals(listOf("false: no, the statement does not hold", "true: yes, the statement holds"), q["angry"]!!.renderOptions())
        assertEquals(listOf("no: fine", "yes: spam"), q["spam"]!!.renderOptions())
        assertEquals(listOf("bug", "how_to"), QuestionsJson.optionLabels(team))
        assertEquals(listOf("Can wait", "Now"), QuestionsJson.optionLabels(q["urgency"]!!))
        assertEquals(listOf("no", "yes"), QuestionsJson.optionLabels(q["angry"]!!))
    }

    @Test
    fun bad_input_is_reported_with_the_question_id() {
        assertFailsWith<QuestionsJson.InvalidQuestions> { QuestionsJson.parseQuestions("not json") }
        assertFailsWith<QuestionsJson.InvalidQuestions> { QuestionsJson.parseQuestions("[]") }
        assertFailsWith<QuestionsJson.InvalidQuestions> { QuestionsJson.parseQuestions("{}") }
        val e = assertFailsWith<QuestionsJson.InvalidQuestions> { QuestionsJson.parseQuestions("""{"x": {"type": "rank", "instructions": "?"}}""") }
        assertTrue("'x'" in e.message!!)
        assertFailsWith<QuestionsJson.InvalidQuestions> { QuestionsJson.parseQuestions("""{"x": {"type": "score", "instructions": "?"}}""") }
    }

    @Test
    fun states_are_text_unless_they_are_json() {
        assertIs<LayaState.Text>(QuestionsJson.parseState("plain text with {braces} inside"))
        assertIs<LayaState.Text>(QuestionsJson.parseState("{not json"))
        assertIs<LayaState.Structured>(QuestionsJson.parseState("""  {"ticket": {"subject": "x"}}"""))
        assertIs<LayaState.Structured>(QuestionsJson.parseState("""[{"role": "user", "text": "hi"}]"""))
        assertEquals("\"hi\"".let { QuestionsJson.parseState(it) }, LayaState.Text("\"hi\""), "a bare JSON string stays text")
    }

    @Test
    fun pretty_printing_round_trips() {
        val raw = """{"a":{"type":"noul","instructions":"x"}}"""
        val pretty = QuestionsJson.pretty(raw)
        assertTrue(pretty.contains("\n"))
        assertEquals(QuestionsJson.parseQuestions(raw).keys, QuestionsJson.parseQuestions(pretty).keys)
        assertEquals("text", QuestionsJson.prettyState("text"))
    }
}
