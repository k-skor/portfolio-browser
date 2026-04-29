package pl.krzyssko.portfoliobrowser.platform

import android.content.Context
import android.util.Log
import com.liftric.kvault.KVault
import mu.KotlinLogging
import pl.krzyssko.portfoliobrowser.BuildConfig

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidConfiguration(private val appContextHandle: Any?): Configuration() {
    override val vault by lazy { KVault(appContextHandle as Context, "private_config") }
    override var config: Config
        get() = restore()
        set(value) = save(value)

    override fun restore(): Config {
        val restored = super.restore()
        return restored.copy(
            azureTenantId = restored.azureTenantId.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureTenantId,
            azureClientId = restored.azureClientId.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureClientId,
            azureClientSecret = restored.azureClientSecret.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureClientSecret,
            azureSearchEndpoint = restored.azureSearchEndpoint.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureSearchEndpoint,
            azureSearchIndex = restored.azureSearchIndex.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureSearchIndex,
            azureScope = restored.azureSearchIndex.takeIf { !it.isNullOrBlank() } ?: BuildConfig.azureScope,
        )
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
actual fun getConfiguration(appContextHandle: Any?): Configuration = AndroidConfiguration(appContextHandle)
actual fun getLogging(): Logging = AndroidLogging()