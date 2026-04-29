package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Profile.Companion.DefaultProfile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.di.NAMED_GITHUB
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.navigation.toRoute
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OnboardingState
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

interface ImportSource {
    suspend fun totalItems(): Int
    fun importFlow(): Flow<Project>
}

class GitHubSource(private val repository: ProjectRepository, categoriesRepository: CategoriesRepository) : ImportSource {

    override suspend fun totalItems() = repository.fetchTotalProjectsSize().getOrThrow()

    override fun importFlow() = flow {
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

class Onboarding(
    coroutineScope: CoroutineScope,
    private val dispatcherIO: CoroutineDispatcher,
    private val auth: Auth,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<OnboardingState>(coroutineScope, OnboardingState.Initialized) {
    private val repository: ProjectRepository by inject(NAMED_GITHUB)
    private val categoriesRepository: CategoriesRepository by inject(NAMED_GITHUB)
    private val projectsList: MutableList<ProjectDto> = mutableListOf()
    private var checkJob: Job? = null
    private var importJob: Job? = null
    private var saveJob: Job? = null

    init {
        stateFlow.onEach {
            when (it) {
                is OnboardingState.ProfileNotCreated -> {
                    check()
                }

                is OnboardingState.FirstTimeSignUp -> {
                    create()
                }

                is OnboardingState.FirstTimeSignUpCompleted,
                is OnboardingState.ProfileExists -> {
                    start()
                }

                //is OnboardingState.OnboardingCompleted -> {
                //    fetch(auth.userAccount!!)
                //}

                else -> {}
            }
        }
            .launchIn(coroutineScope)
    }

    fun initialize() = intent {
        reduce {
            OnboardingState.ProfileNotCreated
        }
    }

    fun check() = intent {
        checkJob?.cancel()
        //val userId = user.account.id
        checkJob = coroutineScope {
            val hasUser = auth.userAccount?.id?.let {
                profileExists(it).await()
            } ?: false
            launch {
                if (!hasUser) {
                    reduce {
                        OnboardingState.FirstTimeSignUp
                    }
                    postSideEffect(UserSideEffects.NavigateTo(Route.PrepareProfile))
                } else {
                    reduce {
                        OnboardingState.ProfileExists
                    }
                }
            }
        }
    }

    fun create() = intent {
        //val userId = user.account.id
        val userId = auth.userAccount?.id.orEmpty()
        //val alias = user.identityProviders.firstNotNullOfOrNull { it.name }
        //val avatarUrl = user.identityProviders.firstNotNullOfOrNull { it.photoUrl }
        val alias = auth.providerData?.firstNotNullOfOrNull { it.name }
        val avatarUrl = auth.providerData?.firstNotNullOfOrNull { it.photoUrl }
        val hasUser = auth.userAccount?.id?.let {
            profileExists(it).await()
        } ?: false
        if (!hasUser) {
            val profile = DefaultProfile.copy(
                alias = alias,
                avatarUrl = avatarUrl
            )
            val result = runCatching {
                withContext(dispatcherIO) {
                    firestore.createProfile(userId, profile.toDto())
                }
            }
            when {
                result.isSuccess -> {
                    reduce {
                        OnboardingState.FirstTimeSignUpCompleted(userName = profile.alias ?: "${profile.firstName} ${profile.lastName}")
                    }
                }
                else -> {
                    reduce {
                        OnboardingState.Error(result.exceptionOrNull())
                    }
                }
            }
        }
    }

    private suspend fun profileExists(userId: String): Deferred<Boolean> {
        return withContext(dispatcherIO) {
            async {
                firestore.hasUser(userId)
            }
        }
    }

    fun start() = intent {
        if (!auth.isUserSignedIn) {
            return@intent
        }
        val hasTimestamp = withContext(dispatcherIO) {
            firestore.getLastSyncTimestampForSource(auth.userAccount!!.id, Source.GitHub) != null
        }
        if (!hasTimestamp) {
            reduce {
                OnboardingState.ImportSourceAvailable
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.ProviderImport))
        } else {
            reduce {
                OnboardingState.OnboardingCompleted
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.List))
        }
    }

    fun confirm() = intent {
        reduce {
            OnboardingState.ImportConfirmed
        }
        postSideEffect(UserSideEffects.NavigateTo(Route.Login(ViewType.SourceSelection)))
    }

    //fun import(uiHandler: Any?, source: Source) = intent {
    //    postSideEffect(UserSideEffects.NavigateTo(Route.ProviderImportOngoing))
    //    importProjects(uiHandler)
    //    importJob?.join()
    //    if (state !is OnboardingState.Error) {
    //        saveProjects(projectsList, source)
    //    }
    //}

    fun cancel() = intent {
        saveJob?.join()
        importJob?.cancel()
        reduce {
            OnboardingState.ProfileNotCreated
        }
    }

    @OptIn(OrbitExperimental::class, ExperimentalCoroutinesApi::class)
    fun import(uiHandler: Any?, source: Source) = intent {
        val sourceProvider: ImportSource = when (source) {
            Source.GitHub -> GitHubSource(repository, categoriesRepository)
        }

        importJob = coroutineScope {
            launch {
                with(sourceProvider) {
                    flow {
                        emit(totalItems())
                    }
                        .onStart {
                            auth.startSignInFlow(
                                uiHandler = uiHandler,
                                providerType = Auth.LoginMethod.GitHub,
                                refresh = true
                            )
                            reduce {
                                OnboardingState.ImportStarted
                            }
                        }
                        .flowOn(dispatcherIO)
                        .onEach {
                            reduce {
                                OnboardingState.ImportProgress(0, it, null)
                            }
                        }
                        .flatMapLatest {
                            importFlow()
                                .flowOn(dispatcherIO)
                                .onStart {
                                    projectsList.clear()
                                }
                                .map { it.toDto() }
                                .onEach {
                                    projectsList += it
                                    reduce {
                                        (state as OnboardingState.ImportProgress).copy(
                                            progress = projectsList.size,
                                            displayName = it.name
                                        )
                                    }
                                }
                        }
                        .catch {
                            reduce {
                                OnboardingState.Error(it)
                            }
                            postSideEffect(UserSideEffects.NavigateTo(it.toRoute()))
                            postSideEffect(UserSideEffects.Error(it))
                        }
                        .onCompletion {
                            if (it == null) {
                                saveJob = coroutineScope {
                                    launch {
                                        save(projectsList, Source.GitHub)
                                    }
                                }
                            }
                            importJob = null
                            saveJob = null
                        }
                        .collect()
            }
        }
            //importJob = importFlow()
            //    .onStart {
            //        reduce {
            //            OnboardingState.ImportStarted
            //        }
            //        auth.startSignInFlow(
            //            uiHandler = uiHandler,
            //            providerType = Auth.LoginMethod.GitHub,
            //            refresh = true
            //        )
            //        projectsList.clear()
            //        totalItems()
            //            .onEach {
            //                reduce {
            //                    OnboardingState.ImportProgress(0, it, null)
            //                }
            //            }
            //            .collect()
            //    }
            //    .map { it.toDto() }
            //    .onEach {
            //        projectsList += it
            //        reduce {
            //            (state as OnboardingState.ImportProgress).copy(
            //                progress = projectsList.size,
            //                displayName = it.name
            //            )
            //        }
            //    }
            //    .catch {
            //        reduce {
            //            OnboardingState.Error(it)
            //        }
            //        postSideEffect(UserSideEffects.NavigateTo(it.toRoute()))
            //        postSideEffect(UserSideEffects.Error(it))
            //    }
            //    .flowOn(dispatcherIO)
            //    .launchIn(coroutineScope)
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun save(projectsList: List<ProjectDto>, source: Source) = subIntent {
        val result = runCatching {
            withContext(dispatcherIO) {
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
                    OnboardingState.OnboardingCompleted
                }
                postSideEffect(UserSideEffects.NavigateTo(Route.List))
            }
            result.isFailure -> {
                reduce {
                    OnboardingState.Error(result.exceptionOrNull())
                }
            }
        }
    }
}
