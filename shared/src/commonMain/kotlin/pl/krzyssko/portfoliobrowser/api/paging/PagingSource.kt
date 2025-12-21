package pl.krzyssko.portfoliobrowser.api.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.util.Response

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

    /**
     *  1. Create intent to fetch repos with paging params
     *  2. Suspend
     *  3. Observe for state updates and return from load function
     */
    override suspend fun load(params: PagingSourceLoadParams<PageKeyType>): PagingSourceLoadResult<PageKeyType, T> {
        val nextPagingKey = params.key

        val data = contentLoader.getContent(nextPagingKey == null)
            .map {
                when (it) {
                    is Response.Ok -> it.data[nextPagingKey]
                    is Response.Error -> emptyList()
                    else -> null
                }
            }
            .filterNotNull()
            .first()

        logging.debug("Load state projects size=${data.size}")

        return PagingSourceLoadResultPage(
            data = data,
            nextKey = contentLoader.pagingState.paging.nextPageKey,
            prevKey = contentLoader.pagingState.paging.prevPageKey,
        ) as PagingSourceLoadResult<PageKeyType, T>
    }
}