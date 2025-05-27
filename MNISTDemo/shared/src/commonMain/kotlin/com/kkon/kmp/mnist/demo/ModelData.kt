package com.kkon.kmp.mnist.demo

import kotlinx.io.Source
import sk.ai.net.Shape
import sk.ai.net.io.csv.CsvParametersLoader
import sk.ai.net.io.mapper.NamesBasedValuesModelMapper
import sk.ai.net.nn.reflection.flattenParams
import sk.ai.net.nn.reflection.summary


class ADigitClassifier(private val handleSource: () -> Source) : DigitClassifier {

    private val model = DigitClassifierNN()

    override fun classify(image: DigitClassifier.GrayScale28To28Image): Int =
        5 //TODO implement real logic, its a placeholder now.

    override suspend fun loadModel() {
        print(model.summary(Shape(128, 784)))
        val parametersLoader = CsvParametersLoader(handleSource)

        val mapper = NamesBasedValuesModelMapper()
        print(model.summary(Shape(128, 784)))

        parametersLoader.load { name, shape ->
            mapper.mapToModel(model, mapOf(name to shape))
        }
        val params = flattenParams(model)
        print(params)
    }
}


