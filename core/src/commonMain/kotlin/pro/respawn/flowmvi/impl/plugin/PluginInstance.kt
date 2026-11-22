package pro.respawn.flowmvi.impl.plugin

import pro.respawn.flowmvi.annotation.NotIntendedForInheritance
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.StorePlugin
import pro.respawn.flowmvi.api.context.ShutdownContext

/**
 * Optimized plugin implementation that stores optional lambdas directly and avoids their invocations
 */
@OptIn(NotIntendedForInheritance::class)
internal data class PluginInstance<S : MVIState, I : MVIIntent, A : MVIAction>(
    val onState: (suspend PipelineContext<S, I, A>.(old: S, new: S) -> S?)? = null,
    val onIntentEnqueue: ((intent: I) -> I?)? = null,
    val onIntent: (suspend PipelineContext<S, I, A>.(intent: I) -> I?)? = null,
    val onAction: (suspend PipelineContext<S, I, A>.(action: A) -> A?)? = null,
    val onActionDispatch: ((action: A) -> A?)? = null,
    val onException: (suspend PipelineContext<S, I, A>.(e: Exception) -> Exception?)? = null,
    val onStart: (suspend PipelineContext<S, I, A>.() -> Unit)? = null,
    val onSubscribe: (suspend PipelineContext<S, I, A>.(subscriberCount: Int) -> Unit)? = null,
    val onUnsubscribe: (suspend PipelineContext<S, I, A>.(subscriberCount: Int) -> Unit)? = null,
    val onStop: (ShutdownContext<S, I, A>.(e: Exception?) -> Unit)? = null,
    val onUndeliveredIntent: (ShutdownContext<S, I, A>.(intent: I) -> Unit)? = null,
    val onUndeliveredAction: (ShutdownContext<S, I, A>.(action: A) -> Unit)? = null,
    override val name: String? = null,
) : StorePlugin<S, I, A> {

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onState(old: S, new: S): S? {
        return (onState ?: return new).invoke(ctx, old, new)
    }

    override fun onIntentEnqueue(intent: I): I? {
        return (onIntentEnqueue ?: return intent).invoke(intent)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onIntent(intent: I): I? {
        return (onIntent ?: return intent).invoke(ctx, intent)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onAction(action: A): A? {
        return (onAction ?: return action).invoke(ctx, action)
    }

    override fun onActionDispatch(action: A): A? {
        return (onActionDispatch ?: return action).invoke(action)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onException(e: Exception): Exception? {
        return (onException ?: return e).invoke(ctx, e)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onStart() {
        onStart?.invoke(ctx)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onSubscribe(newSubscriberCount: Int) {
        onSubscribe?.invoke(ctx, newSubscriberCount)
    }

    context(ctx: PipelineContext<S, I, A>)
    override suspend fun onUnsubscribe(newSubscriberCount: Int) {
        onUnsubscribe?.invoke(ctx, newSubscriberCount)
    }

    context(ctx: ShutdownContext<S, I, A>)
    override fun onStop(e: Exception?) {
        onStop?.invoke(ctx, e)
    }

    context(ctx: ShutdownContext<S, I, A>)
    override fun onUndeliveredIntent(intent: I) {
        onUndeliveredIntent?.invoke(ctx, intent)
    }

    context(ctx: ShutdownContext<S, I, A>)
    override fun onUndeliveredAction(action: A) {
        onUndeliveredAction?.invoke(ctx, action)
    }

    override fun toString(): String = "StorePlugin${name?.let { " \"$it\"" }.orEmpty()}"
    override fun hashCode(): Int = name?.hashCode() ?: super.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is StorePlugin<*, *, *> -> false
        other.name == null && name == null -> this === other
        else -> name == other.name
    }
}
