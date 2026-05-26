package pl.krzyssko.portfoliobrowser.android.di

import androidx.lifecycle.SavedStateHandle
import com.liftric.kvault.KVault
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.di.NAMED_AZURE
import pl.krzyssko.portfoliobrowser.di.NAMED_FIRESTORE
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.VaultProvider
import pl.krzyssko.portfoliobrowser.platform.getConfiguration

val androidAppModule = module {
    viewModel { (handle: SavedStateHandle) ->
        ProjectViewModel(handle, get(qualifier = NAMED_FIRESTORE), get(qualifier = NAMED_AZURE), get())
    }
    viewModel {
        ProfileViewModel()
    }
    single<VaultProvider> {
        object : VaultProvider {
            override val vault: KVault
                get() = KVault(androidContext(), "private_config")
        }
    }
    single<Configuration> { getConfiguration(get()) }
}
