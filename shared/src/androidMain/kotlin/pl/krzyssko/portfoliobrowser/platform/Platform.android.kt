package pl.krzyssko.portfoliobrowser.platform

import android.Manifest
import android.content.Context
import android.util.Log
import com.liftric.kvault.KVault
import mu.KotlinLogging
import org.slf4j.event.Level
import pl.krzyssko.portfoliobrowser.BuildConfig
import pl.krzyssko.portfoliobrowser.data.Config

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidConfiguration(private val contextHandle: Any?): Configuration() {
    override val vault by lazy { KVault(contextHandle as Context, "private_config") }
    override val default = Config() //Config(BuildConfig.githubApiUser, BuildConfig.githubApiKey)
    override var config = default
        set(value) {
            field = value
            save()
        }

    init {
        restore()
    }
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
actual fun getConfiguration(contextHandle: Any?): Configuration = AndroidConfiguration(contextHandle)
actual fun getLogging(): Logging = AndroidLogging()