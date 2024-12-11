package pl.krzyssko.portfoliobrowser.platform

import android.Manifest
import android.util.Log
import mu.KotlinLogging
import org.slf4j.event.Level
import pl.krzyssko.portfoliobrowser.BuildConfig
import pl.krzyssko.portfoliobrowser.data.Config

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidConfiguration: Configuration {
    //override val gitHubApiUser: String = BuildConfig.githubApiUser
    //override val gitHubApiToken: String = BuildConfig.githubApiKey
    override val config = Config(BuildConfig.githubApiUser, BuildConfig.githubApiKey)
}

class AndroidLogging: Logging {
    companion object {
        const val TAG = BuildConfig.TAG
    }
    private val logger = KotlinLogging.logger {}
    //init {
    //    logger.underlyingLogger.makeLoggingEventBuilder(Level.DEBUG)
    //}
    override fun info(message: String) {
        Log.d(TAG, message)
    }
    override fun debug(message: String) {
        Log.d(TAG, message)
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun getConfiguration(): Configuration = AndroidConfiguration()
actual fun getLogging(): Logging = AndroidLogging()