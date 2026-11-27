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

interface ConfigurationSaver {
    val data: ConfigurationData
    fun save(data: ConfigurationData)
    fun restore(defaults: ConfigurationData? = null): ConfigurationData
    fun clear()
    fun update(data: ConfigurationData)
}

interface VaultProvider {
    val vault: KVault
}

abstract class Configuration(vaultProvider: VaultProvider) : ConfigurationSaver {

    override val data: ConfigurationData
        get() = restore()

    private val vault by lazy {
        vaultProvider.vault
    }

    override fun save(data: ConfigurationData) {
        with(data) {
            gitHubApiUser?.let { vault.set("$KEY_CONFIG.gitHubApiUser", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiUser")
            gitHubApiToken?.let { vault.set("$KEY_CONFIG.gitHubApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.gitHubApiToken")
            lastSignInMethod?.let { vault.set("$KEY_CONFIG.lastSignInMethod", it) } ?: vault.deleteObject("$KEY_CONFIG.lastSignInMethod")
            azureSearchEndpoint?.let { vault.set("$KEY_CONFIG.azureSearchEndpoint", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchEndpoint")
            azureSearchIndex?.let { vault.set("$KEY_CONFIG.azureSearchIndex", it) } ?: vault.deleteObject("$KEY_CONFIG.azureSearchIndex")
            azureTenantId?.let { vault.set("$KEY_CONFIG.azureTenantId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureTenantId")
            azureClientId?.let { vault.set("$KEY_CONFIG.azureClientId", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientId")
            azureClientSecret?.let { vault.set("$KEY_CONFIG.azureClientSecret", it) } ?: vault.deleteObject("$KEY_CONFIG.azureClientSecret")
            azureApiToken?.let { vault.set("$KEY_CONFIG.azureApiToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiToken")
            azureApiRefreshToken?.let { vault.set("$KEY_CONFIG.azureApiRefreshToken", it) } ?: vault.deleteObject("$KEY_CONFIG.azureApiRefreshToken")
            azureScope?.let { vault.set("$KEY_CONFIG.azureScope", it) } ?: vault.deleteObject("$KEY_CONFIG.azureScope")
        }
    }

    override fun restore(defaults: ConfigurationData?): ConfigurationData {
        return ConfigurationData(
            gitHubApiUser = vault.string("$KEY_CONFIG.gitHubApiUser").takeIf { !it.isNullOrBlank() } ?: defaults?.gitHubApiUser,
            gitHubApiToken = vault.string("$KEY_CONFIG.gitHubApiToken").takeIf { !it.isNullOrBlank() } ?: defaults?.gitHubApiToken,
            lastSignInMethod = vault.string("$KEY_CONFIG.lastSignInMethod").takeIf { !it.isNullOrBlank() } ?: defaults?.lastSignInMethod,
            azureSearchEndpoint = vault.string("$KEY_CONFIG.azureSearchEndpoint").takeIf { !it.isNullOrBlank() } ?: defaults?.azureSearchEndpoint,
            azureSearchIndex = vault.string("$KEY_CONFIG.azureSearchIndex").takeIf { !it.isNullOrBlank() } ?: defaults?.azureSearchIndex,
            azureTenantId = vault.string("$KEY_CONFIG.azureTenantId").takeIf { !it.isNullOrBlank() } ?: defaults?.azureTenantId,
            azureClientId = vault.string("$KEY_CONFIG.azureClientId").takeIf { !it.isNullOrBlank() } ?: defaults?.azureClientId,
            azureClientSecret = vault.string("$KEY_CONFIG.azureClientSecret").takeIf { !it.isNullOrBlank() } ?: defaults?.azureClientSecret,
            azureApiToken = vault.string("$KEY_CONFIG.azureApiToken").takeIf { !it.isNullOrBlank() } ?: defaults?.azureApiToken,
            azureApiRefreshToken = vault.string("$KEY_CONFIG.azureApiRefreshToken").takeIf { !it.isNullOrBlank() } ?: defaults?.azureApiRefreshToken,
            azureScope = vault.string("$KEY_CONFIG.azureScope").takeIf { !it.isNullOrBlank() } ?: defaults?.azureScope
        )
    }

    override fun clear() {
        vault.clear()
    }

    override fun update(data: ConfigurationData) {
        save(data)
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
