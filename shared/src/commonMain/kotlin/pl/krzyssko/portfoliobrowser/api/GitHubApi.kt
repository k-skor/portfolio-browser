package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headers
import io.ktor.http.parseQueryString
import pl.krzyssko.portfoliobrowser.api.dto.GitHubLanguage
import pl.krzyssko.portfoliobrowser.api.dto.GitHubProject
import pl.krzyssko.portfoliobrowser.api.dto.GitHubSearch
import pl.krzyssko.portfoliobrowser.api.dto.GitHubUser
import pl.krzyssko.portfoliobrowser.api.paging.LinkHeaderPageParser
import pl.krzyssko.portfoliobrowser.api.paging.PagingKey
import pl.krzyssko.portfoliobrowser.platform.Configuration
import pl.krzyssko.portfoliobrowser.platform.getLogging

data class PagedResponse<out T>(
    val data: List<T>,
    val prev: String? = null,
    val next: String? = null,
    val last: String? = null
)

data class PagedSearchResult<out T>(
    val data: T,
    val prev: String? = null,
    val next: String? = null,
    val last: String? = null
)

interface Api {
    suspend fun getUser(): GitHubUser
    suspend fun getRepos(): List<GitHubProject>
    suspend fun getRepos(queryParams: String?): PagedResponse<GitHubProject>
    suspend fun getRepoLanguages(repoName: String): GitHubLanguage
    suspend fun getRepoBy(name: String): GitHubProject
    suspend fun searchRepos(query: String, queryParams: String?): PagedSearchResult<GitHubSearch>
}

class GitHubApiException(message: String?, throwable: Throwable?): Exception(message, throwable)

class GitHubApi(private val httpClient: HttpClient, private val configuration: Configuration) : Api {

    companion object Urls {
        const val BASE_URL = "https://api.github.com"
    }

    private val apiVersion = "2022-11-28"

    private val log = getLogging()

    private fun getHttpRequestBuilderBlock(builder: HttpRequestBuilder, path: String, block: HttpRequestBuilder.() -> Unit) {
        val token = configuration.config.gitHubApiToken
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

    override suspend fun getUser(): GitHubUser {
        var error: Throwable? = null
        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "user") {}
            }
            if (request.status == HttpStatusCode.OK) {
                val result = request.body<GitHubUser>()

                log.debug(result.toString())

                result
            } else null
        } catch (e: Exception) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }

    override suspend fun getRepos(): List<GitHubProject> {
        val user = configuration.config.gitHubApiUser
        var error: Throwable? = null
        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "users/$user/repos") {}
            }
            if (request.status == HttpStatusCode.OK) {
                val result = request.body<List<GitHubProject>>()

                log.debug(result.toString())

                result
            } else null
        } catch (e: Exception) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }

    override suspend fun getRepos(queryParams: String?): PagedResponse<GitHubProject> {
        val user = configuration.config.gitHubApiUser
        var error: Throwable? = null
        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "users/$user/repos") {
                    if (queryParams == null) {
                        url.parameters.apply {
                            append("per_page", "5")
                            append("page", "1")
                        }
                    } else {
                        val startIndex = queryParams.indexOfFirst { c -> c == '?' }
                        url.parameters.apply {
                            appendAll(parseQueryString(queryParams, startIndex + 1))
                        }
                    }
                    log.debug("Requesting URL $url")
                }
            }
            if (request.status == HttpStatusCode.OK) {
                val result = request.body<List<GitHubProject>>()

                val paging = LinkHeaderPageParser()
                if (request.headers.contains("link")) {
                    log.debug("Raw link=${request.headers["link"]}")
                    paging.parse(request.headers["link"]!!)
                } else {
                    log.debug("No link headers")
                }

                PagedResponse(result, paging.get(PagingKey.Rel.Prev), paging.get(PagingKey.Rel.Next), paging.get(PagingKey.Rel.Last))
            } else null
        } catch (e: Exception) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }

    override suspend fun getRepoLanguages(repoName: String): GitHubLanguage {
        val user = configuration.config.gitHubApiUser
        //var result: GitHubLanguage? = null
        var error: Throwable? = null
        //try {
        //    val request = httpClient.get {
        //        getHttpRequestBuilderBlock(this, "repos/$user/$repoName/languages") {}
        //    }
        //    if (request.status == HttpStatusCode.OK) {
        //        result = request.body<GitHubLanguage>()

        //        log.debug(result.toString())
        //    }
        //} catch (e: Exception) {
        //    throwable = e
        //} finally {
        //    if (result == null) {
        //        throw GitHubApiException("GitHub API exception.", throwable)
        //    }
        //    return result
        //}
        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "repos/$user/$repoName/languages") {}
            }
            if (request.status == HttpStatusCode.OK) {
                val result = request.body<GitHubLanguage>()

                log.debug(result.toString())

                result
            } else null
        } catch (e: Exception) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }

    override suspend fun getRepoBy(name: String): GitHubProject {
        val user = configuration.config.gitHubApiUser
        var error: Throwable? = null

        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "repos/$user/$name") {}
            }
            if (request.status == HttpStatusCode.OK) {
                val result = request.body<GitHubProject>()
                log.debug(result.toString())
                result
            } else null
        } catch (e: Exception) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }

    /**
     * Query example: android in:name in:description user:k-skor
     */
    override suspend fun searchRepos(query: String, queryParams: String?): PagedSearchResult<GitHubSearch> {
        var error: Throwable? = null

        val result = try {
            val request = httpClient.get {
                getHttpRequestBuilderBlock(this, "search/repositories") {
                    url.parameters.apply {
                        appendAll(parseQueryString(query))
                    }
                    if (queryParams == null) {
                        url.parameters.apply {
                            append("per_page", "5")
                            append("page", "1")
                        }
                    } else {
                        val startIndex = queryParams.indexOfFirst { c -> c == '?' }
                        url.parameters.apply {
                            appendAll(parseQueryString(queryParams, startIndex + 1))
                        }
                    }
                    log.debug("Requesting URL $url")
                }
            }

            if (request.status == HttpStatusCode.OK) {

                val result = request.body<GitHubSearch>()

                val paging = LinkHeaderPageParser()
                if (request.headers.contains("link")) {
                    log.debug("Raw link=${request.headers["link"]}")
                    paging.parse(request.headers["link"]!!)
                } else {
                    log.debug("No link headers")
                }

                PagedSearchResult(
                    result,
                    paging.get(PagingKey.Rel.Prev),
                    paging.get(PagingKey.Rel.Next),
                    paging.get(PagingKey.Rel.Last)
                )
            } else null

        } catch (e: Error) {
            error = e
            null
        } ?: throw GitHubApiException("GitHub API exception.", error)

        return result
    }
}