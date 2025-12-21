package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.transfer.toProject
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.util.Response

class FirestoreException(message: String? = null, throwable: Throwable? = null): Exception(message)

class FirestorePagingState(val nextPageCursor: Any? = null, override val paging: Paging): PagingState

class FirestoreProjectRepository(private val firestore: Firestore, private val auth: Auth) : ProjectRepository {
    private val logging: Logging = getLogging()

    // Flow interface for reading latest value of query from store
    //private var lastDocId: Any? = null

    //override fun setLast(docId: Any) {
    //    lastDocId = docId
    //}

    //override fun getLast(): Any? = lastDocId
    private var firestorePagingState = FirestorePagingState(null, Paging())

    override val pagingState: PagingState
        get() = firestorePagingState

    override fun resetPagingState() {
        firestorePagingState = FirestorePagingState(null, Paging())
    }

    //override suspend fun uploadProject(docId: String, project: Project) {
    //    uploadProjects(docId, listOf(project))
    //}

    //override suspend fun uploadProjects(docId: String, projects: List<Project>) {
    //    firestore.syncProjects(docId, projects)
    //    //auth.userProfile?.id?.let { uid ->
    //    //    firestore.syncProjects(uid, projects)
    //    //} ?: throw IllegalStateException("User id missing.")
    //}

    //override suspend fun uploadProjects(projectsFlow: Flow<List<Project>>) {
    //    logging.debug("upload projects")
    //    val last = (userFlow.value as? User.Authenticated)
    //    logging.debug("upload projects last value=$last")
    //    firestore.syncProjects(
    //        last?.account?.id ?: return,
    //        projectsFlow.toList().reduce { acc, projects -> acc.toMutableList() + projects }
    //    )
    //}

    override fun fetchUser(): Flow<Result<String>> {
        TODO("Not yet implemented")
    }

    override fun fetchStack(name: String): Flow<Result<List<Stack>>> = flow {
        emit(Result.success(emptyList()))
    }

    override fun fetchProjectDetails(uid: String, id: String): Flow<Result<Project>> = flow {
        if (!auth.isUserSignedIn) {
            emit(Result.failure(FirestoreException("User not logged in.")))
            return@flow
        }
        emit(runCatching {
            firestore.getProject(auth.userAccount?.id!!, uid, id)?.toProject()
                ?: throw FirestoreException("Project not found.")
        })
    }

    override fun searchProjects(query: String, queryParams: String?): Flow<Result<PagedResponse<Project>>> {
        TODO("Not yet implemented")
    }

    override fun nextPage(): Flow<Result<List<Project>>> = flow {
        if (!auth.isUserSignedIn) {
            emit(Result.failure(FirestoreException("User not logged in.")))
            return@flow
        }
        emit(runCatching {
            //throw FirestoreException(message = "User not logged in.")
            val pageKey = firestorePagingState.nextPageCursor?.toString()
            firestore.getProjects(
                firestorePagingState.nextPageCursor,
                auth.userAccount?.id!!
            ).also {
                val nextKey = if (it.value.isNotEmpty()) it.cursor?.toString() else null
                firestorePagingState = FirestorePagingState(
                    it.cursor, Paging(
                        pageKey = pageKey,
                        prevPageKey = firestorePagingState.paging.pageKey,
                        nextPageKey = nextKey,
                        isLastPage = nextKey == null
                    )
                )
            }.value.map {
                it.toProject()
            }
        })
    }

    //override fun fetchList(pageKey: Any?): Flow<PagedResponse<Project>> = flow {
    //    val pageKey = firestorePagingState?.nextPageCursor?.toString()

    //    emit(firestore.getProjects(firestorePagingState?.nextPageCursor, auth.userProfile?.id!!).also {
    //        val nextKey = if (it.value.isNotEmpty()) it.cursor?.toString() else null
    //        firestorePagingState = FirestorePagingState(it.cursor, Paging(
    //            pageKey = pageKey,
    //            prevPageKey = firestorePagingState?.paging?.pageKey,
    //            nextPageKey = nextKey,
    //            isLastPage = nextKey == null
    //        ))
    //    }.value.map {
    //        it.toProject()
    //    })
    //}

    override fun nextSearchPage(
        query: String
    ): Flow<Result<List<Project>>> {
        TODO("Not yet implemented")
    }
}
