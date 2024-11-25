package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.State
import pl.krzyssko.portfoliobrowser.store.loadPageFrom
import pl.krzyssko.portfoliobrowser.store.projectsList

//@Suppress("CAST_NEVER_SUCCEEDS")
class MyPagingSource(private val repository: ProjectRepository, private val store: OrbitStore<State.ProjectsListState>): PagingSource<String, Project>() {
    private val log: Logging = getLogging()

    override fun getRefreshKey(state: PagingState<String, Project>): String? = null

    /**
     *  1. Create intent to fetch repos with paging params
     *  2. Suspend
     *  3. Observe for state updates and return from load function
     */
    override suspend fun load(params: PagingSourceLoadParams<String>): PagingSourceLoadResult<String, Project> {
        val nextPagingKey = params.key

        val state = store.stateFlow
        withContext(Dispatchers.IO) {
            store.projectsList {
                launch {
                    loadPageFrom(repository, nextPagingKey).join()
                }
            }
        }

        log.debug("Load state projects size=${state.value.projectsPage.size}")

        return PagingSourceLoadResultPage(
            data = state.value.projectsPage,
            nextKey = state.value.nextPageUrl,
            prevKey = null,
        ) as PagingSourceLoadResult<String, Project>
    }
}