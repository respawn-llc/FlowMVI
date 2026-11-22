# FlowMVI plugin callbacks (signatures + behavior)

Use this file for exact callback signatures and rules. These are the full API surface for plugin/decorator interception.

## StorePlugin callbacks

```kotlin
public interface StorePlugin<S : MVIState, I : MVIIntent, A : MVIAction> : LazyPlugin<S, I, A> {
    public val name: String?

    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onState(old: S, new: S): S? = new
    public fun onIntentEnqueue(intent: I): I? = intent
    public fun onActionDispatch(action: A): A? = action
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onIntent(intent: I): I? = intent
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onAction(action: A): A? = action
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onException(e: Exception): Exception? = e
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onStart(): Unit = Unit
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onSubscribe(newSubscriberCount: Int): Unit = Unit
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onUnsubscribe(newSubscriberCount: Int): Unit = Unit
    context(ctx: ShutdownContext<S, I, A>)
    public fun onStop(e: Exception?): Unit = Unit
    context(ctx: ShutdownContext<S, I, A>)
    public fun onUndeliveredIntent(intent: I): Unit = Unit
    context(ctx: ShutdownContext<S, I, A>)
    public fun onUndeliveredAction(action: A): Unit = Unit
}
```

### Callback quick guide (1–3 sentences each)

- `onState(old, new)`
  - Runs after `updateState` computes a new value, before it is committed. Return `null` to cancel, `old` to veto but continue, or `new` (or modified) to apply. Not invoked for `updateStateImmediate`.
- `onIntentEnqueue(intent)`
  - Runs **before** an intent is buffered. Return `null` to drop or replace to transform. Exceptions bypass `onException` and bubble to the caller.
- `onActionDispatch(action)`
  - Runs after an action is dequeued and before delivery. Return `null` to drop or replace to transform. Exceptions bypass `onException` and bubble to the caller.
- `onIntent(intent)`
  - Runs when intent starts processing. Return `null` to consume and stop the chain, return a different intent to replace. Useful for analytics, intent rewriting, or early rejection.
- `onAction(action)`
  - Runs when an action is sent and **before** subscribers receive it. Return `null` to veto or replace to transform. If you drop here, `onActionDispatch` will not run.
- `onException(e)`
  - Runs asynchronously after a job throws and is already canceled. Return `null` to swallow or return `e` to propagate. Do not throw here.
- `onStart()`
  - Runs each time the store starts. Suspending here delays startup and blocks other plugin `onStart` chains and `onSubscribe`.
- `onSubscribe(count)`
  - Runs after subscriber count increments; `count` is the **new** count. Runs in store scope, not subscriber scope.
- `onUnsubscribe(count)`
  - Runs after subscriber count decrements; `count` is the **new** count. Also runs in store scope.
- `onStop(e)`
  - Runs after store is closed; cannot affect outcome. Must be fast and non-throwing and treats `e` as the close cause.
- `onUndeliveredIntent(intent)`
  - Runs when an intent is dropped (overflow, stop, or exception). Runs on a random thread/context and must never throw.
- `onUndeliveredAction(action)`
  - Runs when an action is dropped (overflow or stop). Runs on a random thread/context and must never throw.

## StorePluginBuilder callback setters (DSL)

```kotlin
public open class StorePluginBuilder<S : MVIState, I : MVIIntent, A : MVIAction> {
    public var name: String?
    public fun onStart(block: suspend PipelineContext<S, I, A>.() -> Unit): Unit
    public fun onState(block: suspend PipelineContext<S, I, A>.(old: S, new: S) -> S?): Unit
    public fun onIntentEnqueue(block: (intent: I) -> I?): Unit
    public fun onIntent(block: suspend PipelineContext<S, I, A>.(intent: I) -> I?): Unit
    public fun onAction(block: suspend PipelineContext<S, I, A>.(action: A) -> A?): Unit
    public fun onActionDispatch(block: (action: A) -> A?): Unit
    public fun onException(block: suspend PipelineContext<S, I, A>.(e: Exception) -> Exception?): Unit
    public fun onSubscribe(block: suspend PipelineContext<S, I, A>.(newSubscriberCount: Int) -> Unit): Unit
    public fun onUnsubscribe(block: suspend PipelineContext<S, I, A>.(subscriberCount: Int) -> Unit): Unit
    public fun onStop(block: ShutdownContext<S, I, A>.(e: Exception?) -> Unit): Unit
    public fun onUndeliveredIntent(block: ShutdownContext<S, I, A>.(intent: I) -> Unit): Unit
    public fun onUndeliveredAction(block: ShutdownContext<S, I, A>.(action: A) -> Unit): Unit
}
```

