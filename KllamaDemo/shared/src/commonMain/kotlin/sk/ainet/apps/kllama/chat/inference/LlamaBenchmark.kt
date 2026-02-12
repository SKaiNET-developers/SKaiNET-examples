package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.runtime.LlamaRuntime
import sk.ainet.benchmark.BenchmarkRunner
import sk.ainet.benchmark.benchmarkSuite
import sk.ainet.context.DirectCpuExecutionContext

fun createLlamaBenchmarkSuite(runtime: LlamaRuntime) =
    benchmarkSuite("LLaMA Inference") {
        context { DirectCpuExecutionContext() }
        case("Single token forward pass") {
            warmup(3)
            iterations(20)
            setup { runtime.reset() }
            run { runtime.forward(1) }
        }
        case("10-token generation") {
            warmup(2)
            iterations(10)
            setup { runtime.reset() }
            run {
                repeat(10) { runtime.forward(1) }
            }
        }
    }

fun runLlamaBenchmark(runtime: LlamaRuntime) {
    val suite = createLlamaBenchmarkSuite(runtime)
    val results = BenchmarkRunner.runSuite(suite)
    println(results.prettyPrint())
}
