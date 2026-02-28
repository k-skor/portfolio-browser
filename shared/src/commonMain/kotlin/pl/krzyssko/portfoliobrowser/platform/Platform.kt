package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault

interface Platform {
    val name: String
}

data class Config(
    val gitHubApiUser: String? = null,
    val gitHubApiToken: String? = null,
    val lastSignInMethod: String? = null,
    val isEmulator: Boolean? = null
)

abstract class Configuration {
    abstract val vault: KVault
    //abstract val default: Config
    abstract var config: Config

    fun update(config: Config) {
        this.config = config
    }

    fun save(config: Config) {
        config.gitHubApiUser?.let { vault.set("$KEY_CONFIG.gitHubApiUser", it) }
        config.gitHubApiToken?.let { vault.set("$KEY_CONFIG.gitHubApiToken", it) }
        config.lastSignInMethod?.let { vault.set("$KEY_CONFIG.lastSignInMethod", it) }
        config.isEmulator?.let { vault.set("$KEY_CONFIG.isEmulator", it) }
    }

    fun restore(): Config {
        return Config(
            gitHubApiUser = vault.string("$KEY_CONFIG.gitHubApiUser"),
            gitHubApiToken = vault.string("$KEY_CONFIG.gitHubApiToken"),
            lastSignInMethod = vault.string("$KEY_CONFIG.lastSignInMethod"),
            isEmulator = vault.bool("$KEY_CONFIG.isEmulator")
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