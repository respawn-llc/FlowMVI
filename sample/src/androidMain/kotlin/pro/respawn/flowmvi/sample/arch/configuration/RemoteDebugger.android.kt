package pro.respawn.flowmvi.sample.arch.configuration

import org.koin.mp.KoinPlatform
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.dsl.StoreBuilder
import pro.respawn.flowmvi.metrics.MetricsSink
import pro.respawn.flowmvi.metrics.NoopSink
import pro.respawn.flowmvi.metrics.api.MetricsSnapshot

/** App build flags and optional debugger implementation supplied at startup. */
data class AndroidSampleConfiguration(val debuggable: Boolean, val debugger: AndroidSampleDebugger?)

/** Debugger integration supplied by the debug APK without including it in the release APK. */
interface AndroidSampleDebugger {
    /** Install remote debugging in a sample store. */
    fun <S : MVIState, I : MVIIntent, A : MVIAction> install(builder: StoreBuilder<S, I, A>)

    /** Create a sink that sends store metrics to the debugger. */
    fun metricsSink(): MetricsSink
}

internal val androidSampleConfiguration: AndroidSampleConfiguration
    get() = KoinPlatform.getKoin().get()

actual fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreBuilder<S, I, A>.remoteDebugger() {
    androidSampleConfiguration.debugger?.install(this)
}

actual fun metricsSink(): MetricsSink =
    androidSampleConfiguration.debugger?.metricsSink() ?: NoopSink<MetricsSnapshot>()
