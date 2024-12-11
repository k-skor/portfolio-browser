package pl.krzyssko.portfoliobrowser.android.di

import androidx.lifecycle.SavedStateHandle
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel

val androidAppModule = module {
    viewModel { (handle: SavedStateHandle) ->
        ProjectViewModel(handle, get(), get(), get())
    }
    viewModel { (handle: SavedStateHandle) ->
        ProjectDetailsViewModel(handle, get())
    }
}