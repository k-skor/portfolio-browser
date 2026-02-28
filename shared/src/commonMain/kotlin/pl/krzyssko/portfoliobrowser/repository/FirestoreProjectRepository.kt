package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.PagedResponse
import pl.krzyssko.portfoliobrowser.auth.Auth
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.Firestore
import pl.krzyssko.portfoliobrowser.db.QueryPagedResult
import pl.krzyssko.portfoliobrowser.db.transfer.toProject
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging

private val logging: Logging = getLogging()

class FirestoreException(message: String? = null, throwable: Throwable? = null): Exception(message)

class FirestorePagingState(val nextPageCursor: Any? = null, override val pageSize: Int = 5, override val paging: Paging = Paging()): PagingState

class FirestoreProjectRepository(private val firestore: Firestore, private val auth: Auth) : ProjectRepository {

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

    override suspend fun fetchTotalProjectsSize(): Result<Int> {
        return Result.failure(FirestoreException(throwable = NotImplementedError()))
    }

    override suspend fun fetchProjectDetails(uid: String, id: String): Result<Project> {
        if (!auth.isUserSignedIn) {
            return Result.failure(IllegalStateException("User not logged in."))
        }
        return runCatching {
            firestore.getProject(auth.userAccount?.id!!, uid, id)?.toProject()
                ?: throw FirestoreException("Project not found.")
        }
    }

    override suspend fun searchProjects(query: String, queryParams: String?): Result<PagedResponse<Project>> {
        return Result.failure(FirestoreException(throwable = NotImplementedError()))
    }

    override suspend fun nextPage(nextPageKey: Any?): Result<List<Project>> {
        if (!auth.isUserSignedIn) {
            return Result.failure(IllegalStateException("User not logged in."))
        }
        return runCatching {
            firestore.getProjects(
                firestorePagingState.nextPageCursor,
                auth.userAccount?.id!!
            ).also {
                firestorePagingState = it.getNextPage2(firestorePagingState, nextPageKey)
            }.value.map {
                it.toProject()
            }
        }
    }

    override suspend fun nextSearchPage(
        query: String,
        nextPageKey: Any?
    ): Result<List<Project>> {
        return Result.failure(FirestoreException(throwable = NotImplementedError()))
    }

    override suspend fun nextFavoritePage(nextPageKey: Any?): Result<List<Project>> {
        if (!auth.isUserSignedIn) {
            return Result.failure(FirestoreException("User not logged in."))
        }
        return runCatching {
            firestore.getFavoriteProjects(
                firestorePagingState.nextPageCursor,
                auth.userAccount?.id!!
            ).also {
                firestorePagingState = it.getNextPage2(firestorePagingState, nextPageKey)

            }.value.map {
                it.toProject()
            }
        }
    }
}

fun QueryPagedResult<*>.getNextPage2(prevState: FirestorePagingState, pageKey: Any?): FirestorePagingState {
    return FirestorePagingState(
        cursor, prevState.pageSize, Paging(
            pageKey = pageKey,
            prevPageKey = prevState.paging.pageKey,
            nextPageKey = cursor,
            isLastPage = cursor == null
        )
    )
}
