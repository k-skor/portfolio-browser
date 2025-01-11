package pl.krzyssko.portfoliobrowser.db

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class AndroidFirestore: Firestore {
    private val db = Firebase.firestore

    private suspend fun <T> callOnDb(block: () -> Task<T>) = suspendCoroutine { continuation ->
        val callback = createCallbackWithContinuation(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )

        block().addOnSuccessListener { ref: T -> callback.onSuccess(ref) }
            .addOnFailureListener { error -> callback.onFailure(error) }
    }

    interface DbFlowCallback<T> {
        fun onSuccess(ref: T)
        fun onFailure(error: Throwable)
    }

    private fun <T> createCallbackWithContinuation(
        onSuccess: (ref: T) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : DbFlowCallback<T> {
        // Resume the coroutine with the result
        override fun onSuccess(ref: T) = onSuccess(ref)

        // Resume the coroutine with an exception
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    override suspend fun isUserCreated(uid: String): Boolean {
        val result = callOnDb {
            db.collection("users").document(uid).get()
        }
        return result.exists()
    }

    override suspend fun getProfile(uid: String): ProfileDto? {
        val result = callOnDb {
            db.collection("users").document(uid).get()
        }
        return result?.toObject<ProfileDto>()
    }

    override suspend fun createProfile(uid: String, profile: ProfileDto) {
        callOnDb {
            db.collection("users").document(uid).set(profile, SetOptions.merge())
        }
    }

    override suspend fun createProjects(): String {
        val docRef = db.collection("projects").document()
        return docRef.id
    }

    override suspend fun syncProjects(uid: String, projectsList: List<ProjectDto>, source: Source) {
        val usersRef = db.collection("users").document(uid)
        val syncsRef = db.collection("sync").document()
        callOnDb {
            db.runBatch { batch ->
                for (project in projectsList) {
                    val projectRef = usersRef.collection("projects").document()
                    batch.set(projectRef, project)
                }
                batch.set(syncsRef, DataSyncDto(uid = uid, timestamp = Timestamp.now().let { it.seconds * 1_000 + it.nanoseconds.toLong() / 1_000_000 }, source = source.toString(), projectIds = projectsList.map { it.id!! }))
                batch.commit()
            }
        }
    }

    override suspend fun getProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto> {
        val colRef = db.collectionGroup("projects")
        var query = colRef.where(Filter.or(Filter.equalTo("public", true), Filter.equalTo("createdBy", uid))).orderBy("followersCount", Query.Direction.DESCENDING).limit(5)

        (cursor as? DocumentSnapshot)?.let {
            query = colRef.where(Filter.or(Filter.equalTo("public", true), Filter.equalTo("createdBy", uid))).orderBy("followersCount", Query.Direction.DESCENDING).startAfter(it).limit(5)
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

    override suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long? {
        val snapshot = callOnDb<QuerySnapshot> {
            db.collection("sync").where(Filter.and(Filter.equalTo("uid", uid), Filter.equalTo("source", source))).orderBy("timestamp").limitToLast(1).get()
        }

        return if (!snapshot.isEmpty) snapshot.last().toObject<DataSyncDto>().timestamp else null
    }

    override suspend fun writeProject(uid: String, project: ProjectDto) {
        callOnDb {
            db.collection("users").document(uid).collection("projects").add(project)
        }
    }
}

actual fun getFirestore(): Firestore = AndroidFirestore()
