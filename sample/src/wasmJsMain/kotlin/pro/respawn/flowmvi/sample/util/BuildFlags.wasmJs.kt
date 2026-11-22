package pro.respawn.flowmvi.sample.util

import pro.respawn.flowmvi.sample.BuildFlags

internal actual val BuildFlags.debuggable: Boolean get() = isDevelopmentBuild()

private fun isDevelopmentBuild(): Boolean = js("process.env.NODE_ENV === 'development'")
internal actual val BuildFlags.platform: Platform get() = Platform.Web
