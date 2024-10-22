package pl.krzyssko.portfoliobrowser.data

sealed class Resource {
    data class LocalResource(val name: String): Resource()
    data class RemoteResource(val url: String): Resource()
}

data class Project(val name: String, val description: String, val stack: String, val icon: Resource)
