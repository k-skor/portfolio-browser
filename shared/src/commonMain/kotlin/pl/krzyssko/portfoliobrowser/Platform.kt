package pl.krzyssko.portfoliobrowser

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform