package pl.krzyssko.portfoliobrowser.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import pl.krzyssko.portfoliobrowser.platform.Configuration

class AndroidAzureTokenProvider(
    private val httpClient: HttpClient,
    private val configuration: Configuration
) : AzureTokenProvider {

    override val token: String
        get() = configuration.config.azureApiToken.orEmpty()

    override val refreshToken: String
        get() = configuration.config.azureApiRefreshToken.orEmpty()

    init {
        val config = configuration.config.copy(azureApiToken = null, azureApiRefreshToken = null)
        configuration.update(config)
    }

    override suspend fun initialize() {
        getAccessToken()
    }

    override suspend fun getAccessToken(): String? {
        val tenantId = configuration.config.azureTenantId ?: return null
        val clientId = configuration.config.azureClientId ?: return null
        val clientSecret = configuration.config.azureClientSecret ?: return null
        val scope = configuration.config.azureScope ?: return null

        val url = "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token"

        val res = httpClient.submitForm(
            url = url,
            formParameters = Parameters.build {
                append("client_id", clientId)
                append("scope", scope)
                append("client_secret", clientSecret)
                append("grant_type", "client_credentials")
            }
        )
        
        val response: AzureTokenResponse = res.body()

        configuration.update(configuration.config.copy(
            azureApiToken = response.accessToken,
            azureApiRefreshToken = response.refreshToken
        ))

        return response.accessToken
    }

    override suspend fun refreshToken(oldTokens: BearerTokens?, requestBlock: HttpRequestBuilder.() -> Unit): BearerTokens? {
        val tenantId = configuration.config.azureTenantId ?: return null
        val clientId = configuration.config.azureClientId ?: return null

        val url = "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token"

        val res = httpClient.submitForm(
            url = url,
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("client_id", clientId)
                append("refresh_token", oldTokens?.refreshToken ?: "")
            }
        ) { requestBlock() }
        
        val response: AzureTokenResponse = res.body()

        configuration.update(configuration.config.copy(
            azureApiToken = response.accessToken,
            azureApiRefreshToken = oldTokens?.refreshToken!!
        ))

        return BearerTokens(response.accessToken, oldTokens.refreshToken!!)
    }
}

actual fun getAzureTokenProvider(httpClient: HttpClient, configuration: Configuration): AzureTokenProvider =
    AndroidAzureTokenProvider(httpClient, configuration)