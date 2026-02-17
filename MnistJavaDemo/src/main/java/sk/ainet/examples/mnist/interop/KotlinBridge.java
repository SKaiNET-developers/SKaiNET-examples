package sk.ainet.examples.mnist.interop;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import sk.ainet.context.DirectCpuExecutionContext;
import sk.ainet.context.ExecutionContext;

/**
 * Utilities for calling Kotlin/SKaiNET APIs from Java.
 * Bridges the gap between Java's eager execution model and Kotlin's
 * coroutine-based suspend functions.
 */
public final class KotlinBridge {

    private KotlinBridge() {}

    /**
     * Runs a suspend function blockingly from Java.
     * Wraps {@code kotlinx.coroutines.runBlocking}.
     *
     * @param block a function that receives a CoroutineScope and Continuation
     * @param <T>   the result type
     * @return the result of the coroutine
     */
    @SuppressWarnings("unchecked")
    public static <T> T runBlocking(
            kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope, ? super Continuation<? super T>, ?> block
    ) {
        try {
            return (T) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    block
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Coroutine interrupted", e);
        }
    }

    /**
     * Creates a SKaiNET CPU execution context for tensor operations.
     */
    public static DirectCpuExecutionContext createExecutionContext() {
        return new DirectCpuExecutionContext();
    }
}
