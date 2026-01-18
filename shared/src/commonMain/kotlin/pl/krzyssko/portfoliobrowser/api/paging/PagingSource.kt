package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultError
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

@Suppress("CAST_NEVER_SUCCEEDS")
class MyPagingSource<T : Any>(
    private val repository: ProjectRepository,
    private val query: String,
    private val categories: List<String>,
    private val featured: Boolean
) : PagingSource<Any, T>(), KoinComponent {
    private val logging: Logging by inject()

    override fun getRefreshKey(state: PagingState<Any, T>): Any? = null
    //{
    //    val closestPage = state.closestPageToPosition(state.anchorPosition ?: 0)

    //    logging.debug("getRefreshKey: closest page=$closestPage")
    //    return closestPage?.nextKey?.let {
    //        it.ifBlank { null }
    //    } ?: closestPage?.prevKey
    //}

    /**
     *  1. Create intent to fetch repos with paging params
     *  2. Suspend
     *  3. Observe for state updates and return from load function
     */
    override suspend fun load(params: PagingSourceLoadParams<Any>): PagingSourceLoadResult<Any, T> {
        val pageKey = params.key

        logging.debug("Loading page key=${pageKey}")
        return try {
            val response = withContext(Dispatchers.IO) {
                when {
                    query.isNotBlank() -> repository.nextSearchPage(query, pageKey)
                    featured -> repository.nextFavoritePage(pageKey)
                    else -> repository.nextPage(pageKey)
                }
            }

            logging.debug("Load page projects size=${response.getOrNull()?.size}")

            PagingSourceLoadResultPage(
                data = response.getOrNull() ?: emptyList(),
                prevKey = null,
                nextKey = repository.pagingState.paging.nextPageKey
            ) as PagingSourceLoadResult<Any, T>
        } catch (e: Exception) {
            PagingSourceLoadResultError<Any, List<Project>>(e) as PagingSourceLoadResult<Any, T>
        }
    }
}