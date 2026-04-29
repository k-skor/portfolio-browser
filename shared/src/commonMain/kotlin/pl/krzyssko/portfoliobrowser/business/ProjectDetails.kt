package pl.krzyssko.portfoliobrowser.business

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
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull

class ProjectDetails(
    coroutineScope: CoroutineScope,
    private val dispatcherIO: CoroutineDispatcher,
    private val repository: ProjectRepository,
    private val auth: Auth,
    private val firestore: Firestore
) : KoinComponent, OrbitStore<ProjectState>(coroutineScope, ProjectState.Loading) {

    val details: Flow<Project> = stateFlow
        .map {
            when (it) {
                is ProjectState.Loading -> Response.Pending
                is ProjectState.Loaded -> Response.Ok(it.project)
                is ProjectState.Error -> Response.Error(it.reason)
            }
        }
        .map { it.getOrNull() }
        .filterNotNull()
        //.stateIn(viewModelScope, SharingStarted.Eagerly, Response.Pending)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Project>.mapProject(colorPicker: InfiniteColorPicker, ownerId: String): Flow<Project> =
        map { project ->
            project.copy(
                stack = project.stack.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(
                            stack.name
                        )
                    )
                }, 
                favorite = project.followers.any { it.uid == ownerId }
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadFrom(
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

    fun followProject(follow: Boolean) = intent {
        if (!auth.isUserSignedIn) {
            postSideEffect(UserSideEffects.Error(IllegalStateException("User not signed in.")))
            return@intent
        }
        val project = (state as ProjectState.Loaded).project
        val userId = auth.userAccount!!.id
        val ownerId = project.createdBy
        val profile = firestore.getProfile(userId)
        val follower = Follower(
            uid = userId,
            name = profile?.alias ?: "${profile?.firstName} ${profile?.lastName}"
        )

        withContext(dispatcherIO) {
            firestore.toggleFollowProject(ownerId, project.id, follower, follow)
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
    suspend fun updateProject(
        project: Project,
    ) = subIntent {
        reduce {
            ProjectState.Loaded(project)
        }
    }

    fun saveProject(
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
}
