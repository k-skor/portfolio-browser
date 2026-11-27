package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.HttpRequestBuilder
import pl.krzyssko.portfoliobrowser.platform.Configuration

class IosAzureTokenProvider : AzureTokenProvider {
    override val token: String get() = TODO("Not yet implemented")
    override val refreshToken: String get() = TODO("Not yet implemented")
    override suspend fun initialize(): Unit = TODO("Not yet implemented")
    override suspend fun getAccessToken(): String? = TODO("Not yet implemented")
    override suspend fun refreshToken(
        oldTokens: BearerTokens?,
        requestBlock: HttpRequestBuilder.() -> Unit
    ): BearerTokens? = TODO("Not yet implemented")
}

actual fun getAzureTokenProvider(httpClient: HttpClient, configuration: Configuration): AzureTokenProvider =
    IosAzureTokenProvider()
