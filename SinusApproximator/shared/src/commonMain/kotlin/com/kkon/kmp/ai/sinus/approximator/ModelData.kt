package com.kkon.kmp.ai.sinus.approximator

import sk.ai.net.Shape
import sk.ai.net.impl.DoublesTensor
import sk.ai.net.nn.Module
import sk.ai.net.nn.reflection.flattenParams
import sk.ai.net.nn.reflection.summary


class ASinusCalculator(private val modelLoader:(Module)-> Unit) : SinusCalculator {
    val model = SineNN()


    override fun calculate(x: Double): Double =
        (model.of(x) as DoublesTensor)[0]


    override suspend fun loadModel() {
        modelLoader(model)
        print(model.summary(Shape(1)))
        val params = flattenParams(model)
        print(params)
    }
}


