package pl.krzyssko.portfoliobrowser.business

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.db.transfer.toDto
import pl.krzyssko.portfoliobrowser.db.Firestore

class Source(val projectsList: Flow<Project>, val source: pl.krzyssko.portfoliobrowser.data.Source)

class Destination(val firestore: Firestore, val auth: Auth)

class SyncHandler(private val source: Source, private val destination: Destination) {
    fun sync() = flow {
        if (!destination.auth.isUserSignedIn) {
            emit(false)
            return@flow
        }
        val userId = destination.auth.userAccount!!.id
        destination.firestore.syncProjects(
            userId,
            source.projectsList
                .map { it.toDto() }
                .catch { emit(false) }
                .toList(),
            source.source
        )
        emit(true)
    }
}
