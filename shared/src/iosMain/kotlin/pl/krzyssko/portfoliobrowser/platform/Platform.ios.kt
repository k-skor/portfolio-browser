package pl.krzyssko.portfoliobrowser.platform

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

class IosConfiguration: Configuration {
    override val gitHubApiUser: String = TODO("Not yet implemented")
    override val gitHubApiToken: String = TODO("Not yet implemented")
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
actual fun getConfiguration(): Configuration = IosConfiguration()
actual fun getLogging(): Logging = IosLogging()
