package pl.krzyssko.portfoliobrowser.data

import pl.krzyssko.portfoliobrowser.db.transfer.DataSyncDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto

sealed class Resource {
    data class LocalResource(val name: String): Resource()
    data class NetworkResource(val url: String): Resource()
}

data class Paging(
    val pageKey: String? = null,
    val nextPageKey: String? = null,
    val prevPageKey: String? = null,
    val isLastPage: Boolean = false
)

data class Stack(val name: String, val percent: Float = 0f, val color: Int = 0x00FFFFFF)

enum class Source {
    GitHub
}
data class Widget(val origin: Source = Source.GitHub, val name: String, val description: String?, val externalUrl: String)
data class Follower(val uid: String, val name: String)
data class Project(val id: Int, val name: String, val description: String? = null, val stack: List<Stack> = emptyList(), val image: Resource?, val followersCount: Int = 0, val followers: List<Follower> = emptyList(), val createdBy: String, val createdOn: Long, val coauthors: List<String> = emptyList(), val public: Boolean = true, val source: Source? = null)

typealias AdditionalUserData = Map<String, Any>

data class Account(val id: String, val name: String, val email: String, val avatarUrl: String?, val isEmailVerified: Boolean) //, val projectsRefId: String? = null

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

data class Provider(val providerId: String, val uid: String, val name: String, val email: String, val photoUrl: String)
sealed class User {
    data object Guest: User()
    data class Authenticated(val account: Account, val additionalData: AdditionalUserData? = null, val token: String? = null, val signInMethod: String? = null): User()
}

data class Config(var gitHubApiUser: String = "", var gitHubApiToken: String = "", var lastSignInMethod: String = "")

typealias DataSync = DataSyncDto
typealias Profile = ProfileDto
