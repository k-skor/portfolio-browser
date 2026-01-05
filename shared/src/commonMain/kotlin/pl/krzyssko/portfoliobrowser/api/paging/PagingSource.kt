package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.util.Response
import kotlin.math.log

typealias PageKeyType = String

interface PagedContentLoader<T> {
    val pagingState: pl.krzyssko.portfoliobrowser.repository.PagingState
    suspend fun getContent(readFromStart: Boolean): Flow<Response<Map<PageKeyType?, List<T>>>>
}

@Suppress("CAST_NEVER_SUCCEEDS")
class MyPagingSource<T : Any>(
    private val contentLoader: PagedContentLoader<T>,
) : PagingSource<PageKeyType, T>(), KoinComponent {
    private val logging: Logging by inject()

    override fun getRefreshKey(state: PagingState<PageKeyType, T>): PageKeyType? = null
    //{
    //    val closestPage = state.closestPageToPosition(state.anchorPosition ?: 0)

    //    return closestPage?.nextKey?.let {
    //        it.ifBlank { null }
    //    } ?: closestPage?.prevKey
    //}

    /**
     *  1. Create intent to fetch repos with paging params
     *  2. Suspend
     *  3. Observe for state updates and return from load function
     */
    override suspend fun load(params: PagingSourceLoadParams<PageKeyType>): PagingSourceLoadResult<PageKeyType, T> {
        val nextPagingKey = params.key

        logging.debug("Loading page key=${nextPagingKey}")
        val data = contentLoader.getContent(nextPagingKey == null)
            .filter { it !is Response.Pending }
            .map {
                when (it) {
                    is Response.Ok -> it.data[nextPagingKey] ?: emptyList()
                    else -> emptyList()
                }
            }
            .first()

        logging.debug("Load page projects size=${data.size}")

        return PagingSourceLoadResultPage(
            data = data,
            nextKey = contentLoader.pagingState.paging.nextPageKey,
            prevKey = contentLoader.pagingState.paging.prevPageKey,
        ) as PagingSourceLoadResult<PageKeyType, T>
    }
}