package pl.krzyssko.portfoliobrowser.data

sealed class Resource {
    data class LocalResource(val name: String): Resource()
    data class NetworkResource(val url: String): Resource()
}

data class Paging(
    val currentPageUrl: String? = null,
    val nextPageUrl: String? = null,
    val prevPageUrl: String? = null,
    val isLastPage: Boolean = false
)

data class Stack(val name: String, val lines: Int, val color: Int = 0x00FFFFFF)

data class Project(val id: Int, val name: String, val description: String?, val stack: List<Stack> = emptyList(), val icon: Resource)

typealias AdditionalUserData = Map<String, Any>

data class Profile(val id: String, val name: String, val email: String, val photoUrl: String?, val isEmailVerified: Boolean)

data class User(val profile: Profile, val additionalData: AdditionalUserData? = null, val token: String)

data class Config(var gitHubApiUser: String, var gitHubApiToken: String)
