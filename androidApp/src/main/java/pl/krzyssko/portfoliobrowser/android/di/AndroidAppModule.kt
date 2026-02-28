package pl.krzyssko.portfoliobrowser.android.di

import androidx.lifecycle.SavedStateHandle
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectsListViewModel
import pl.krzyssko.portfoliobrowser.di.NAMED_FIRESTORE
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getConfiguration

val androidAppModule = module {
    viewModel { (handle: SavedStateHandle) ->
        ProjectsListViewModel(handle, get(qualifier = NAMED_FIRESTORE), get())
    }
    viewModel { (handle: SavedStateHandle) ->
        ProjectDetailsViewModel(handle, get(qualifier = NAMED_FIRESTORE), get(), get())
    }
    viewModel {
        ProfileViewModel(get(qualifier = NAMED_GITHUB), get(), get(), get(), get())
    }
    single<Configuration> { getConfiguration(androidContext()) }
}
