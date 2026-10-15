package sk.ainet.samples.emoji.verifier

import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32
import sk.ainet.models.laya.LayaAgent

/** Builds the agent from a checkpoint directory; the JVM has a loader in the library, Android replicates it. */
expect fun loadLayaAgent(directory: String, ctx: ExecutionContext): LayaAgent<FP32>
