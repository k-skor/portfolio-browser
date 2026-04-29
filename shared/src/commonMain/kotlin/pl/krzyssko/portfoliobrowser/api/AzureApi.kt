package pl.krzyssko.portfoliobrowser.api

import pl.krzyssko.portfoliobrowser.api.dto.AzureSearchResponse
import pl.krzyssko.portfoliobrowser.db.transfer.SearchDocDto

interface AzureApi {
    suspend fun searchProjects(
        searchPhrase: String,
        top: Int? = null,
        skip: Int? = null,
        filter: String? = null
    ): AzureSearchResponse<SearchDocDto>
}
