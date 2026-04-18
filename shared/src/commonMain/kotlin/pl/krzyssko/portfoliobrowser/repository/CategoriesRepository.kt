package pl.krzyssko.portfoliobrowser.repository

import pl.krzyssko.portfoliobrowser.data.Stack

interface CategoriesRepository {
    suspend fun fetchCategory(name: String): Result<List<Stack>>
}