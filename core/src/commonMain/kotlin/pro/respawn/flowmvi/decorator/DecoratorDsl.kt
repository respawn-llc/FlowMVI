@file:pro.respawn.flowmvi.annotation.MustUseReturnValues

package pro.respawn.flowmvi.decorator

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.StorePlugin
import pro.respawn.flowmvi.api.context.ShutdownContext
import pro.respawn.flowmvi.impl.plugin.PluginInstance
import pro.respawn.flowmvi.impl.plugin.asInstance

public typealias DecorateValue<S, I, A, V> = (
suspend PipelineContext<S, I, A>.(chain: DecoratorChain<S, I, A>, it: V) -> V?
)

public typealias DecorateValueNonSuspend<S, I, A, V> = (chain: DecoratorChain<S, I, A>, it: V) -> V?

public typealias DecorateState<S, I, A> = (
suspend PipelineContext<S, I, A>.(chain: DecoratorChain<S, I, A>, old: S, new: S) -> S?
)

public typealias Decorate<S, I, A> = (
suspend PipelineContext<S, I, A>.(chain: DecoratorChain<S, I, A>) -> Unit
)

public typealias DecorateArg<S, I, A, V> = (
suspend PipelineContext<S, I, A>.(chain: DecoratorChain<S, I, A>, it: V) -> Unit
)

public typealias DecorateShutdown<S, I, A, V> = (
ShutdownContext<S, I, A>.(chain: DecoratorChain<S, I, A>, e: V) -> Unit
)

/**
 * Build a new [PluginDecorator].
 *
 * Consult the [PluginDecorator] and [DecoratorBuilder] docs for more info.
 */
public inline fun <S : MVIState, I : MVIIntent, A : MVIAction> decorator(
    block: DecoratorBuilder<S, I, A>.() -> Unit
): PluginDecorator<S, I, A> = DecoratorBuilder<S, I, A>().apply(block).build()

/**
 * Return a new [StorePlugin] that decorates `this` plugin using [decorator]
 */
public infix fun <S : MVIState, I : MVIIntent, A : MVIAction> StorePlugin<S, I, A>.decoratedWith(
    decorator: PluginDecorator<S, I, A>
): StorePlugin<S, I, A> = asInstance().decorate(decorator)

/**
 * Returns a new [StorePlugin] with `this` [PluginDecorator] applied to the [plugin].
 */
public infix fun <S : MVIState, I : MVIIntent, A : MVIAction> PluginDecorator<S, I, A>.decorates(
    plugin: StorePlugin<S, I, A>
): StorePlugin<S, I, A> = plugin.asInstance().decorate(this)

/**
 * Return a new [StorePlugin] that decorates `this` plugin using all [decorators].
 *
 * Decorators are wrapped in the order of iteration: `D_N( ...D1( D0( Plugin ) )... )`
 */
public infix fun <S : MVIState, I : MVIIntent, A : MVIAction> StorePlugin<S, I, A>.decoratedWith(
    decorators: Iterable<PluginDecorator<S, I, A>>
): StorePlugin<S, I, A> = decorators.fold(this) { next, decorator -> next decoratedWith decorator }
internal infix fun <S : MVIState, I : MVIIntent, A : MVIAction> PluginInstance<S, I, A>.decorate(
    decorator: PluginDecorator<S, I, A>,
): PluginInstance<S, I, A> {
    val chain = DecoratorChain(this)
    return copy(
        name = decorator.name,
        onState = wrapNotNull(onState, decorator.onState) { wrap ->
            { old, new -> wrap(chain, old, new) }
        },
        onIntent = wrapNotNull(onIntent, decorator.onIntent) { wrap ->
            { wrap(chain, it) }
        },
        onIntentEnqueue = wrapNotNull(onIntentEnqueue, decorator.onIntentEnqueue) { wrap ->
            { intent ->
                wrap(chain, intent)
            }
        },
        onAction = wrapNotNull(onAction, decorator.onAction) { wrap ->
            { wrap(chain, it) }
        },
        onActionDispatch = wrapNotNull(onActionDispatch, decorator.onActionDispatch) { wrap ->
            { action ->
                wrap(chain, action)
            }
        },
        onException = wrapNotNull(onException, decorator.onException) { wrap ->
            { wrap(chain, it) }
        },
        onStart = wrapNotNull(onStart, decorator.onStart) { wrap ->
            { wrap(chain) }
        },
        onSubscribe = wrapNotNull(onSubscribe, decorator.onSubscribe) { wrap ->
            { wrap(chain, it) }
        },
        onUnsubscribe = wrapNotNull(onUnsubscribe, decorator.onUnsubscribe) { wrap ->
            { wrap(chain, it) }
        },
        onStop = wrapNotNull(onStop, decorator.onStop) { wrap ->
            { wrap(chain, it) }
        },
        onUndeliveredIntent = wrapNotNull(onUndeliveredIntent, decorator.onUndeliveredIntent) { wrap ->
            { wrap(chain, it) }
        },
        onUndeliveredAction = wrapNotNull(onUndeliveredAction, decorator.onUndeliveredAction) { wrap ->
            { wrap(chain, it) }
        }
    )
}

private inline fun <H, W> wrapNotNull(
    action: H?,
    wrapper: W?,
    transform: (block: W) -> H
): H? = wrapper?.let(transform) ?: action
