package sk.ainet.examples.mnist.pipeline;

import org.junit.Test;

import static org.junit.Assert.*;

public class PipelineTest {

    @Test
    public void singleStepPipeline() {
        var pipeline = Pipeline.of("double", (Integer x) -> x * 2);
        assertEquals(10, (int) pipeline.execute(5));
    }

    @Test
    public void pipelineChaining() {
        var double_ = Pipeline.of("double", (Integer x) -> x * 2);
        var addOne = Pipeline.of("add-one", (Integer x) -> x + 1);
        var combined = double_.then(addOne);

        assertEquals(11, (int) combined.execute(5));
    }

    @Test
    public void pipelineChainingDifferentTypes() {
        var toString = Pipeline.of("to-string", (Integer x) -> "n=" + x);
        var length = Pipeline.of("length", (String s) -> s.length());
        var combined = toString.then(length);

        assertEquals(4, (int) combined.execute(42));
    }

    @Test
    public void builderPattern() {
        var pipeline = Pipeline.<Integer>builder("math")
                .addStep("double", x -> x * 2)
                .addStep("add-ten", x -> x + 10)
                .build();

        assertEquals(20, (int) pipeline.execute(5));
    }

    @Test
    public void builderWithTypeChange() {
        var pipeline = Pipeline.<String>builder("converter")
                .addStep("parse", Integer::parseInt)
                .addStep("square", x -> x * x)
                .build();

        assertEquals(25, (int) pipeline.execute("5"));
    }

    @Test
    public void pipelineName() {
        var p = Pipeline.of("my-pipeline", (String s) -> s.length());
        assertEquals("my-pipeline", p.name());
    }

    @Test
    public void chainedPipelineName() {
        var a = Pipeline.of("a", (Integer x) -> x);
        var b = Pipeline.of("b", (Integer x) -> x);
        assertEquals("a -> b", a.then(b).name());
    }
}
