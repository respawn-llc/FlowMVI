@file:pro.respawn.flowmvi.annotation.MustUseReturnValues

package pro.respawn.flowmvi.compose.dsl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import pro.respawn.flowmvi.api.SubscriberLifecycle

/**
 * A local composition [SubscriberLifecycle] instance. May return `null` if no lifecycle was provided.
 * Can be provided with [ProvideSubscriberLifecycle].
 */
public val LocalSubscriberLifecycle: ProvidableCompositionLocal<SubscriberLifecycle?> = staticCompositionLocalOf {
    null
}

/** Provides [LocalSubscriberLifecycle] with an instance of [lifecycle] for child [content].
 *
 */
@Composable
public fun ProvideSubscriberLifecycle(
    lifecycle: SubscriberLifecycle,
    content: @Composable () -> Unit
): Unit = CompositionLocalProvider(
    LocalSubscriberLifecycle provides lifecycle,
    content = content,
)

/**
 * Remember a new subscriber lifecycle instance from [delegate] to convert it using [factory]
 */
@Composable
public fun <T> rememberSubscriberLifecycle(
    delegate: T,
    factory: T.() -> SubscriberLifecycle
): SubscriberLifecycle = remember(delegate) { delegate.factory() }

/**
 * Get the current provided subscriber lifecycle, or if not found, fall back to the platform-provided lifecycle
 */
public val DefaultLifecycle: SubscriberLifecycle
    @Composable
    get() = LocalSubscriberLifecycle.current
        ?: rememberSubscriberLifecycle(LocalLifecycleOwner.current) { lifecycle.asSubscriberLifecycle }

/**
 * Require [LocalSubscriberLifecycle] to be provided using a composition local.
 *
 * For a fallback behavior, use [DefaultLifecycle].
 */
@Composable
public fun requireLifecycle(): SubscriberLifecycle = requireNotNull(LocalSubscriberLifecycle.current) {
    "Subscriber lifecycle was required but not found. " +
        "Please either provide a lifecycle using LocalSubscriberLifecycle, pass the lifecycle manually, " +
        "or use DefaultLifecycle to fall back to system lifecycle."
}
