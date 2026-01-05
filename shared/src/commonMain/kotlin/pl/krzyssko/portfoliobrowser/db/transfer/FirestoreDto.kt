package pl.krzyssko.portfoliobrowser.db.transfer

data class StackDto(val name: String? = null, val percent: Float? = null)
data class FollowerDto(val uid: String? = null, val name: String? = null)
data class ProjectDto(
    val id: String? = null,
    val name: String? = null,
    val namePartial: List<String> = emptyList(),
    val description: String? = null,
    val stack: List<StackDto> = emptyList(),
    val image: String? = null,
    val followersCount: Int = 0,
    val followers: List<FollowerDto> = emptyList(),
    val createdBy: String? = null,
    val createdByName: String? = null,
    val createdOn: Long? = null,
    val coauthors: List<String> = emptyList(),
    val public: Boolean = true,
    val source: String? = null
)
data class PrivateDataDto(val roles: Map<String, String>)

data class ProfileDto(
    val firstName: String? = null,
    val lastName: String? = null,
    val alias: String? = null,
    val role: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val title: String? = null,
    val about: String? = null,
    val assets: List<String> = emptyList(),
    val experience: Int? = null,
    val location: String? = null,
    val contact: Map<String, String> = emptyMap()
)

data class DataSyncDto(
    val uid: String? = null,
    val timestamp: Long? = null,
    val source: String? = null,
    val projectIds: List<String> = emptyList()
)
