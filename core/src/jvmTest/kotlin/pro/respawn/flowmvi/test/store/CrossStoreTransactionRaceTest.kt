package pro.respawn.flowmvi.test.store

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import pro.respawn.flowmvi.api.DelicateStoreApi
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.dsl.LambdaIntent
import pro.respawn.flowmvi.dsl.emit
import pro.respawn.flowmvi.dsl.reduceLambdas
import pro.respawn.flowmvi.dsl.state
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.installChild
import pro.respawn.flowmvi.util.configure

@OptIn(DelicateStoreApi::class)
class CrossStoreTransactionRaceTest : FreeSpec({

    configure()

    "parent-child read-modify-write is not serializable across child stores" {
        data class StateC(val value: Int = 0) : MVIState
        data class StateA(val value: Int = 11) : MVIState
        data class StateB(val value: Int = 22) : MVIState
        data class StateP(val value: Unit = Unit) : MVIState

        val parentReadMax = CompletableDeferred<Int>()
        val childSumApplied = CompletableDeferred<Unit>()
        val continueParentWrites = CompletableDeferred<Unit>()

        val storeC = store<StateC, LambdaIntent<StateC, Nothing>, Nothing>(StateC()) {
            reduceLambdas()
        }
        val storeA = store<StateA, LambdaIntent<StateA, Nothing>, Nothing>(StateA()) {
            installChild(storeC, blocking = true, force = true)
            reduceLambdas()
        }
        val storeB = store<StateB, LambdaIntent<StateB, Nothing>, Nothing>(StateB()) {
            reduceLambdas()
        }
        val storeP = store<StateP, LambdaIntent<StateP, Nothing>, Nothing>(StateP()) {
            installChild(storeA, storeB, blocking = true, force = true)
            reduceLambdas()
        }

        storeP.start(this).awaitStartup()

        storeP.emit {
            val maxValue = maxOf(storeA.state.value, storeB.state.value)
            parentReadMax.complete(maxValue)
            continueParentWrites.await()
            storeA.emit { updateState { copy(value = maxValue) } }
            storeB.emit { updateState { copy(value = maxValue) } }
        }
        testCoroutineScheduler.runCurrent()
        parentReadMax.await()

        storeA.emit {
            parentReadMax.await()
            updateState {
                val sum = value + 33
                storeC.emit { updateState { copy(value = sum) } }
                childSumApplied.complete(Unit)
                copy(value = sum)
            }
        }
        testCoroutineScheduler.runCurrent()

        childSumApplied.await()
        continueParentWrites.complete(Unit)
        testCoroutineScheduler.runCurrent()
        testCoroutineScheduler.runCurrent()

        storeA.state.value shouldBe 22
        storeB.state.value shouldBe 22
        storeC.state.value shouldBe 44

        listOf(storeA, storeB, storeC, storeP).forEach { it.closeAndWait() }
    }
})
