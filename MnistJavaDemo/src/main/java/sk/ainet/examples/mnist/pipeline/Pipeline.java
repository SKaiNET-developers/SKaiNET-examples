package sk.ainet.examples.mnist.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A lightweight, composable pipeline for orchestrating multi-step operations.
 * <p>
 * This is a Java-idiomatic orchestration abstraction -- for image transforms,
 * see SKaiNET's {@code Transform<I,O>} API used in {@link ImagePipeline}.
 *
 * @param <I> input type
 * @param <O> output type
 */
public final class Pipeline<I, O> {

    private final String name;
    private final Function<I, O> action;

    private Pipeline(String name, Function<I, O> action) {
        this.name = name;
        this.action = action;
    }

    public static <I, O> Pipeline<I, O> of(String name, Function<I, O> action) {
        return new Pipeline<>(name, action);
    }

    public O execute(I input) {
        return action.apply(input);
    }

    public <R> Pipeline<I, R> then(Pipeline<O, R> next) {
        return new Pipeline<>(name + " -> " + next.name, input -> next.execute(this.execute(input)));
    }

    public String name() {
        return name;
    }

    public static <I> Builder<I, I> builder(String name) {
        return new Builder<>(name, Function.identity());
    }

    public static final class Builder<I, O> {
        private final String name;
        private final Function<I, O> composed;
        private final List<String> stepNames = new ArrayList<>();

        private Builder(String name, Function<I, O> composed) {
            this.name = name;
            this.composed = composed;
        }

        public <R> Builder<I, R> addStep(String stepName, Function<O, R> step) {
            stepNames.add(stepName);
            return new Builder<>(name, composed.andThen(step));
        }

        public Pipeline<I, O> build() {
            return new Pipeline<>(name, composed);
        }
    }
}
