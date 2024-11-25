package pl.krzyssko.portfoliobrowser

import pl.krzyssko.portfoliobrowser.platform.Platform
import pl.krzyssko.portfoliobrowser.platform.getPlatform

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}