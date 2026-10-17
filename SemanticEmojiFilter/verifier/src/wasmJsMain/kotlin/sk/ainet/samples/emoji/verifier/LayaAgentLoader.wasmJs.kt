package sk.ainet.samples.emoji.verifier

import sk.ainet.context.ExecutionContext
import sk.ainet.lang.types.FP32
import sk.ainet.models.laya.LayaAgent

/** The web has no checkpoint directory; [LayaWebSource] builds the agent from fetched bytes instead. */
actual fun loadLayaAgent(directory: String, ctx: ExecutionContext): LayaAgent<FP32> =
    error("the web build has no filesystem ($directory); use LayaWebSource")
