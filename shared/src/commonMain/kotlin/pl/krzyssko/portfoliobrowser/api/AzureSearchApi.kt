package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import pl.krzyssko.portfoliobrowser.api.dto.AzureSearchRequest
import pl.krzyssko.portfoliobrowser.api.dto.AzureSearchResponse
import pl.krzyssko.portfoliobrowser.db.transfer.SearchDocDto
import pl.krzyssko.portfoliobrowser.platform.Configuration

class AzureSearchApi(private val httpClient: HttpClient, private val configuration: Configuration) : AzureApi {

    private val apiVersion = "2025-09-01"
    
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun searchProjects(
        searchPhrase: String,
        top: Int?,
        skip: Int?,
        filter: String?
    ): AzureSearchResponse<SearchDocDto> {
        val endpoint = configuration.config.azureSearchEndpoint ?: throw Exception("Missing azureSearchEndpoint")
        val index = configuration.config.azureSearchIndex ?: throw Exception("Missing azureSearchIndex")

        val url = "$endpoint/indexes('$index')/docs/search.post.search?api-version=$apiVersion"

        val requestBody = AzureSearchRequest(
            search = searchPhrase,
            top = top,
            skip = skip,
            filter = filter
        )

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        return if (response.status == HttpStatusCode.OK) {
            val text = response.bodyAsText()
            json.decodeFromString<AzureSearchResponse<SearchDocDto>>(text)
        } else {
            AzureSearchResponse(0, emptyList())
        }
    }
}