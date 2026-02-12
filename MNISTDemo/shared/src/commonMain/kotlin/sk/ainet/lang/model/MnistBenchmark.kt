package sk.ainet.lang.model

import sk.ainet.benchmark.BenchmarkRunner
import sk.ainet.benchmark.benchmarkSuite
import sk.ainet.clean.data.image.GrayScale28To28Image
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.types.FP32

fun createMnistBenchmarkSuite(
    mlpModel: Module<FP32, Float>,
    cnnModel: Module<FP32, Float>,
    testImage: GrayScale28To28Image
) = benchmarkSuite("MNIST Classification") {
    context { DirectCpuExecutionContext() }
    case("MLP inference") {
        warmup(5)
        iterations(50)
        run { classifyImageMLP(mlpModel, testImage) }
    }
    case("CNN inference") {
        warmup(5)
        iterations(50)
        run { classifyImageCNN(cnnModel, testImage) }
    }
}

fun runMnistBenchmark(
    mlpModel: Module<FP32, Float>,
    cnnModel: Module<FP32, Float>,
    testImage: GrayScale28To28Image
) {
    val suite = createMnistBenchmarkSuite(mlpModel, cnnModel, testImage)
    val results = BenchmarkRunner.runSuite(suite)
    println(results.prettyPrint())
}
