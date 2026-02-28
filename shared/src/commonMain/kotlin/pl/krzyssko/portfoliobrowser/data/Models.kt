package pl.krzyssko.portfoliobrowser.data

sealed class Resource {
    data class LocalResource(val res: Int): Resource()
    data class NetworkResource(val url: String): Resource()
}

data class Stack(val name: String, val percent: Float = 0f, val color: Int = 0x00FFFFFF)

enum class Source {
    GitHub
}
enum class Role {
    Owner,
    Editor,
    Reader
}
data class Widget(
    val origin: Source = Source.GitHub,
    val name: String,
    val description: String?,
    val externalUrl: String
)

data class Follower(val uid: String, val name: String)
data class AccessRole(val uid: String, val role: Role)
data class Project(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val stack: List<Stack> = emptyList(),
    val image: Resource? = null,
    val followersCount: Int = 0,
    val followers: List<Follower> = emptyList(),
    val favorite: Boolean = false,
    val createdBy: String = "",
    val createdByName: String = "",
    val createdOn: Long = 0L,
    val coauthors: List<String> = emptyList(),
    val public: Boolean = true,
    val source: Source? = null,
    val roles: List<AccessRole> = emptyList()
)

data class FilterParams(
    val query: String,
    val stack: String?,
    val featured: Boolean
)

typealias AdditionalUserData = Map<String, Any>

data class Account(
    val id: String,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val isEmailVerified: Boolean,
    val anonymous: Boolean
)

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
enum class ProfileRole {
    Developer,
    Designer,
    Other
}

data class Provider(
    val providerId: String,
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?
)

sealed class User {
    data object Guest : User()
    data class Authenticated(
        val account: Account,
        val additionalData: AdditionalUserData? = null,
        val oauthAccessToken: String? = null,
        val signInMethod: String? = null,
        val identityProviders: List<Provider> = emptyList()
    ) : User()
}

sealed class Profile {
    data object Stub : Profile()
    data class Loaded(
        val firstName: String,
        val lastName: String,
        val alias: String? = null,
        val role: List<ProfileRole> = listOf(ProfileRole.Other),
        val avatarUrl: String? = null,
        val title: String? = null,
        val about: String? = null,
        val assets: List<String> = emptyList(),
        val experience: Int,
        val location: String,
        val contact: List<Contact> = emptyList()
    ) : Profile()

    companion object {
        val DEFAULT = Loaded(
            firstName = "Krzysztof",
            lastName = "Skorcz",
            title = "apps for Android",
            role = listOf(ProfileRole.Developer),
            about = "I'm a developer...",
            experience = 10,
            location = "Poznań, Poland",
        )
    }
}

fun Project.canEdit(uid: String): Boolean = createdBy == uid
fun Int.toExperience(): String = when {
    this == 0 -> "Less than 1"
    this >= 10 -> "10+"
    this >= 15 -> "15+"
    this >= 20 -> "20+"
    else -> this.toString()
}
