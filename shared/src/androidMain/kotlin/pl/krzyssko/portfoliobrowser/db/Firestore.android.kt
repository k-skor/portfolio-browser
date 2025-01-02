package pl.krzyssko.portfoliobrowser.db

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.SyncData
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

    override suspend fun getProfile(uid: String): Profile? {
        val result = callOnDb {
            db.collection("users").document(uid).get()
        }
        return result?.toObject<Profile>()
    }

    override suspend fun createProfile(uid: String, profile: Profile) {
        callOnDb {
            db.collection("users").document(uid).set(profile, SetOptions.merge())
        }
    }

    override suspend fun createProjects(): String {
        val docRef = db.collection("projects").document()
        return docRef.id
    }

    override suspend fun syncProjects(uid: String, projectsList: List<Project>) {
        val projectsRef = db.collection("users").document(uid).collection("projects").document()
        val syncsRef = db.collection("sync").document()
        callOnDb {
            db.runBatch { batch ->
                for (project in projectsList) {
                    batch.set(projectsRef, project)
                }
                batch.set(syncsRef, SyncData(uid = uid, timestamp = Timestamp.now().let { it.seconds * 1_000 + it.nanoseconds.toLong() / 1_000_000 }, projectIds = projectsList.map { it.id }))
                batch.commit()
            }
        }
    }

    override suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long? {
        val snapshot = callOnDb<QuerySnapshot> {
            db.collection("sync").where(Filter.and(Filter.equalTo("uid", uid), Filter.equalTo("source", source))).orderBy("timestamp").limitToLast(1).get()
        }

        return if (!snapshot.isEmpty) snapshot.last().toObject<SyncData>().timestamp else null
    }

    override suspend fun writeProject(uid: String, project: Project) {
        callOnDb {
            db.collection("users").document(uid).collection("projects").add(project)
        }
    }
}

actual fun getFirestore(): Firestore = AndroidFirestore()
