package pl.krzyssko.portfoliobrowser.repository

interface UserRepository {
    suspend fun fetchUser(): Result<String>
}