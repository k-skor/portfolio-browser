package pl.krzyssko.portfoliobrowser.android.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel

val androidAppModule = module {
    viewModel {
        ProjectViewModel(get(), get())
    }
    viewModel {
        ProjectDetailsViewModel(get())
    }
}