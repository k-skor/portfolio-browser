package pl.krzyssko.portfoliobrowser.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AzureSearchRequest(
    val search: String,
    val count: Boolean = true,
    val top: Int? = null,
    val skip: Int? = null,
    val filter: String? = null
)

@Serializable
data class AzureSearchResponse<T>(
    @SerialName("@odata.count") val count: Int? = null,
    val value: List<T>
)