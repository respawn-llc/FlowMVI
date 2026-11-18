package pro.respawn.flowmvi.benchmarks.setup.orbit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.orbitContainer
import pro.respawn.flowmvi.benchmarks.setup.BenchmarkState

internal class OrbitCounterHost(
    scope: CoroutineScope,
) : OrbitContainerHost<BenchmarkState, BenchmarkState, Unit> {

    override val container = scope.orbitContainer<BenchmarkState, Unit>(
        initialState = BenchmarkState(),
        buildSettings = { sideEffectBufferSize = Channel.UNLIMITED },
    )

    fun increment() = blockingIntent(registerIdling = false) {
        reduce { state.copy(counter = state.counter + 1) }
    }

    fun touch() = blockingIntent(registerIdling = false) { }
}
