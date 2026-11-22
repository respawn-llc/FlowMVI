package pro.respawn.flowmvi.debugger.server.navigation.util

import com.arkivanov.essenty.instancekeeper.InstanceKeeperOwner
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import pro.respawn.flowmvi.debugger.server.navigation.destination.Destination
import pro.respawn.kmmutils.common.fastLazy

internal infix fun Destination.duplicateOf(
    other: Destination
) = if (singleTop) this::class == other::class else this == other

internal inline fun <T> InstanceKeeperOwner.retained(
    key: String,
    crossinline block: () -> T
) = fastLazy { instanceKeeper.getOrCreateSimple(key = key) { block() } }

private val humps by fastLazy { "(?<=.)(?=\\p{Upper})".toRegex() }

internal fun String.toSnakeCase() = replace(humps, "_").lowercase()
