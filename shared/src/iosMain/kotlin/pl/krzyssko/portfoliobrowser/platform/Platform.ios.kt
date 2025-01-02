package pl.krzyssko.portfoliobrowser.platform

import com.liftric.kvault.KVault
import pl.krzyssko.portfoliobrowser.data.Config
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

class IosConfiguration: Configuration() {
    override var config: Config
        get() = TODO("Not yet implemented")
        set(value) {}
    override val default: Config
        get() = TODO("Not yet implemented")
    override val vault: KVault
        get() = TODO("Not yet implemented")
}

class IosLogging: Logging {
    override fun debug(message: String) {
        TODO("Not yet implemented")
    }

    override fun info(message: String) {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
actual fun getConfiguration(contextHandle: Any?): Configuration = IosConfiguration()
actual fun getLogging(): Logging = IosLogging()
