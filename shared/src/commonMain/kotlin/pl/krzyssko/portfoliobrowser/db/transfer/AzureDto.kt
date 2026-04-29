package pl.krzyssko.portfoliobrowser.db.transfer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchDocDto(
    val id: String,
    val name: String,
    @SerialName("description_pl") val descriptionPl: String? = null,
    @SerialName("description_en") val descriptionEn: String? = null
)