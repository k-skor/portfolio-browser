package pl.krzyssko.portfoliobrowser.db

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class AndroidFirestore: Firestore {
    private val db = Firebase.firestore

    private suspend fun <T: Any> callOnDb(block: () -> Task<T>) = suspendCoroutine { continuation ->
        val callback = createCallbackWithContinuation(
            onSuccess = continuation::resume,
            onFailure = continuation::resumeWithException
        )

        block().addOnSuccessListener { ref: T? -> callback.onSuccess(ref) }
            .addOnFailureListener { error -> callback.onFailure(error) }
    }

    interface DbFlowCallback {
        fun onSuccess(ref: Any?)
        fun onFailure(error: Throwable)
    }

    private fun createCallbackWithContinuation(
        onSuccess: (ref: Any?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) = object : DbFlowCallback {
        // Resume the coroutine with the result
        override fun onSuccess(ref: Any?) = onSuccess(ref)

        // Resume the coroutine with an exception
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    override suspend fun createUser(uid: String, profile: Profile) {
        callOnDb<Void> {
            db.collection("users").document(uid).set(profile, SetOptions.merge())
        }
    }

    override suspend fun syncProjects(projectsList: List<Project>) {

    }
}

actual fun getFirestore(): Firestore = AndroidFirestore()
