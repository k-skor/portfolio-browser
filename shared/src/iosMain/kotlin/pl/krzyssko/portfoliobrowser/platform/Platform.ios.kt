package pl.krzyssko.portfoliobrowser.platform

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

class IosConfiguration(vaultProvider: VaultProvider): Configuration(vaultProvider) {
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
actual fun getConfiguration(vaultProvider: VaultProvider): Configuration = IosConfiguration(vaultProvider)
actual fun getLogging(): Logging = IosLogging()
