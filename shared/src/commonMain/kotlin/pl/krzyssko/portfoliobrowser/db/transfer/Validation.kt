package pl.krzyssko.portfoliobrowser.db.transfer

import io.konform.validation.Validation
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.pattern
import pl.krzyssko.portfoliobrowser.data.Contact
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.ProfileRole
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.SocialMediaType
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.Stack

val projectDtoValidator = Validation<ProjectDto> {
    ProjectDto::name required {}
    //{
    //    pattern("^[a-zA-Z0-9 ]+\$") hint "Name must be alphanumeric"
    //}
    ProjectDto::description ifPresent {
        maxLength(250) hint "Description must be at most 250 characters"
    }
    //ProjectDto::image required {
    //    pattern("^https?://.*") hint "Image must be a valid URL"
    //}
    ProjectDto::createdBy required {
        pattern("^[a-zA-Z0-9]+$") hint "Created By must be alphanumeric"
    }
    //ProjectDto::createdOn required {
    //    pattern("^\\d{4}-\\d{2}-\\d{2}$") hint "Created On must be in the format YYYY-MM-DD"
    //}
}

fun ProjectDto.toProject(): Project {
    val validationResult = projectDtoValidator(this)
    if (validationResult.errors.isNotEmpty()) {
        throw IllegalArgumentException("Invalid ProjectDto: ${validationResult.errors}")
    }
    return Project(
        id = this.id ?: throw IllegalArgumentException("id is required"),
        name = this.name ?: throw IllegalArgumentException("name is required"),
        description = this.description,
        stack = this.stack.map { dto -> dto.toStack() },
        image = this.image?.let {
            if (it.startsWith("http")) Resource.NetworkResource(this.image) else Resource.LocalResource(this.image.toInt())
        }, // ?: throw IllegalArgumentException("image is required")
        followersCount = this.followersCount,
        followers = this.followers.map { dto -> dto.toFollower() },
        createdBy = this.createdBy ?: throw IllegalArgumentException("createdBy is required"),
        createdByName = this.createdByName ?: throw IllegalArgumentException("createdByName is required"),
        createdOn = this.createdOn ?: throw IllegalArgumentException("createdOn is required"),
        coauthors = this.coauthors,
        public = this.public,
        source = this.source?.let { dto -> Source.valueOf(dto) }
    )
}

fun Project.toPrivateData() = PrivateDataDto(
    roles = this.roles.associate { role -> role.uid to role.role.toString().lowercase() }
)

fun Project.toDto(): ProjectDto {
    return ProjectDto(
        id = this.id,
        name = this.name,
        namePartial = this.name.split("[a-zA-Z0-9]+".toRegex()),
        description = this.description,
        stack = this.stack.map { stack -> stack.toDto() },
        image = this.image?.let {
            when (it) {
                is Resource.LocalResource -> it.res.toString()
                is Resource.NetworkResource -> it.url
            }
        },
        followersCount = this.followersCount,
        followers = this.followers.map { follower -> follower.toDto() },
        createdBy = this.createdBy,
        createdByName = this.createdByName,
        createdOn = this.createdOn,
        coauthors = this.coauthors,
        public = this.public,
        source = this.source?.name
    )
}

val followerDtoValidation = Validation<FollowerDto> {
    FollowerDto::uid required {
        pattern("^[a-zA-Z0-9]+$") hint "uid must be alphanumeric"
    }
}

fun FollowerDto.toFollower(): Follower {
    val validationResult = followerDtoValidation(this)
    if (validationResult.errors.isNotEmpty()) {
        throw IllegalArgumentException("Invalid FollowerDto: ${validationResult.errors}")
    }
    return Follower(
        this.uid ?: throw IllegalArgumentException("uid is required"),
        this.name ?: throw IllegalArgumentException("name is required")
    )
}

fun Follower.toDto(): FollowerDto {
    return FollowerDto(this.uid, this.name)
}

val stackDtoValidation = Validation<StackDto> {
    StackDto::name required {
        pattern("^[a-zA-Z0-9 #+]+\$") hint "Name must be alphanumeric"
    }
}

fun StackDto.toStack(): Stack {
    val validationResult = stackDtoValidation(this)
    if (validationResult.errors.isNotEmpty()) {
        throw IllegalArgumentException("Invalid StackDto: ${validationResult.errors}")
    }
    return Stack(this.name ?: throw IllegalArgumentException("name is required"), this.percent ?: 0f)
}

fun Stack.toDto(): StackDto {
    return StackDto(this.name, this.percent)
}

val profileDtoValidation = Validation<ProfileDto> {
    ProfileDto::firstName required {
        pattern("^[a-zA-Z]*$") hint "First Name must be alphabetic"
    }
    ProfileDto::lastName required {
        pattern("^[a-zA-Z]*$") hint "Last Name must be alphabetic"
    }
    ProfileDto::role onEach {
        constrain("Role must be one of ${ProfileRole.entries.joinToString()}") { value ->
            ProfileRole.entries.map { it.name }.contains(value)
        }
    }
    ProfileDto::avatarUrl ifPresent {
        pattern("^https?://.*") hint "Avatar URL must be a valid URL"
    }
    ProfileDto::title ifPresent {
        pattern("^[a-zA-Z0-9 ]+\$") hint "Title must be alphanumeric"
    }
    ProfileDto::about ifPresent  {
        maxLength(20) hint "About must be at most 20 characters long"
    }
    ProfileDto::experience required {}
}

fun ProfileDto.toProfile(): Profile {
    val validationResult = profileDtoValidation(this)
    if (validationResult.errors.isNotEmpty()) {
        throw IllegalArgumentException("Invalid ProfileDto: ${validationResult.errors}")
    }
    return Profile(
        firstName = this.firstName ?: throw IllegalArgumentException("firstName is required"),
        lastName = this.lastName ?: throw IllegalArgumentException("lastName is required"),
        alias = this.alias,
        role = this.role.map { role -> ProfileRole.valueOf(role) },
        avatarUrl = this.avatarUrl,
        title = this.title,
        about = this.about, // ?: throw IllegalArgumentException("about is required")
        assets = this.assets,
        experience = this.experience ?: throw IllegalArgumentException("experience is required"),
        location = this.location ?: throw IllegalArgumentException("location is required"),
        contact = this.contact.map { (key, value) ->
            when (key) {
                "tel" -> Contact.Phone(value)
                "email" -> Contact.Email(value)
                "li" -> Contact.SocialMedia(SocialMediaType.LinkedIn, value)
                "fb" -> Contact.SocialMedia(SocialMediaType.Facebook, value)
                "ig" -> Contact.SocialMedia(SocialMediaType.Instagram, value)
                else -> Contact.CustomLink(key, value)
            }
        }
    )
}

fun Profile.toDto(): ProfileDto {
    return ProfileDto(
        firstName = this.firstName,
        lastName = this.lastName,
        alias = this.alias,
        role = this.role.map { role -> role.name },
        avatarUrl = this.avatarUrl,
        title = this.title,
        about = this.about,
        assets = this.assets,
        experience = this.experience,
        location = this.location,
        contact = this.contact.associate { contact ->
            when (contact) {
                is Contact.Phone -> "tel" to contact.number
                is Contact.Email -> "email" to contact.address
                is Contact.SocialMedia -> when (contact.type) {
                    SocialMediaType.LinkedIn -> "li" to contact.link
                    SocialMediaType.Facebook -> "fb" to contact.link
                    SocialMediaType.Instagram -> "ig" to contact.link
                }
                is Contact.CustomLink -> contact.title to contact.link
            }
        }
    )
}
