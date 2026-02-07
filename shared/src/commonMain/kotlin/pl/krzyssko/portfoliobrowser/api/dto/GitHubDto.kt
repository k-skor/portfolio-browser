package pl.krzyssko.portfoliobrowser.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias GitHubLanguage = Map<String, Int>

@Serializable
data class GitHubUser(
    val id: Int,
    val login: String,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("public_repos") val totalPublicRepos: Int = 0,
    @SerialName("total_private_repos") val totalPrivateRepos: Int = 0
)

@Serializable
data class GitHubProject(val id: Int, val name: String, val description: String?, val fork: Boolean, @SerialName("languages_url") val languagesUrl: String, val private: Boolean, @SerialName("html_url") val externalLink: String)

@Serializable
data class GitHubSearch(@SerialName("items") val projects: List<GitHubProject>)
