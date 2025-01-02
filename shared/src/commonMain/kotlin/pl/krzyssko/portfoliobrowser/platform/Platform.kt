package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault
import pl.krzyssko.portfoliobrowser.data.Config

interface Platform {
    val name: String
}

abstract class Configuration {
    //private val vault: KVault by lazy { getKVault() }
    abstract val vault: KVault
    abstract val default: Config
    abstract var config: Config

    //init {
    //    restore()
    //}

    //abstract fun getKVault(): KVault

    fun save() {
        vault.set("config.gitHubApiUser", config.gitHubApiUser)
        vault.set("config.gitHubApiToken", config.gitHubApiToken)
        vault.set("config.lastSignInMethod", config.lastSignInMethod)
    }

    fun restore() {
        this.config.gitHubApiUser = vault.string("config.gitHubApiUser") ?: ""
        this.config.gitHubApiToken = vault.string("config.gitHubApiToken") ?: ""
        this.config.lastSignInMethod = vault.string("config.lastSignInMethod") ?: ""
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