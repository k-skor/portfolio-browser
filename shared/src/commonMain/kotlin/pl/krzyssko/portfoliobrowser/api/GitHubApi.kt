package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.headers
import io.ktor.http.parseQueryString
import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.paging.LinkHeaderPageParser
import pl.krzyssko.portfoliobrowser.api.paging.PagingKey
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging

class GitHubApi(private val httpClient: HttpClient, configuration: Configuration) : Api {

    companion object Urls {
        const val BASE_URL = "https://api.github.com"
    }

    private val user: String = configuration.gitHubApiUser
    private val token: String = configuration.gitHubApiToken
    private val apiVersion = "2022-11-28"

    private val log = getLogging()

    private fun getHttpRequestBuilderBlock(builder: HttpRequestBuilder, path: String, block: HttpRequestBuilder.() -> Unit) {
        with(builder) {
            url( Url("$BASE_URL/$path") )
            headers {
                accept(ContentType("application", "vnd.github+json"))
                bearerAuth(token)
                append("X-GitHub-Api-Version", apiVersion)
            }
            block()
        }
    }

    override suspend fun getRepos(page: Number): List<GitHubProject> {
        val request = httpClient.get {
            getHttpRequestBuilderBlock(this, "users/$user/repos") {
                url.parameters.apply {
                    append("per_page", "5")
                    append("page", page.toString())
                }
            }
        }
        val result = request.body<List<GitHubProject>>()

        log.debug(result.toString())

        return result
    }

    override suspend fun getRepos(query: String?): PagedResponse<GitHubProject> {
        val request = httpClient.get {
            getHttpRequestBuilderBlock(this, "users/$user/repos") {
                if (query == null) {
                    url.parameters.apply {
                        append("per_page", "5")
                        append("page", "1")
                    }
                } else {
                    val startIndex = query.indexOfFirst { c -> c == '?' }
                    url.parameters.apply {
                        appendAll(parseQueryString(query, startIndex + 1))
                    }
                }
                log.debug("Requesting URL $url")
            }
        }
        val result = request.body<List<GitHubProject>>()

        val paging = LinkHeaderPageParser()
        if (request.headers.contains("link")) {
            log.debug("Raw link=${request.headers["link"]}")
            paging.parse(request.headers["link"]!!)
        } else {
            log.debug("No link headers")
        }

        return PagedResponse(result, paging.get(PagingKey.Rel.Prev), paging.get(PagingKey.Rel.Next), paging.get(PagingKey.Rel.Last))
    }

    override suspend fun getRepoLanguages(repoName: String): GitHubLanguage {
        val result = httpClient.get {
            getHttpRequestBuilderBlock(this, "repos/$user/$repoName/languages") {}
        }.body<GitHubLanguage>()

        log.debug(result.toString())

        return result
    }

    override suspend fun getRepoBy(name: String): GitHubProject {
        val result = httpClient.get {
            getHttpRequestBuilderBlock(this, "repos/$user/$name") {}
        }.body<GitHubProject>()

        log.debug(result.toString())

        return result
    }
}