## Decorator callbacks and types

Decorators wrap plugins and can **short-circuit the chain** if they do not call child callbacks.

```kotlin
public typealias DecorateValue<S, I, A, V> = (
    suspend PipelineContext<S, I, A>.(child: StorePlugin<S, I, A>, it: V) -> V?
)

public typealias DecorateValueNonSuspend<S, I, A, V> = (
    child: StorePlugin<S, I, A>, it: V
) -> V?

public typealias DecorateState<S, I, A> = (
    suspend PipelineContext<S, I, A>.(child: StorePlugin<S, I, A>, old: S, new: S) -> S?
)

public typealias Decorate<S, I, A> = (
    suspend PipelineContext<S, I, A>.(child: StorePlugin<S, I, A>) -> Unit
)

public typealias DecorateArg<S, I, A, V> = (
    suspend PipelineContext<S, I, A>.(child: StorePlugin<S, I, A>, it: V) -> Unit
)

public typealias DecorateShutdown<S, I, A, V> = (
    ShutdownContext<S, I, A>.(child: StorePlugin<S, I, A>, e: V) -> Unit
)
```

### DecoratorBuilder callbacks

```kotlin
public class DecoratorBuilder<S : MVIState, I : MVIIntent, A : MVIAction> {
    public var name: String?
    public fun onStart(block: Decorate<S, I, A>): Unit
    public fun onIntentEnqueue(block: DecorateValueNonSuspend<S, I, A, I>): Unit
    public fun onIntent(block: DecorateValue<S, I, A, I>): Unit
    public fun onState(block: DecorateState<S, I, A>): Unit
    public fun onAction(block: DecorateValue<S, I, A, A>): Unit
    public fun onActionDispatch(block: DecorateValueNonSuspend<S, I, A, A>): Unit
    public fun onException(block: DecorateValue<S, I, A, Exception>): Unit
    public fun onSubscribe(block: DecorateArg<S, I, A, Int>): Unit
    public fun onUnsubscribe(block: DecorateArg<S, I, A, Int>): Unit
    public fun onStop(block: DecorateShutdown<S, I, A, Exception?>): Unit
    public fun onUndeliveredIntent(block: DecorateShutdown<S, I, A, I>): Unit
    public fun onUndeliveredAction(block: DecorateShutdown<S, I, A, A>): Unit
}
```

### Decorator semantics (short)

- Decorators wrap a plugin (or the whole store as a composite plugin).
- If you do **not** call `child.onX(...)` inside the decorator, the chain ends there.
- Decorators are installed **after** plugins and wrap all previously installed plugins.
- Return values are **final** unless wrapped by another decorator.

### Decorator example (callback forwarding)

Call `child.onIntent(intent)` to forward the intent.

```kotlin
@FlowMVIDSL
fun <S : MVIState, I : MVIIntent, A : MVIAction> debounceIntentsDecorator(
    timeout: Duration,
    name: String? = "DebounceIntents",
): PluginDecorator<S, I, A> = decorator {
    this.name = name
    val handle = JobHandle()

    onIntent { child, intent ->
        handle.job?.cancelAndJoin()
        val job = launch {
            delay(timeout)
            child.onIntent(intent)
        }
        handle.set(job)
        null
    }

    onStop { child, e ->
        handle.set(null)
        child.onStop(e)
    }
}
```

