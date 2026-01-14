package sk.ainet.app.samples.sinus

import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SinusTrainerTest {

    @Test
    fun testTrainingStarts() = runTest {
        val trainer = SinusTrainer()
        val progress = trainer.train(epochs = 1).last()
        assertTrue(progress.epoch == 1)
        assertTrue(progress.loss >= 0f)
    }
}
