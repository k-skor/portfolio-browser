package pl.krzyssko.portfoliobrowser.data

sealed class Resource {
    data class LocalResource(val name: String): Resource()
    data class NetworkResource(val url: String): Resource()
}

data class Stack(val name: String, val lines: Int, val color: Int = 0x00FFFFFF)

data class Project(val id: Int, val name: String, val description: String?, val stack: List<Stack>, val icon: Resource)
