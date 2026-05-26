package pl.krzyssko.portfoliobrowser.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.UserInfo
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.QueryPagedResult
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.repository.CategoriesRepository
import pl.krzyssko.portfoliobrowser.repository.Paging
import pl.krzyssko.portfoliobrowser.repository.PagingState
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.repository.SearchRepository
import pl.krzyssko.portfoliobrowser.repository.UserRepository


class FakeAuth(configuration: Configuration, isSignedIn: Boolean = false) : Auth(configuration) {
    override val isUserSignedIn: Boolean = isSignedIn
    override val userAccount: Account?
        get() = if (isUserSignedIn) Account("uid123", "user", "user@email.com", null, isEmailVerified = false, anonymous = false) else null
    override val userInfo: UserInfo? = if (isUserSignedIn) UserInfo("uid123", false) else null
    override val providerData: List<Provider>?
        get() = TODO("Not yet implemented")
    override val hasGitHubProvider: Boolean
        get() = TODO("Not yet implemented")
    override val hasEmailProvider: Boolean
        get() = TODO("Not yet implemented")
    //override val userState: StateFlow<UserInfo?> = userFlow().stateIn(
    //    CoroutineScope(StandardTestDispatcher()),
    //    SharingStarted.WhileSubscribed(),
    //    null
    //)
    override val userState: StateFlow<UserInfo?> = MutableStateFlow(userInfo)

    override fun userFlow(): Flow<UserInfo?> {
        TODO("Not yet implemented")
    }

    override suspend fun signInAnonymous(): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithGitHub(
        uiHandler: Any?,
        token: String?,
        refresh: Boolean
    ): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInLinkWithGitHub(uiHandler: Any?): User? {
        TODO("Not yet implemented")
    }

    override suspend fun createWithEmail(
        uiHandler: Any?,
        login: String,
        password: String
    ): User? {
        TODO("Not yet implemented")
    }

    override suspend fun signInWithEmail(
        uiHandler: Any?,
        login: String,
        password: String
    ): User? {
        TODO("Not yet implemented")
    }

    override suspend fun linkWithProvider(token: String): User? {
        TODO("Not yet implemented")
    }

    override suspend fun linkWithProvider(
        login: String,
        password: String
    ): User? {
        TODO("Not yet implemented")
    }

    override fun signOut() {
        TODO("Not yet implemented")
    }

    override suspend fun delete() {
        TODO("Not yet implemented")
    }
}

class FakeFirestore(
    val hasUser: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    private val shouldFailSync: Boolean = false
) : Firestore {
    override suspend fun hasUser(uid: String): Boolean = hasUser

    override suspend fun getProfile(uid: String): ProfileDto? {
        TODO("Not yet implemented")
    }

    override fun observeProfile(uid: String): Flow<ProfileDto?> {
        TODO("Not yet implemented")
    }

    override suspend fun createProfile(
        uid: String,
        profile: ProfileDto
    ) { }

    override suspend fun createProjects(): String {
        TODO("Not yet implemented")
    }

    override suspend fun syncProjects(
        uid: String,
        projectsList: List<ProjectDto>,
        source: Source
    ) {
        if (shouldFailSync) throw RuntimeException("Sync failed")
    }

    override suspend fun getProjects(
        cursor: Any?,
        uid: String,
        category: String?
    ): QueryPagedResult<ProjectDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getProject(
        uid: String,
        ownerId: String,
        projectId: String
    ): ProjectDto? {
        TODO("Not yet implemented")
    }

    override fun observeProject(
        uid: String,
        ownerId: String,
        projectId: String
    ): Flow<ProjectDto?> {
        TODO("Not yet implemented")
    }

    override suspend fun updateProject(
        uid: String,
        id: String?,
        project: ProjectDto
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun toggleFollowProject(
        uid: String,
        id: String,
        follower: Follower,
        toggle: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getFavoriteProjects(
        cursor: Any?,
        uid: String
    ): QueryPagedResult<ProjectDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSearchResults(documentIds: List<String>): List<ProjectDto> {
        TODO("Not yet implemented")
    }

    override suspend fun searchProjects(
        phrase: String,
        cursor: Any?,
        uid: String
    ): QueryPagedResult<ProjectDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getLastSyncTimestampForSource(
        uid: String,
        source: Source
    ): Long? = lastSyncTimestamp
}

class FakeProjectRepository(
    private val projects: List<Project> = emptyList(),
    private val totalSize: Int = projects.size,
) : ProjectRepository, SearchRepository, UserRepository, CategoriesRepository {
    override val searchPagingState: PagingState = object : PagingState {
        override val paging: Paging = Paging(isLastPage = true)
        override val pageSize: Int = 10
    }
    override val pagingState: PagingState = object : PagingState {
        override val paging: Paging = Paging(isLastPage = true)
        override val pageSize: Int = 10
    }

    override fun resetPagingState() {}

    override suspend fun fetchTotalProjectsSize(): Result<Int> = Result.success(totalSize)

    override suspend fun fetchStack(name: String): Result<List<Stack>> = Result.success(emptyList())

    override suspend fun nextPage(nextPageKey: Any?, category: String?): Result<List<Project>> = Result.success(projects)

    override suspend fun fetchProjectsByIds(docIds: List<String>): Result<List<Project>> = Result.success(emptyList())

    override suspend fun fetchProjectDetails(uid: String, id: String): Result<Project> = TODO()

    override fun observeProjectDetails(ownerId: String, projectId: String): Flow<Project?> = TODO()

    override suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>> = Result.success(emptyList())
    override fun resetSearchPagingState() {
        TODO("Not yet implemented")
    }

    override suspend fun nextSearchPage(
        query: String,
        nextPageKey: Any?
    ): Result<List<Project>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchUser(): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCategory(name: String): Result<List<Stack>> {
        TODO("Not yet implemented")
    }
}
