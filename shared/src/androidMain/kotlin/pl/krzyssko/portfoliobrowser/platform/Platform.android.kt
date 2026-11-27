package pl.krzyssko.portfoliobrowser.platform

import android.util.Log
import mu.KotlinLogging
import pl.krzyssko.portfoliobrowser.BuildConfig

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

class AndroidConfiguration(vaultProvider: VaultProvider) : Configuration(vaultProvider) {
    override fun restore(defaults: ConfigurationData?): ConfigurationData =
        super.restore(defaults ?: DEFAULTS)

    companion object {
        val DEFAULTS = ConfigurationData(
            azureTenantId = BuildConfig.azureTenantId,
            azureClientId = BuildConfig.azureClientId,
            azureClientSecret = BuildConfig.azureClientSecret,
            azureSearchEndpoint = BuildConfig.azureSearchEndpoint,
            azureSearchIndex = BuildConfig.azureSearchIndex,
            azureScope = BuildConfig.azureScope,
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
actual fun getConfiguration(vaultProvider: VaultProvider): Configuration = AndroidConfiguration(vaultProvider)
actual fun getLogging(): Logging = AndroidLogging()