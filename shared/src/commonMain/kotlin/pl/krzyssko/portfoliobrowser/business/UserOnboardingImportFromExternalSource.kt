package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.di.NAMED_ONBOARDING_IMPORT
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.ProjectsImportState
import pl.krzyssko.portfoliobrowser.store.checkImport
import pl.krzyssko.portfoliobrowser.store.importProjects
import pl.krzyssko.portfoliobrowser.store.openImport

class UserOnboardingImportFromExternalSource(
    coroutineScope: CoroutineScope,
    profileStateFlow: StateFlow<ProfileState>,
    private val auth: Auth,
    private val db: Firestore
) : KoinComponent {
    private val store: OrbitStore<ProjectsImportState> by inject(NAMED_ONBOARDING_IMPORT) {
        parametersOf(
            coroutineScope,
            ProjectsImportState.Initialized
        )
    }

    val stateFlow = store.stateFlow
    val sideEffectsFlow = store.sideEffectFlow

    private val sourceRepository: ProjectRepository by inject(NAMED_GITHUB)

    init {
        //coroutineScope.launch {
        //    profileStateFlow.collect {
        //        if (it is ProfileState.Authenticated) {
        //            if (auth.hasGitHubProvider) {
        //                //checkImport()
        //            }
        //        }
        //    }
        //}
    }

    fun checkImport() {
        store.checkImport(auth, db)
    }

    fun openImport() {
        store.openImport()
    }

    fun startImportFromSource(uiHandler: Any?) {
        store.importProjects(sourceRepository, db, auth, uiHandler)
    }
}