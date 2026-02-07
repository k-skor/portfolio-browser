package pl.krzyssko.portfoliobrowser.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias GitHubLanguage = Map<String, Int>

@Serializable
data class GitHubUser(val id: Int, val login: String, val email: String, /*@SerialName("public_repos") val totalPublicRepos: Int, @SerialName("total_private_repos") val totalPrivateRepos: Int*/)

@Serializable
data class GitHubProject(val id: Int, val name: String, val description: String?, val fork: Boolean, @SerialName("languages_url") val languagesUrl: String, val private: Boolean, @SerialName("html_url") val externalLink: String)

@Serializable
data class GitHubSearch(@SerialName("items") val projects: List<GitHubProject>)
