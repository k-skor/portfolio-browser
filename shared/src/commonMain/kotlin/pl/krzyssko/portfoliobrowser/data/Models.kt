package pl.krzyssko.portfoliobrowser.data

import pl.krzyssko.portfoliobrowser.db.transfer.DataSyncDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto

sealed class Resource {
    data class LocalResource(val res: Int): Resource()
    data class NetworkResource(val url: String): Resource()
}

data class Stack(val name: String, val percent: Float = 0f, val color: Int = 0x00FFFFFF)

enum class Source {
    GitHub
}
data class Widget(
    val origin: Source = Source.GitHub,
    val name: String,
    val description: String?,
    val externalUrl: String
)

data class Follower(val uid: String, val name: String)
data class Project(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val stack: List<Stack> = emptyList(),
    val image: Resource? = null,
    val followersCount: Int = 0,
    val followers: List<Follower> = emptyList(),
    val createdBy: String = "",
    val createdByName: String = "",
    val createdOn: Long = 0L,
    val coauthors: List<String> = emptyList(),
    val public: Boolean = true,
    val source: Source? = null
)

typealias AdditionalUserData = Map<String, Any>

data class Account(
    val id: String,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val isEmailVerified: Boolean,
    val anonymous: Boolean
) //, val projectsRefId: String? = null

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

data class Provider(
    val providerId: String,
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String
)

sealed class User {
    data object None : User()
    data object Guest : User()
    data class Authenticated(
        val account: Account,
        val additionalData: AdditionalUserData? = null,
        val token: String? = null,
        val signInMethod: String? = null
    ) : User()
}

data class Config(
    val gitHubApiUser: String? = null,
    val gitHubApiToken: String? = null,
    val lastSignInMethod: String? = null
)

typealias DataSync = DataSyncDto
typealias Profile = ProfileDto

fun Project.canEdit(uid: String): Boolean = createdBy == uid
fun Int.toExperience(): String = when {
    this == 0 -> "Less than 1"
    this >= 10 -> "10+"
    this >= 15 -> "15+"
    this >= 20 -> "20+"
    else -> this.toString()
}