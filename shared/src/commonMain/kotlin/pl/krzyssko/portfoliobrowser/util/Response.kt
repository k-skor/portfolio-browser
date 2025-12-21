package pl.krzyssko.portfoliobrowser.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pl.krzyssko.portfoliobrowser.data.User

sealed class Response<out T> {
    //data object Pending: Response<Nothing>()
    data class Ok<T>(val data: T): Response<T>()
    data class Error(val throwable: Throwable?): Response<Nothing>()
}

fun <T> Flow<Response<T>>.exceptionAsResponse(): Flow<Response<T>> {
    return catch { emit(Response.Error(it)) }
}
