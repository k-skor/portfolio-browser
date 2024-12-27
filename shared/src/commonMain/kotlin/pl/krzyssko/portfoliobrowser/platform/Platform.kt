package pl.krzyssko.portfoliobrowser.platform

import pl.krzyssko.portfoliobrowser.data.Config

interface Platform {
    val name: String
}

interface Configuration {
    val default: Config
    var config: Config
}

interface Logging {
    fun debug(message: String)
    fun info(message: String)
}

expect fun getPlatform(): Platform
expect fun getConfiguration(): Configuration
expect fun getLogging(): Logging