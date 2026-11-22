package pro.respawn.flowmvi.sample.arch.configuration

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.debugger.plugin.DebuggerSink
import pro.respawn.flowmvi.debugger.plugin.enableRemoteDebugging
import pro.respawn.flowmvi.dsl.StoreBuilder
import pro.respawn.flowmvi.logging.PlatformStoreLogger
import pro.respawn.flowmvi.logging.invoke

internal val androidSampleDebugger: AndroidSampleDebugger = object : AndroidSampleDebugger {
    override fun <S : MVIState, I : MVIIntent, A : MVIAction> install(builder: StoreBuilder<S, I, A>) {
        builder.enableRemoteDebugging()
    }

    override fun metricsSink() = DebuggerSink { PlatformStoreLogger(it) }
}
