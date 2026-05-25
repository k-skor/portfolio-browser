package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.annotation.OrbitExperimental
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsQueryState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects
import pl.krzyssko.portfoliobrowser.util.Response
import pl.krzyssko.portfoliobrowser.util.getOrNull
import kotlin.random.Random

class ProjectDetails(
    coroutineScope: CoroutineScope,
    private val dispatcherIO: CoroutineDispatcher,
    private val repository: ProjectRepository,
    private val colorPicker: InfiniteColorPicker,
    private val auth: Auth,
    private val firestore: Firestore,
    private val selectedFlow: Flow<ProjectsQueryState.DetailsSelected?>
) : KoinComponent, OrbitStore<ProjectState>(coroutineScope, ProjectState.Initialized) {

    init {
        doOnCreate {
            repeatOnSubscription {
                selectedFlow
                    .collectLatest {
                        if (it != null) {
                            observe(it.projectId, it.createdBy)
                        } else {
                            reduce {
                                ProjectState.Initialized
                            }
                        }
                    }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Project>.mapProject(colorPicker: InfiniteColorPicker, currentUserId: String): Flow<Project> =
        map { project ->
            project.copy(
                stack = project.stack.map { stack ->
                    stack.copy(
                        color = colorPicker.pick(
                            stack.name
                        )
                    )
                },
                favorite = project.followers.any { it.uid == currentUserId }
            )
        }

    @OptIn(OrbitExperimental::class)
    suspend fun observe(projectId: String, ownerId: String) = subIntent {
        repository.observeProjectDetails(ownerId = ownerId, projectId = projectId)
            .flowOn(dispatcherIO)
            .onStart {
                reduce {
                    ProjectState.Loading
                }
                postSideEffect(UserSideEffects.Toast("Observing project $ownerId:$projectId details"))
            }
            .filterNotNull()
            .mapProject(colorPicker, auth.userAccount?.id ?: "")
            .collect {
                getLogging().debug("store project emit")
                reduce {
                    ProjectState.Loaded(it)
                }
            }
    }

    fun followProject(follow: Boolean) = intent {
        if (!auth.isUserSignedIn) {
            postSideEffect(UserSideEffects.Error(IllegalStateException("User not signed in.")))
            return@intent
        }
        val project = (state as? ProjectState.Loaded)?.project ?: return@intent
        val userId = auth.userAccount!!.id
        val ownerId = project.createdBy
        val profile = firestore.getProfile(userId)
        val follower = Follower(
            uid = userId,
            name = profile?.let { it.alias ?: "${it.firstName} ${it.lastName}" }
                ?: "User${Random.nextInt().toString().take(6)}"
        )

        withContext(dispatcherIO) {
            firestore.toggleFollowProject(ownerId, project.id, follower, follow)
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
        try {
            db.updateProject(project.createdBy, if (!createNew) project.id else null, project.toDto())
        } catch (e: Exception) {
            postSideEffect(UserSideEffects.Toast(e.toString()))
        }
    }

    fun reset() = intent {
        reduce {
            ProjectState.Loading
        }
    }
}

fun StateFlow<ProjectState>.toDetails() =
    map {
        when (it) {
            is ProjectState.Initialized,
            is ProjectState.Loading -> Response.Pending
            is ProjectState.Loaded -> Response.Ok(it.project)
            is ProjectState.Error -> Response.Error(it.reason)
        }
    }
        .map { it.getOrNull() }
