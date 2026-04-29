package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.api.AzureApi
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.db.transfer.toSearchDoc
import pl.krzyssko.portfoliobrowser.platform.getLogging

class AzureSearchException(message: String? = null, throwable: Throwable? = null): Exception(message, throwable)

class AzureSearchPagingState(
    val nextSkip: Int? = null,
    override val pageSize: Int = 5,
    override val paging: Paging = Paging()
): PagingState

class AzureSearchRepository(
    private val azureApi: AzureApi,
    private val repository: ProjectRepository
) : SearchRepository {

    private var azureSearchPagingState = AzureSearchPagingState(null, paging = Paging())

    override val searchPagingState: PagingState
        get() = azureSearchPagingState

    override fun resetSearchPagingState() {
        azureSearchPagingState = AzureSearchPagingState(null, paging = Paging())
    }

    override suspend fun nextSearchPage(query: String, nextPageKey: Any?): Result<List<Project>> {
        val skip = (nextPageKey as? Int) ?: 0
        
        return runCatching {
            val response = azureApi.searchProjects(
                searchPhrase = query,
                top = azureSearchPagingState.pageSize,
                skip = skip
            )

            getLogging().debug("nextSearchPage: search=$response")
            repository.fetchProjectsByIds(response.value.map { it.toSearchDoc().id }).getOrThrow()
        }
    }
}
