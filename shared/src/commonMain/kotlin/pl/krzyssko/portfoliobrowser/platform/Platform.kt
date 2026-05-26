package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault

interface Platform {
    val name: String
}

data class ConfigurationData(
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

interface ConfigurationDataSaver {
    var configurationData: ConfigurationData
        get() = restore()
        set(value) = save(value)

    fun save(data: ConfigurationData)
    fun restore(): ConfigurationData
}

interface VaultProvider {
    val vault: KVault
}

abstract class Configuration(vaultProvider: VaultProvider) : ConfigurationDataSaver {

    private val vault by lazy {
        vaultProvider.vault
    }

    override fun save(data: ConfigurationData) {
        data.gitHubApiUser?.let { vault.set("$KEY_CONFIG.gitHubApiUser", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiUser")
        data.gitHubApiToken?.let { vault.set("$KEY_CONFIG.gitHubApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiToken")
        data.lastSignInMethod?.let { vault.set("$KEY_CONFIG.lastSignInMethod", it) } ?: vault.deleteObject("$KEY_CONFIG.lastSignInMethod")
        data.azureSearchEndpoint?.let { vault.set("$KEY_CONFIG.azureSearchEndpoint", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchEndpoint")
        data.azureSearchIndex?.let { vault.set("$KEY_CONFIG.azureSearchIndex", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchIndex")
        data.azureTenantId?.let { vault.set("$KEY_CONFIG.azureTenantId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureTenantId")
        data.azureClientId?.let { vault.set("$KEY_CONFIG.azureClientId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientId")
        data.azureClientSecret?.let { vault.set("$KEY_CONFIG.azureClientSecret", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientSecret")
        data.azureApiToken?.let { vault.set("$KEY_CONFIG.azureApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiToken")
        data.azureApiRefreshToken?.let { vault.set("$KEY_CONFIG.azureApiRefreshToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiRefreshToken")
        data.azureScope?.let { vault.set("$KEY_CONFIG.azureScope", it) } ?: vault.deleteObject("$KEY_CONFIG.azureScope")
    }

    override fun restore(): ConfigurationData {
        return ConfigurationData(
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

    fun update(data: ConfigurationData) {
        this.configurationData = data
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
expect fun getConfiguration(vaultProvider: VaultProvider): Configuration
expect fun getLogging(): Logging
