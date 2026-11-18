package pro.respawn.flowmvi.sample.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import pro.respawn.flowmvi.android.ContainerViewModel
import pro.respawn.flowmvi.sample.BuildConfig
import pro.respawn.flowmvi.sample.arch.configuration.AndroidSampleConfiguration
import pro.respawn.flowmvi.sample.arch.configuration.androidSampleDebugger
import pro.respawn.flowmvi.sample.features.xmlactivity.XmlActivityContainer
import pro.respawn.flowmvi.sample.navigation.AndroidFeatureLauncher
import pro.respawn.flowmvi.sample.platform.PlatformFeatureLauncher

private val androidAppModule = module {
    single { AndroidSampleConfiguration(BuildConfig.DEBUG, androidSampleDebugger) }
    singleOf(::AndroidFeatureLauncher) bind PlatformFeatureLauncher::class
    factoryOf(::XmlActivityContainer)
    viewModel(qualifier<XmlActivityContainer>()) { ContainerViewModel(get<XmlActivityContainer>()) }
}

internal fun Application.startKoin() = startKoin(modules = listOf(androidAppModule)) configure@{
    androidContext(this@startKoin)
}
