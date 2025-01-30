package pl.krzyssko.portfoliobrowser.db

import kotlinx.coroutines.flow.Flow
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto


interface Firestore {
    suspend fun isUserCreated(uid: String): Boolean
    suspend fun getProfile(uid: String): ProfileDto?
    suspend fun createProfile(uid: String, profile: ProfileDto)
    suspend fun createProjects(): String
    suspend fun syncProjects(uid: String, projectsList: List<ProjectDto>, source: Source)
    suspend fun getProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto>
    suspend fun getProject(uid: String, ownerId: String, projectId: String): ProjectDto?
    suspend fun updateProject(uid: String, id: String?, project: ProjectDto)
    suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long?
}

data class QueryPagedResult<out T>(
    val updates: Flow<List<T>>,
    val value: List<T>,
    val cursor: Any?
)

expect fun getFirestore(): Firestore
