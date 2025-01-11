package pl.krzyssko.portfoliobrowser.db.transfer

import io.konform.validation.Validation
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.pattern
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
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
            if (it.startsWith("http")) Resource.NetworkResource(this.image) else Resource.LocalResource(this.image)
        }, // ?: throw IllegalArgumentException("image is required")
        followersCount = this.followersCount,
        followers = this.followers.map { dto -> dto.toFollower() },
        createdBy = this.createdBy ?: throw IllegalArgumentException("createdBy is required"),
        createdOn = this.createdOn ?: throw IllegalArgumentException("createdOn is required"),
        coauthors = this.coauthors,
        public = this.public,
        source = this.source?.let { dto -> Source.valueOf(dto) }
    )
}

fun Project.toDto(): ProjectDto {
    return ProjectDto(
        id = this.id,
        name = this.name,
        description = this.description,
        stack = this.stack.map { stack -> stack.toDto() },
        image = this.image?.let {
            when (it) {
                is Resource.LocalResource -> it.name
                is Resource.NetworkResource -> it.url
            }
        },
        followersCount = this.followersCount,
        followers = this.followers.map { follower -> follower.toDto() },
        createdBy = this.createdBy,
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
        this.name ?: "Anonymous user"
    )
}

fun Follower.toDto(): FollowerDto {
    return FollowerDto(this.uid, this.name)
}

val stackDtoValidation = Validation<StackDto> {
    StackDto::name required {
        pattern("^[a-zA-Z0-9 ]+\$") hint "Name must be alphanumeric"
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