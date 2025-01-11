package pl.krzyssko.portfoliobrowser.db.transfer

data class StackDto(val name: String? = null, val percent: Float? = null)
data class FollowerDto(val uid: String? = null, val name: String? = null)
data class ProjectDto(val id: Int? = null, val name: String? = null, val description: String? = null, val stack: List<StackDto> = emptyList(), val image: String? = null, val followersCount: Int = 0, val followers: List<FollowerDto> = emptyList(), val createdBy: String? = null, val createdOn: Long? = null, val coauthors: List<String> = emptyList(), val public: Boolean = true, val source: String? = null)
data class ProfileDto(val firstName: String? = null, val lastName: String? = null, val alias: String? = null, val role: String? = null, val title: String? = null, val assets: List<String> = emptyList(), val experience: Int? = null, val location: String? = null, val contact: List<String> = emptyList())
data class DataSyncDto(val uid: String? = null, val timestamp: Long? = null, val source: String? = null, val projectIds: List<Int> = emptyList())
