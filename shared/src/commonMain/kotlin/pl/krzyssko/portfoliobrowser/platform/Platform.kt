package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault

interface Platform {
    val name: String
}

data class Config(
    val gitHubApiUser: String? = null,
    val gitHubApiToken: String? = null,
    val lastSignInMethod: String? = null,
    val azureSearchEndpoint: String? = null,
    val azureSearchIndex: String? = null,
    val azureTenantId: String? = null,
    val azureClientId: String? = null,
    val azureClientSecret: String? = null,
    val azureApiToken: String? = null,
    val azureApiRefreshToken: String? = null,
    val azureScope: String? = null
)

abstract class Configuration {
    abstract val vault: KVault
    abstract var config: Config

    fun update(config: Config) {
        this.config = config
    }

    fun save(config: Config) {
        config.gitHubApiUser?.let { vault.set("$KEY_CONFIG.gitHubApiUser", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiUser")
        config.gitHubApiToken?.let { vault.set("$KEY_CONFIG.gitHubApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiToken")
        config.lastSignInMethod?.let { vault.set("$KEY_CONFIG.lastSignInMethod", it) } ?: vault.deleteObject("$KEY_CONFIG.lastSignInMethod")
        config.azureSearchEndpoint?.let { vault.set("$KEY_CONFIG.azureSearchEndpoint", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchEndpoint")
        config.azureSearchIndex?.let { vault.set("$KEY_CONFIG.azureSearchIndex", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchIndex")
        config.azureTenantId?.let { vault.set("$KEY_CONFIG.azureTenantId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureTenantId")
        config.azureClientId?.let { vault.set("$KEY_CONFIG.azureClientId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientId")
        config.azureClientSecret?.let { vault.set("$KEY_CONFIG.azureClientSecret", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientSecret")
        config.azureApiToken?.let { vault.set("$KEY_CONFIG.azureApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiToken")
        config.azureApiRefreshToken?.let { vault.set("$KEY_CONFIG.azureApiRefreshToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiRefreshToken")
        config.azureScope?.let { vault.set("$KEY_CONFIG.azureScope", it) } ?: vault.deleteObject("$KEY_CONFIG.azureScope")
    }

    open fun restore(): Config {
        return Config(
            gitHubApiUser = vault.string("$KEY_CONFIG.gitHubApiUser"),
            gitHubApiToken = vault.string("$KEY_CONFIG.gitHubApiToken"),
            lastSignInMethod = vault.string("$KEY_CONFIG.lastSignInMethod"),
            azureSearchEndpoint = vault.string("$KEY_CONFIG.azureSearchEndpoint"),
            azureSearchIndex = vault.string("$KEY_CONFIG.azureSearchIndex"),
            azureTenantId = vault.string("$KEY_CONFIG.azureTenantId"),
            azureClientId = vault.string("$KEY_CONFIG.azureClientId"),
            azureClientSecret = vault.string("$KEY_CONFIG.azureClientSecret"),
            azureApiToken = vault.string("$KEY_CONFIG.azureApiToken"),
            azureApiRefreshToken = vault.string("$KEY_CONFIG.azureApiRefreshToken"),
            azureScope = vault.string("$KEY_CONFIG.azureScope")
        )
    }

    fun clear() {
        vault.clear()
    }

    companion object {
        const val KEY_CONFIG = "configuration.key"
    }
}

interface Logging {
    fun debug(message: String)
    fun info(message: String)
}

expect fun getPlatform(): Platform
expect fun getConfiguration(appContextHandle: Any?): Configuration
expect fun getLogging(): Logging
