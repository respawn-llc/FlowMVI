package pro.respawn.flowmvi.sample.util

import pro.respawn.flowmvi.sample.BuildFlags
import pro.respawn.flowmvi.sample.arch.configuration.androidSampleConfiguration

internal actual val BuildFlags.debuggable: Boolean get() = androidSampleConfiguration.debuggable
internal actual val BuildFlags.platform get() = Platform.Android
