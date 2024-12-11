package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.krzyssko.portfoliobrowser.InfiniteColorPicker
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository
import pl.krzyssko.portfoliobrowser.store.OrbitStore
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.loadPageFrom
import pl.krzyssko.portfoliobrowser.store.projectsList
import pl.krzyssko.portfoliobrowser.store.searchProjects

//@Suppress("CAST_NEVER_SUCCEEDS")
class MyPagingSource(
    private val repository: ProjectRepository,
    private val colorPicker: InfiniteColorPicker,
    private val store: OrbitStore<ProjectsListState>
) : PagingSource<String, Project>(), KoinComponent {
    private val logging: Logging by inject()

    override fun getRefreshKey(state: PagingState<String, Project>): String? = null

    /**
     *  1. Create intent to fetch repos with paging params
     *  2. Suspend
     *  3. Observe for state updates and return from load function
     */
    override suspend fun load(params: PagingSourceLoadParams<String>): PagingSourceLoadResult<String, Project> = withContext(Dispatchers.Default) {
        val nextPagingKey = params.key

        val state = store.stateFlow
        val phrase = (state.value as? ProjectsListState.Ready)?.searchPhrase
        store.projectsList {
            if (!phrase.isNullOrEmpty()) {
                searchProjects(repository, colorPicker, nextPagingKey)
            } else {
                loadPageFrom(repository, colorPicker, nextPagingKey)
            }
        }
        val page = state.onEach { logging.debug("loading page new state!!!") }
            .filter { it is ProjectsListState.Ready }.map { it as ProjectsListState.Ready }
            .filter { it.paging.currentPageUrl == nextPagingKey }
            .filter { it.projects.isNotEmpty() }
            .first()

        logging.debug("Load state projects size=${page.projects[nextPagingKey]?.size}")

        PagingSourceLoadResultPage(
            data = page.projects[nextPagingKey] ?: emptyList(),
            nextKey = page.paging.nextPageUrl,
            prevKey = page.paging.prevPageUrl,
        ) as PagingSourceLoadResult<String, Project>
    }
}