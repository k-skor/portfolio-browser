package pl.krzyssko.portfoliobrowser.android.di

import androidx.lifecycle.SavedStateHandle
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getConfiguration

val androidAppModule = module {
    viewModel { (handle: SavedStateHandle) ->
        ProjectViewModel(handle, get(), get(), get())
    }
    viewModel { (handle: SavedStateHandle) ->
        ProjectDetailsViewModel(handle, get())
    }
    viewModel {
        ProfileViewModel(get(), get(), get(), get(), get())
    }
    single<Configuration> { getConfiguration(androidContext()) }
}