package pro.respawn.flowmvi.test.decorator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.annotation.NotIntendedForInheritance
import pro.respawn.flowmvi.api.DelicateStoreApi
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.StorePlugin
import pro.respawn.flowmvi.decorator.decoratedWith
import pro.respawn.flowmvi.decorator.decorates
import pro.respawn.flowmvi.dsl.plugin
import pro.respawn.flowmvi.dsl.state
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.plugins.timeTravelPlugin
import pro.respawn.flowmvi.test.plugin.test
import pro.respawn.flowmvi.util.TestAction
import pro.respawn.flowmvi.util.TestIntent
import pro.respawn.flowmvi.util.TestState
import pro.respawn.flowmvi.util.configure
import pro.respawn.flowmvi.util.testDecorator
import pro.respawn.flowmvi.util.testTimeTravel

@OptIn(DelicateStoreApi::class, NotIntendedForInheritance::class)
class StoreDecoratorTest : FreeSpec({
    configure()

    val timeTravel = testTimeTravel()
    val timeTravelPlugin = timeTravelPlugin(timeTravel)
    afterEach {
        timeTravel.reset()
    }
    "given decorator that doesn't have a callback wrapper defined" - {
        val decorator = testDecorator {}
        "then the plugin is invoked directly" {
            (decorator decorates timeTravelPlugin).test(TestState.Some) {
                onStart()
                timeTravel.starts shouldBe 1
            }
        }
    }
    "given plugin that doesn't have a callback set" - {
        val plugin = plugin<TestState, TestIntent, TestAction> { }

        "and a decorator that defines a callback wrapper" - {
            var invocations = 0
            val decorator = testDecorator {
                onStart { chain ->
                    chain.onStart()
                    ++invocations
                }
            }
            "then the decorator callback should still be executed" {
                (decorator decorates plugin).test(TestState.Some) {
                    onStart()
                    invocations shouldBe 1
                }
            }
        }
    }
    "given multiple decorators" - {
        val invocations = mutableListOf<String>()
        val plugin = plugin<TestState, TestIntent, TestAction> {
            onStart { invocations += "plugin" }
        }
        val decorator1 = testDecorator {
            onStart { chain ->
                invocations += "first before"
                chain.onStart()
                invocations += "first after"
            }
        }
        val decorator2 = testDecorator {
            onStart { chain ->
                invocations += "second before"
                chain.onStart()
                invocations += "second after"
            }
        }
        "then decorators are applied in the correct order" {
            (plugin decoratedWith listOf(decorator1, decorator2)).test(TestState.Some) {
                onStart()
                invocations shouldContainExactly listOf(
                    "second before", "first before", "plugin", "first after", "second after",
                )
            }
        }
    }
    "given a decorator that invokes a callback" - {
        val invocations = mutableListOf<Int>()
        beforeEach { invocations.clear() }
        val decorator1 = testDecorator {
            onStart { chain ->
                chain.onStart()
                invocations += 1
            }
        }
        "and another that does not" - {
            val decorator2 = testDecorator { }
            "then if the chain is invoked in direct order, callback is invoked" {
                (timeTravelPlugin decoratedWith listOf(decorator1, decorator2)).test(TestState.Some) {
                    onStart()
                    timeTravel.starts shouldBe 1
                    invocations shouldContainExactly listOf(1)
                }
            }
            "then if the chain is invoked in reverse order, callback is invoked" {
                (timeTravelPlugin decoratedWith listOf(decorator2, decorator1)).test(TestState.Some) {
                    onStart()
                    timeTravel.starts shouldBe 1
                    invocations shouldContainExactly listOf(1)
                }
            }
        }
    }
    "given a decorator that forwards pipeline and lifecycle callbacks" - {
        val decorator = testDecorator {
            onStart { chain -> chain.onStart() }
            onIntent { chain, intent -> chain.onIntent(intent) }
            onException { chain, cause -> chain.onException(cause) }
            onState { chain, old, new -> chain.onState(old, new) }
            onAction { chain, action -> chain.onAction(action) }
            onSubscribe { chain, count -> chain.onSubscribe(count) }
            onUnsubscribe { chain, count -> chain.onUnsubscribe(count) }
            onStop { chain, cause -> chain.onStop(cause) }
        }
        "then the plugin receives the forwarded pipeline callbacks" {
            (timeTravelPlugin decoratedWith decorator).test(TestState.Some) {
                onStart()
                timeTravel.starts shouldBe 1
                onIntent(TestIntent { })
                timeTravel.intents shouldHaveSize 1
                onAction(TestAction.Some)
                timeTravel.actions shouldHaveSize 1
                onState(TestState.Some, TestState.Some)
                timeTravel.states shouldHaveSize 1
                onSubscribe(1)
                timeTravel.subscriptions shouldBe 1
                onUnsubscribe(1)
                timeTravel.unsubscriptions shouldBe 1
                val cause = IllegalStateException("test")
                onException(cause) shouldBe cause
                timeTravel.exceptions shouldContainExactly listOf(cause)
                onStop(cause)
                timeTravel.stops shouldBe 1
            }
        }
    }
    "given an intent decorator with implicit access to the pipeline" - {
        "then it forwards to a handwritten plugin in the same pipeline" {
            val nextState = TestState.SomeData(2)
            val plugin = object : StorePlugin<TestState, TestIntent, TestAction> {
                override val name = "handwritten"

                override suspend fun PipelineContext<TestState, TestIntent, TestAction>.onIntent(
                    intent: TestIntent,
                ): TestIntent {
                    withState { this shouldBe nextState }
                    return intent
                }

                override fun equals(other: Any?): Boolean = this === other
                override fun hashCode(): Int = name.hashCode()
                override fun toString(): String = name
            }
            val decorator = testDecorator {
                onIntent { chain, intent ->
                    chain.name shouldBe plugin.name
                    updateState { nextState }
                    chain.onIntent(intent)
                }
            }
            (decorator decorates plugin).test(TestState.Some) {
                val intent = TestIntent { }
                onIntent(intent) shouldBe intent
                withState { this shouldBe nextState }
            }
        }
        "then its launched coroutines are cancelled with the pipeline" {
            lateinit var job: Job
            val decorator = testDecorator {
                onIntent { chain, intent ->
                    job = launch { awaitCancellation() }
                    chain.onIntent(intent)
                }
            }
            (timeTravelPlugin decoratedWith decorator).test(TestState.Some) {
                onStart()
                val intent = TestIntent { }
                onIntent(intent) shouldBe intent
                job.isActive shouldBe true
                closeAndWait()
                job.isCancelled shouldBe true
            }
        }
    }
    "given a shutdown decorator with implicit access to the remaining state" - {
        "then it can update that state and forward stop" {
            val nextState = TestState.SomeData(2)
            val decorator = testDecorator {
                onStop { chain, cause ->
                    updateStateImmediate { nextState }
                    chain.onStop(cause)
                }
            }
            (timeTravelPlugin decoratedWith decorator).test(TestState.Some) {
                onStart()
                onStop(null)
                state shouldBe nextState
                timeTravel.stops shouldBe 1
            }
        }
    }
    "given an outer decorator that drops an intent" - {
        "then neither the inner decorator nor the plugin sees the intent" {
            val calls = mutableListOf<String>()
            val inner = testDecorator {
                onIntent { chain, intent ->
                    calls += "inner"
                    chain.onIntent(intent)
                }
            }
            val outer = testDecorator {
                onIntent { _, _ ->
                    calls += "outer"
                    null
                }
            }
            val plugin = plugin<TestState, TestIntent, TestAction> {
                onIntent { intent ->
                    calls += "plugin"
                    intent
                }
            }
            (plugin decoratedWith listOf(inner, outer)).test(TestState.Some) {
                onIntent(TestIntent { }) shouldBe null
                calls shouldContainExactly listOf("outer")
            }
        }
    }
    "given a decorator that wraps undelivered values" - {
        "then the downstream plugin receives the original values" {
            val intents = mutableListOf<TestIntent>()
            val actions = mutableListOf<TestAction>()
            val plugin = plugin<TestState, TestIntent, TestAction> {
                onUndeliveredIntent { intents += it }
                onUndeliveredAction { actions += it }
            }
            val decorator = testDecorator {
                onUndeliveredIntent { chain, intent -> chain.onUndeliveredIntent(intent) }
                onUndeliveredAction { chain, action -> chain.onUndeliveredAction(action) }
            }
            (decorator decorates plugin).test(TestState.Some) {
                val intent = TestIntent { }
                onUndeliveredIntent(intent)
                onUndeliveredAction(TestAction.Some)
                intents shouldContainExactly listOf(intent)
                actions shouldContainExactly listOf(TestAction.Some)
            }
        }
    }
    "given decorator that wraps enqueue" - {
        val calls = mutableListOf<String>()
        val plugin = plugin<TestState, TestIntent, TestAction> {
            onIntentEnqueue { intent ->
                calls += "child:$intent"
                intent
            }
        }
        val decorator = testDecorator {
            onIntentEnqueue { chain, intent ->
                calls += "decorator:$intent"
                chain.onIntentEnqueue(intent)
            }
        }
        "then decorator invokes child" {
            (decorator decorates plugin).test(TestState.Some) {
                val intent = TestIntent { }
                onIntentEnqueue(intent)
                calls shouldContainExactly listOf("decorator:$intent", "child:$intent")
            }
        }
    }
    "given decorator that wraps action dispatch" - {
        val calls = mutableListOf<String>()
        val plugin = plugin<TestState, TestIntent, TestAction> {
            onActionDispatch { action ->
                calls += "child:$action"
                action
            }
        }
        val decorator = testDecorator {
            onActionDispatch { chain, action ->
                calls += "decorator:$action"
                chain.onActionDispatch(action)
            }
        }
        "then decorator invokes child" {
            (decorator decorates plugin).test(TestState.Some) {
                val action = TestAction.Some
                onActionDispatch(action)
                calls shouldContainExactly listOf("decorator:$action", "child:$action")
            }
        }
    }
})
