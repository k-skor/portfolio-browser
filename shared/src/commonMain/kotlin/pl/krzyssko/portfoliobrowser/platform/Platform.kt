package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault
import pl.krzyssko.portfoliobrowser.data.Config

interface Platform {
    val name: String
}

abstract class Configuration {
    abstract val vault: KVault
    //abstract val default: Config
    abstract var config: Config

    fun update(config: Config) {
        this.config = config
    }

    fun save(config: Config) {
        config.gitHubApiUser?.let { vault.set("config.gitHubApiUser", it) }
        config.gitHubApiToken?.let { vault.set("config.gitHubApiToken", it) }
        config.lastSignInMethod?.let { vault.set("config.lastSignInMethod", it) }
    }

    fun restore(): Config {
        return Config(
            gitHubApiUser = vault.string("config.gitHubApiUser"),
            gitHubApiToken = vault.string("config.gitHubApiToken"),
            lastSignInMethod = vault.string("config.lastSignInMethod")
        )
    }

    fun clear() {
        vault.clear()
    }
}

interface Logging {
    fun debug(message: String)
    fun info(message: String)
}

expect fun getPlatform(): Platform
expect fun getConfiguration(contextHandle: Any?): Configuration
expect fun getLogging(): Logging