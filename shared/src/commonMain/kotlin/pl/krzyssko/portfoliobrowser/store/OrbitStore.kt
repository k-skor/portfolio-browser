package pl.krzyssko.portfoliobrowser.store

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.container
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

open class OrbitStore<TState : Any>(
    val coroutineScope: CoroutineScope,
    val dispatcherIO: CoroutineDispatcher,
    initialState: TState
) :
    ContainerHost<TState, UserSideEffects>, KoinComponent {
    override val container = coroutineScope.container<TState, UserSideEffects>(
        initialState,
    )
    val stateFlow = container.stateFlow
    val sideEffectFlow = container.sideEffectFlow
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Project>.mapProject(colorPicker: InfiniteColorPicker, ownerId: String): Flow<Project> =
    map { project ->
        project.copy(stack = project.stack.map { stack ->
            stack.copy(
                color = colorPicker.pick(
                    stack.name
                )
            )
        }, favorite = project.followers.any { it.uid == ownerId })
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun OrbitStore<ProjectState>.loadFrom(
    repository: ProjectRepository,
    colorPicker: InfiniteColorPicker,
    ownerId: String,
    projectId: String
) = intent {
    postSideEffect(UserSideEffects.Toast("Loading project $ownerId:$projectId details"))

    flow {
        emit(repository.fetchProjectDetails(uid = ownerId, id = projectId))
    }
        .flowOn(dispatcherIO)
        .map {
            when {
                it.isSuccess -> {
                    it.getOrNull()
                }

                else -> {
                    val exception = it.exceptionOrNull()
                    reduce {
                        ProjectState.Error(exception)
                    }
                    postSideEffect(UserSideEffects.Error(exception))
                    null
                }
            }
        }
        .filterNotNull()
        .mapProject(colorPicker, ownerId)
        .collect {
            getLogging().debug("store project emit")
            reduce {
                ProjectState.Loaded(it)
            }
            postSideEffect(UserSideEffects.NavigateTo(Route.Details(ownerId, it.id)))
        }
}

fun OrbitStore<ProjectState>.followProject(db: Firestore, auth: Auth, follow: Boolean) = intent {
    if (!auth.isUserSignedIn) {
        postSideEffect(UserSideEffects.Error(IllegalStateException("User not signed in.")))
        return@intent
    }
    val project = (state as ProjectState.Loaded).project
    val userId = auth.userAccount!!.id
    val ownerId = project.createdBy
    val profile = db.getProfile(userId)
    val follower = Follower(
        uid = userId,
        name = profile?.alias ?: "${profile?.firstName} ${profile?.lastName}"
    )

    withContext(dispatcherIO) {
        db.toggleFollowProject(ownerId, project.id, follower, follow)
    }

    reduce {
        ProjectState.Loaded(
            if (follow) {
                project.copy(followers = project.followers + follower)
            } else {
                project.copy(followers = project.followers - follower)
            }
        )
    }
}

@OptIn(OrbitExperimental::class)
suspend fun OrbitStore<ProjectState>.updateProject(
    project: Project,
) = subIntent {
    reduce {
        ProjectState.Loaded(project)
    }
}

fun OrbitStore<ProjectState>.saveProject(
    project: Project,
    db: Firestore,
    createNew: Boolean = false
) = intent {
    (state as? ProjectState.Loaded)?.let {
        try {
            db.updateProject(project.createdBy, if (!createNew) project.id else null, project.toDto())
            reduce {
                ProjectState.Loaded(project)
            }
        } catch (e: Exception) {
            postSideEffect(UserSideEffects.Toast(e.toString()))
        }
    }
}

fun OrbitStore<ProjectsListState>.updateSearchPhrase(
    phrase: String
) = intent {
    reduce {
        ProjectsListState.FilterRequested(searchPhrase = phrase)
    }
}

fun OrbitStore<ProjectsListState>.updateSelectedCategories(
    categories: List<String>
) = intent {
    reduce {
        ProjectsListState.FilterRequested(selectedCategories = categories)
    }
}

fun OrbitStore<ProjectsListState>.updateOnlyFeatured(
    featured: Boolean
) = intent {
    reduce {
        ProjectsListState.FilterRequested(onlyFeatured = featured)
    }
}

fun OrbitStore<ProjectsListState>.clearFilters() = intent {
    reduce {
        ProjectsListState.FilterRequested()
    }
}
