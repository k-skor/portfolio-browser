package pl.krzyssko.portfoliobrowser.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.QueryPagedResult
import pl.krzyssko.portfoliobrowser.db.transfer.toProject
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.util.Response

class FirestoreException(message: String? = null, throwable: Throwable? = null): Exception(message)

class FirestorePagingState(val nextPageCursor: Any? = null, override val pageSize: Int = 5, override val paging: Paging = Paging()): PagingState

class FirestoreProjectRepository(private val firestore: Firestore, private val auth: Auth) : ProjectRepository {
    private val logging: Logging = getLogging()

    // Flow interface for reading latest value of query from store
    //private var lastDocId: Any? = null

    //override fun setLast(docId: Any) {
    //    lastDocId = docId
    //}

    //override fun getLast(): Any? = lastDocId
    private var firestorePagingState = FirestorePagingState(null, paging = Paging())

    override val pagingState: FirestorePagingState
        get() = firestorePagingState

    override fun resetPagingState() {
        firestorePagingState = FirestorePagingState(null, paging = Paging())
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

    override suspend fun nextPage(nextPageKey: Any?): Result<List<Project>> {
        if (!auth.isUserSignedIn) {
            return Result.failure(FirestoreException("User not logged in."))
        }
        return runCatching {
            //throw FirestoreException(message = "User not logged in.")
            //val pageKey = firestorePagingState.nextPageCursor?.toString()
            val pageKey = nextPageKey
            firestore.getProjects(
                firestorePagingState.nextPageCursor,
                auth.userAccount?.id!!
            ).also {
                val nextKey = it.cursor
                firestorePagingState = FirestorePagingState(
                    it.cursor, firestorePagingState.pageSize, Paging(
                        pageKey = pageKey,
                        prevPageKey = firestorePagingState.paging.pageKey,
                        nextPageKey = nextKey,
                        isLastPage = nextKey == null
                    )
                )
            }.value.map {
                it.toProject()
            }
            //PagedData(query.value.map {
            //    it.toProject()
            //}, pagingState.getNextPage(pageKey, query).paging)
        }
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

    override suspend fun nextSearchPage(
        query: String,
        nextPageKey: Any?
    ): Result<List<Project>> {
        return Result.failure(FirestoreException("Not implemented"))
    }

    override suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>> {
        if (!auth.isUserSignedIn) {
            return Result.failure(FirestoreException("User not logged in."))
        }
        return runCatching {
            //val pageKey = firestorePagingState.nextPageCursor?.toString()
            val pageKey = nextPageKey
            firestore.getFavoriteProjects(
                firestorePagingState.nextPageCursor,
                auth.userAccount?.id!!
            ).also {
                val nextKey = it.cursor
                firestorePagingState = FirestorePagingState(
                    it.cursor, firestorePagingState.pageSize, Paging(
                        pageKey = pageKey,
                        prevPageKey = firestorePagingState.paging.pageKey,
                        nextPageKey = nextKey,
                        isLastPage = nextKey == null
                    )
                )
            }.value.map {
                it.toProject()
            }
        }
    }
}
fun FirestorePagingState.getNextPage(pageKey: Any?, query: QueryPagedResult<*>): FirestorePagingState {
    return FirestorePagingState(
        query.cursor, pageSize, Paging(
            pageKey = pageKey,
            prevPageKey = paging.pageKey,
            nextPageKey = query.cursor,
            isLastPage = query.cursor == null
        )
    )
}
