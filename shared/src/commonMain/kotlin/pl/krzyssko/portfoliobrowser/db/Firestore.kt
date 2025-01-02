package pl.krzyssko.portfoliobrowser.db

import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source


interface Firestore {
    suspend fun isUserCreated(uid: String): Boolean
    suspend fun getProfile(uid: String): Profile?
    suspend fun createProfile(uid: String, profile: Profile)
    suspend fun createProjects(): String
    suspend fun syncProjects(uid: String, projectsList: List<Project>)
    suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long?
    suspend fun writeProject(uid: String, project: Project)
}

expect fun getFirestore(): Firestore
