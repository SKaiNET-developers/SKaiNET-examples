package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.apps.llm.sampleFromLogits
import sk.ainet.benchmark.BenchmarkRunner
import sk.ainet.benchmark.benchmarkSuite
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.lang.types.FP32

fun createLlamaBenchmarkSuite(runtime: OptimizedLLMRuntime<FP32>) =
    benchmarkSuite("LLaMA Inference") {
        context { DirectCpuExecutionContext() }
        case("Single token forward pass") {
            warmup(3)
            iterations(20)
            setup { runtime.reset() }
            run { sampleFromLogits(runtime.forward(1), 0f) }
        }
        case("10-token generation") {
            warmup(2)
            iterations(10)
            setup { runtime.reset() }
            run {
                repeat(10) { sampleFromLogits(runtime.forward(1), 0f) }
            }
        }
    }

fun runLlamaBenchmark(runtime: OptimizedLLMRuntime<FP32>) {
    val suite = createLlamaBenchmarkSuite(runtime)
    val results = BenchmarkRunner.runSuite(suite)
    println(results.prettyPrint())
}
