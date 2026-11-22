package pro.respawn.flowmvi.decorator

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.StorePlugin
import pro.respawn.flowmvi.api.context.ShutdownContext
import pro.respawn.flowmvi.impl.plugin.PluginInstance

/**
 * The callbacks wrapped by a decorator. Forward an event by calling its matching method.
 */
@Suppress("TooManyFunctions")
public class DecoratorChain<S : MVIState, I : MVIIntent, A : MVIAction> internal constructor(
    private val plugin: PluginInstance<S, I, A>,
) {
    /** The name of the wrapped plugin or composite, if it has one. */
    public val name: String? get() = plugin.name

    /** Forwards [StorePlugin.onStart]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onStart(): Unit = with(plugin) { ctx.onStart() }

    /** Forwards [StorePlugin.onState]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onState(old: S, new: S): S? = with(plugin) { ctx.onState(old, new) }

    /** Forwards [StorePlugin.onIntent]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onIntent(intent: I): I? = with(plugin) { ctx.onIntent(intent) }

    /** Forwards [StorePlugin.onIntentEnqueue]. */
    public fun onIntentEnqueue(intent: I): I? = plugin.onIntentEnqueue(intent)

    /** Forwards [StorePlugin.onAction]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onAction(action: A): A? = with(plugin) { ctx.onAction(action) }

    /** Forwards [StorePlugin.onActionDispatch]. */
    public fun onActionDispatch(action: A): A? = plugin.onActionDispatch(action)

    /** Forwards [StorePlugin.onException]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onException(e: Exception): Exception? = with(plugin) { ctx.onException(e) }

    /** Forwards [StorePlugin.onSubscribe]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onSubscribe(newSubscriberCount: Int): Unit = with(plugin) { ctx.onSubscribe(newSubscriberCount) }

    /** Forwards [StorePlugin.onUnsubscribe]. */
    context(ctx: PipelineContext<S, I, A>)
    public suspend fun onUnsubscribe(newSubscriberCount: Int): Unit = with(plugin) {
        ctx.onUnsubscribe(newSubscriberCount)
    }

    /** Forwards [StorePlugin.onStop]. */
    context(ctx: ShutdownContext<S, I, A>)
    public fun onStop(e: Exception?): Unit = with(plugin) { ctx.onStop(e) }

    /** Forwards [StorePlugin.onUndeliveredIntent]. */
    context(ctx: ShutdownContext<S, I, A>)
    public fun onUndeliveredIntent(intent: I): Unit = with(plugin) { ctx.onUndeliveredIntent(intent) }

    /** Forwards [StorePlugin.onUndeliveredAction]. */
    context(ctx: ShutdownContext<S, I, A>)
    public fun onUndeliveredAction(action: A): Unit = with(plugin) { ctx.onUndeliveredAction(action) }
}
