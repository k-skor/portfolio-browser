package pl.krzyssko.portfoliobrowser.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias GitHubLanguage = Map<String, Int>

@Serializable
data class GitHubProject(val id: Int, val name: String, val description: String?, val fork: Boolean, @SerialName("languages_url") val languagesUrl: String)

@Serializable
data class GitHubSearch(@SerialName("items") val projects: List<GitHubProject>)
