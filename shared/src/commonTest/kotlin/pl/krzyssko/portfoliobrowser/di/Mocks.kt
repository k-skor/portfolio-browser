package pl.krzyssko.portfoliobrowser.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.api.Api
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.api.PagedSearchResult
import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.dto.GitHubSearch
import pl.krzyssko.portfoliobrowser.api.dto.GitHubUser
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Provider
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.UserInfo
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.QueryPagedResult
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto
import pl.krzyssko.portfoliobrowser.platform.ConfigurationDataSaver


class FakeAuth(configuration: ConfigurationDataSaver, isSignedIn: Boolean = false) : Auth(configuration) {
    override val isUserSignedIn: Boolean = isSignedIn
    override val userAccount: Account?
        get() = if (isUserSignedIn) Account("uid123", "user", "user@email.com", null, isEmailVerified = false, anonymous = false) else null
    override val userInfo: UserInfo? = if (isUserSignedIn) UserInfo("uid123", false) else null
    override val providerData: List<Provider> = emptyList()
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

    override suspend fun startSignInFlow(
        uiHandler: Any?,
        providerType: LoginMethod,
        create: Boolean,
        login: String?,
        password: String?,
        refresh: Boolean,
        token: String?,
        linkWithProvider: Boolean
    ): User.Authenticated? = if (isUserSignedIn && userAccount != null) User.Authenticated(userAccount!!) else null

    override suspend fun signInAnonymous(): User? = null

    override suspend fun signInWithGitHub(
        uiHandler: Any?,
        token: String?,
        refresh: Boolean
    ): User? = null

    override suspend fun signInLinkWithGitHub(uiHandler: Any?): User? = null

    override suspend fun createWithEmail(
        uiHandler: Any?,
        login: String,
        password: String
    ): User? = null

    override suspend fun signInWithEmail(
        uiHandler: Any?,
        login: String,
        password: String
    ): User? = null

    override suspend fun linkWithProvider(token: String): User? = null

    override suspend fun linkWithProvider(
        login: String,
        password: String
    ): User? = null

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

class FakeApi(
    private val user: GitHubUser? = null,
    private val projects: List<GitHubProject> = emptyList(),
    private val languages: GitHubLanguage = emptyMap()
) : Api {
    override suspend fun getUser(): GitHubUser? = user

    override suspend fun getRepos(): List<GitHubProject> = projects

    override suspend fun getRepos(queryParams: String?): PagedResponse<GitHubProject> {
        return PagedResponse(page = projects)
    }

    override suspend fun getRepoLanguages(repoName: String): GitHubLanguage = languages

    override suspend fun getRepoBy(name: String): GitHubProject? {
        return projects.find { it.name == name }
    }

    override suspend fun searchRepos(
        searchPhrase: String,
        queryParams: String?
    ): PagedSearchResult<GitHubSearch> {
        return PagedSearchResult(
            page = GitHubSearch(projects)
        )
    }
}
