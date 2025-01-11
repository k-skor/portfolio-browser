package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.db.Firestore

class Source(val projectsList: Flow<Project>, val source: pl.krzyssko.portfoliobrowser.data.Source)

class Destination(val firestore: Firestore, val user: User.Authenticated)

sealed class SyncResult {
    data object Success: SyncResult()
    data class Failure(val throwable: Throwable): SyncResult()
}

class SyncHandler(private val source: Source, private val destination: Destination) {
    fun sync() = flow {
        try {
            destination.firestore.syncProjects(destination.user.account.id, source.projectsList.map { it.toDto() }.toList(), source.source)
            emit(SyncResult.Success)
        } catch (exception: Exception) {
            emit(SyncResult.Failure(exception))
        }
    }
}