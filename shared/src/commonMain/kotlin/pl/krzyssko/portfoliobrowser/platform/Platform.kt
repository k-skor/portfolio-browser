package pl.krzyssko.portfoliobrowser.platform

interface Platform {
    val name: String
}

interface Configuration {
    val gitHubApiUser: String
    val gitHubApiToken: String
}

interface Logging {
    fun debug(message: String)
    fun info(message: String)
}

expect fun getPlatform(): Platform
expect fun getConfiguration(): Configuration
expect fun getLogging(): Logging