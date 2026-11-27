package pl.krzyssko.portfoliobrowser.db

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto

class IosFirestore : Firestore {
    override suspend fun hasUser(uid: String): Boolean = TODO("Not yet implemented")
    override suspend fun getProfile(uid: String): ProfileDto? = TODO("Not yet implemented")
    override fun observeProfile(uid: String): Flow<ProfileDto?> = TODO("Not yet implemented")
    override suspend fun createProfile(uid: String, profile: ProfileDto): Unit = TODO("Not yet implemented")
    override suspend fun createProjects(): String = TODO("Not yet implemented")
    override suspend fun syncProjects(uid: String, projectsList: List<ProjectDto>, source: Source): Unit = TODO("Not yet implemented")
    override suspend fun getProjects(cursor: Any?, uid: String, category: String?): QueryPagedResult<ProjectDto> = TODO("Not yet implemented")
    override suspend fun getProject(uid: String, ownerId: String, projectId: String): ProjectDto? = TODO("Not yet implemented")
    override fun observeProject(uid: String, ownerId: String, projectId: String): Flow<ProjectDto?> = TODO("Not yet implemented")
    override suspend fun updateProject(uid: String, id: String?, project: ProjectDto): Unit = TODO("Not yet implemented")
    override suspend fun toggleFollowProject(uid: String, id: String, follower: Follower, toggle: Boolean): Unit = TODO("Not yet implemented")
    override suspend fun getFavoriteProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto> = TODO("Not yet implemented")
    override suspend fun getSearchResults(documentIds: List<String>): List<ProjectDto> = TODO("Not yet implemented")
    override suspend fun searchProjects(phrase: String, cursor: Any?, uid: String): QueryPagedResult<ProjectDto> = TODO("Not yet implemented")
    override suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long? = TODO("Not yet implemented")
}

actual fun getFirestore(): Firestore = IosFirestore()
