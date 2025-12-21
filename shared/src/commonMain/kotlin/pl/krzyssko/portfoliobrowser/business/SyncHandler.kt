package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.db.Firestore

class Source(val projectsList: Flow<Project>, val source: pl.krzyssko.portfoliobrowser.data.Source)

class Destination(val firestore: Firestore, val user: User.Authenticated)

//sealed class SyncResult {
//    data object Success: SyncResult()
//    data class Failure(val throwable: Throwable): SyncResult()
//}

class SyncHandler(private val source: Source, private val destination: Destination) {
    fun sync() = flow {
        val user = destination.user
        destination.firestore.syncProjects(
            user.account.id,
            source.projectsList.map { it.toDto() }.toList(),
            source.source
        )
        //emit(SyncResult.Success)
        emit(true)
    }
}

sealed class ExternalProfileOnboardingRequest(val source: pl.krzyssko.portfoliobrowser.data.Source) {
    data class FromGitHub(val projectsList: StateFlow<Project>) :
        ExternalProfileOnboardingRequest(pl.krzyssko.portfoliobrowser.data.Source.GitHub)
}

sealed class ProfileDestination() {
    data class FirestoreDestination(val firestore: Firestore)
}

class UserOnboardingFromExternalProfile() {

    private var request: ExternalProfileOnboardingRequest? = null
    private var destination: ProfileDestination? = null

    fun newProfileRequest(request: ExternalProfileOnboardingRequest, destination: ProfileDestination) {
        this.request = request
        this.destination = destination
        setupProfileSync()
    }

    suspend fun start() {

    }

    private fun setupProfileSync() {

    }


}