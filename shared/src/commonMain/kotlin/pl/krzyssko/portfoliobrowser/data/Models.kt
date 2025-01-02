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

enum class Source {
    GitHub
}
data class Widget(val origin: Source = Source.GitHub, val name: String, val description: String?, val externalUrl: String)
data class Project(val id: Int, val name: String, val description: String?, val stack: List<Stack> = emptyList(), val image: Resource, val followers: List<String> = emptyList(), val createdBy: String, val createdOn: String, val coauthors: List<String> = emptyList(), val accessRestriction: List<String> = emptyList(), val source: Source? = null)

typealias AdditionalUserData = Map<String, Any>

data class Account(val id: String, val name: String, val email: String, val avatarUrl: String?, val isEmailVerified: Boolean) //, val projectsRefId: String? = null
data class SyncData(val uid: String, val timestamp: Long, val source: Source = Source.GitHub, val projectIds: List<Int>)

enum class SocialMediaType {
    LinkedIn,
    Facebook,
    Instagram
}

sealed class Contact {
    data class Phone(val number: String): Contact()
    data class Email(val address: String): Contact()
    data class SocialMedia(val type: SocialMediaType, val link: String): Contact()
    data class CustomLink(val title: String, val link: String): Contact()
}
enum class Role {
    All,
    Developer,
    Designer
}
data class Profile(val firstName: String? = null, val lastName: String? = null, val alias: String? = null, val role: Role? = null, val title: String? = null, val assets: List<String> = emptyList(), val experience: Int? = null, val location: String? = null, val contact: List<Contact> = emptyList())

data class Provider(val providerId: String, val uid: String, val name: String, val email: String, val photoUrl: String)
sealed class User {
    data object Guest: User()
    data class Authenticated(val account: Account, val additionalData: AdditionalUserData? = null, val token: String? = null, val signInMethod: String? = null): User()
}

data class Config(var gitHubApiUser: String = "", var gitHubApiToken: String = "", var lastSignInMethod: String = "")
