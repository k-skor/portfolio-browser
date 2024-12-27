package pl.krzyssko.portfoliobrowser.android.di

import androidx.lifecycle.SavedStateHandle
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel

val androidAppModule = module {
    viewModel { (handle: SavedStateHandle) ->
        ProjectViewModel(handle, get())
    }
    viewModel { (handle: SavedStateHandle) ->
        ProjectDetailsViewModel(handle, get())
    }
    viewModel {
        ProfileViewModel(get(), get(), get(), get(), get())
    }
}