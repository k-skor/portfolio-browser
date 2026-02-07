package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserOnboardingImportState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

abstract class ImportSource(val source: Source) {
    abstract val totalItems: Flow<Int>
    abstract val importFlow: Flow<Project>
}

class GitHubSource(repository: ProjectRepository) : ImportSource(Source.GitHub) {

    override var totalItems: Flow<Int> = flow {
        emit(repository.fetchTotalProjectsSize().getOrThrow())
    }

    override val importFlow = flow {
        var isLastPage = false
        while (!isLastPage && currentCoroutineContext().isActive) {
            val page = repository.nextPage(repository.pagingState.paging.nextPageKey)
            val projects = page.getOrThrow()
            projects.forEach { project ->
                val stack = repository.fetchStack(project.name)
                emit(project.copy(stack = stack.getOrThrow()))
            }
            isLastPage = repository.pagingState.paging.isLastPage || projects.isEmpty()
        }
    }
}

class UserOnboardingProjectsImport(
    coroutineScope: CoroutineScope,
    dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<UserOnboardingImportState>(coroutineScope, dispatcherIO, UserOnboardingImportState.Initialized) {

    private val repository: ProjectRepository by inject(NAMED_GITHUB)
    private val projectsList: MutableList<ProjectDto> = mutableListOf()
    private var importJob: Job? = null
    private var saveJob: Job? = null

    fun start() = intent {
        if (!auth.isUserSignedIn) {
            return@intent
        }
        val hasTimestamp = withContext(dispatcherIO) {
            firestore.getLastSyncTimestampForSource(auth.userAccount!!.id, Source.GitHub) != null
        }
        if (!hasTimestamp) {
            reduce {
                UserOnboardingImportState.SourceAvailable
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.ProviderImport))
        }
    }

    fun confirm() = intent {
        reduce {
            UserOnboardingImportState.ImportConfirmed
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Login(ViewType.SourceSelection)))
    }

    fun import(uiHandler: Any?, source: Source) = intent {
        postSideEffect(UserSideEffects.NavigateTo(Route.ProviderImportOngoing))
        importProjects(uiHandler)
        importJob?.join()
        if (state !is UserOnboardingImportState.ImportError) {
            saveProjects(projectsList, source)
        }
    }

    fun cancel() = intent {
        importJob?.cancel()
        saveJob?.join()
        reduce {
            UserOnboardingImportState.ImportCompleted
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun importProjects(uiHandler: Any?) = subIntent {
        with(GitHubSource(repository)) {
            importJob = importFlow
                .onStart {
                    reduce {
                        UserOnboardingImportState.ImportStarted
                    }
                    auth.startSignInFlow(
                        uiHandler = uiHandler,
                        providerType = Auth.LoginMethod.GitHub,
                        refresh = true
                    )
                    projectsList.clear()
                    totalItems
                        .onEach {
                            reduce {
                                UserOnboardingImportState.ImportProgress(0, it, null)
                            }
                        }
                        .collect()
                }
                .map { it.toDto() }
                .onEach {
                    projectsList += it
                    reduce {
                        (state as UserOnboardingImportState.ImportProgress).copy(
                            progress = projectsList.size,
                            displayName = it.name
                        )
                    }
                }
                .catch {
                    reduce {
                        UserOnboardingImportState.ImportError(it)
                    }
                    postSideEffect(UserSideEffects.NavigateTo(it.toRoute()))
                    postSideEffect(UserSideEffects.Error(it))
                }
                .flowOn(dispatcherIO)
                .launchIn(coroutineScope)
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun saveProjects(projectsList: List<ProjectDto>, source: Source) = subIntent {
        val result = runCatching {
            saveJob = coroutineScope.launch(dispatcherIO) {
                val userId = auth.userAccount!!.id
                firestore.syncProjects(
                    userId,
                    projectsList,
                    source
                )
            }
        }
        when {
            result.isSuccess -> {
                reduce {
                    UserOnboardingImportState.ImportCompleted
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.List))
            }
            result.isFailure -> {
                reduce {
                    UserOnboardingImportState.ImportError(result.exceptionOrNull())
                }
            }
        }
    }
}
