package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.krzyssko.portfoliobrowser.platform.Configuration

@Serializable
data class AzureTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

interface AzureTokenProvider {
    val token: String
    val refreshToken: String
    suspend fun initialize()
    suspend fun getAccessToken(): String?
    suspend fun refreshToken(oldTokens: BearerTokens?, requestBlock: HttpRequestBuilder.() -> Unit): BearerTokens?
}

expect fun getAzureTokenProvider(httpClient: HttpClient, configuration: Configuration): AzureTokenProvider
