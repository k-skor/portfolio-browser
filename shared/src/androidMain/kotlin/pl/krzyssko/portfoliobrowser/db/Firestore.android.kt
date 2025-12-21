package pl.krzyssko.portfoliobrowser.db

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.transfer.DataSyncDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto


class AndroidFirestore: Firestore {
    private val db = Firebase.firestore

    override suspend fun hasUser(uid: String): Boolean {
        val result = db.collection("users").document(uid).get().await()
        return result.exists()
    }

    override suspend fun getProfile(uid: String): ProfileDto? {
        val result = db.collection("users").document(uid).get().await()
        return result?.toObject<ProfileDto>()
    }

    override suspend fun createProfile(uid: String, profile: ProfileDto) {
        db.collection("users").document(uid).set(profile, SetOptions.merge()).await()
    }

    override suspend fun createProjects(): String {
        val docRef = db.collection("projects").document()
        return docRef.id
    }

    override suspend fun syncProjects(uid: String, projectsList: List<ProjectDto>, source: Source) {
        val usersRef = db.collection("users").document(uid)
        val syncsRef = db.collection("sync").document()
        db.runBatch { batch ->
            for (project in projectsList) {
                val projectRef = usersRef.collection("projects").document()
                batch.set(projectRef, project.copy(id = projectRef.id))
            }
            batch.set(
                syncsRef,
                DataSyncDto(
                    uid = uid,
                    timestamp = Timestamp.now()
                        .let { it.seconds * 1_000 + it.nanoseconds.toLong() / 1_000_000 },
                    source = source.toString(),
                    projectIds = projectsList.map { it.id!! })
            )
            batch.commit()
        }.await()
    }

    override suspend fun getProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto> {
        val colRef = db.collectionGroup("projects")
        var query = colRef.where(
            Filter.or(
                Filter.equalTo("public", true),
                Filter.equalTo("createdBy", uid)
            )
        ).orderBy("followersCount", Query.Direction.DESCENDING).limit(5)

        (cursor as? DocumentSnapshot)?.let {
            query = colRef.where(
                Filter.or(
                    Filter.equalTo("public", true),
                    Filter.equalTo("createdBy", uid)
                )
            ).orderBy("followersCount", Query.Direction.DESCENDING).startAfter(it).limit(5)
        }

        val snapshot = query.get().await()

        return QueryPagedResult(
            updates = (this::projectsUpdates)(query),
            value = if (!snapshot.isEmpty) snapshot.toObjects<ProjectDto>() else emptyList(),
            cursor = if (!snapshot.isEmpty) snapshot.documents[snapshot.size() - 1] else null
        )
    }

    fun projectsUpdates(query: Query): Flow<List<ProjectDto>> = flow {
        query.snapshots().map { snapshots ->
            snapshots.toObjects<List<ProjectDto>>()
        }
    }

    override suspend fun getProject(uid: String, ownerId: String, projectId: String): ProjectDto? {
        val collection = db.collection("users").document(ownerId).collection("projects")

        val result = if (ownerId != uid) {
            collection
                .where(
                    Filter.and(
                        Filter.equalTo("public", true),
                        Filter.equalTo("id", projectId)
                    )
                )
                .get().await()
        } else {
            collection
                .where(
                    Filter.and(
                        Filter.equalTo("createdBy", uid),
                        Filter.equalTo("id", projectId)
                    )
                )
                .get().await()
        }
        return result.documents.firstOrNull()?.toObject<ProjectDto>()
    }

    override suspend fun updateProject(uid: String, id: String?, project: ProjectDto) {
        val collection = db.collection("users").document(uid).collection("projects")
        id?.let {
            collection.document(id).set(project, SetOptions.merge()).await()
        } ?: run {
            collection.add(project).await()
        }
    }

    override suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long? {
        val snapshot = db.collection("users").document(uid).collection("sync_data")
            .where(Filter.and(Filter.equalTo("uid", uid), Filter.equalTo("source", source)))
            .orderBy("timestamp").limitToLast(1).get().await()

        return if (!snapshot.isEmpty) snapshot.last().toObject<DataSyncDto>().timestamp else null
    }
}

actual fun getFirestore(): Firestore = AndroidFirestore()
