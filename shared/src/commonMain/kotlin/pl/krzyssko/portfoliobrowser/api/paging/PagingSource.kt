package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultError
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.repository.ProjectRepository

typealias PageKeyType2 = Any

@Suppress("CAST_NEVER_SUCCEEDS")
class MyPagingSource<T : Any>(
    private val repository: ProjectRepository,
    private val query: String,
    private val stack: String?,
    private val featured: Boolean
) : PagingSource<PageKeyType2, T>(), KoinComponent {
    private val logging: Logging by inject()

    override fun getRefreshKey(state: PagingState<PageKeyType2, T>): PageKeyType2? = null
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
    override suspend fun load(params: PagingSourceLoadParams<PageKeyType2>): PagingSourceLoadResult<PageKeyType2, T> {
        val pageKey = params.key

        logging.debug("Loading page key=${pageKey}")
        return try {
            val response = when {
                query.isNotBlank() -> repository.nextSearchPage(query, pageKey)
                featured -> repository.nextFavoritePage(pageKey)
                else -> repository.nextPage(pageKey)
            }

            logging.debug("Load page projects size=${response.getOrNull()?.size}")

            PagingSourceLoadResultPage(
                data = response.getOrNull() ?: emptyList(),
                prevKey = null,
                nextKey = repository.pagingState.paging.nextPageKey
            ) as PagingSourceLoadResult<PageKeyType2, T>
        } catch (e: Exception) {
            PagingSourceLoadResultError<PageKeyType2, List<Project>>(e) as PagingSourceLoadResult<PageKeyType2, T>
        }
    }
}