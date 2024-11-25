package pl.krzyssko.portfoliobrowser.api.paging

import io.ktor.http.LinkHeader
import io.ktor.http.parseHeaderValue
import pl.krzyssko.portfoliobrowser.platform.getLogging

interface PagingKey<T> {
    enum class Rel(val value: String) {
        Next("next"),
        Prev("prev"),
        Last("last")
    }

    fun get(rel: Rel): T?
}

class LinkHeaderPageParser: PagingKey<String> {

    private val linkHeaderParamsMap = mutableMapOf<String, String>()

    override fun get(rel: PagingKey.Rel): String? = linkHeaderParamsMap[rel.value]

    fun parse(linkHeader: String) {
        val log = getLogging()
        val linkValues = linkHeader.split(",")
        log.debug("Splitted size=${linkValues.size} values=${linkValues.joinToString("===")}")
        val parsedValues = linkValues
            .map { headerValue ->
                parseHeaderValue(headerValue).take(1).getOrNull(0)?.let { value ->
                    LinkHeader(value.value, value.params)
                }
            }
            .filter { !it?.parameters.isNullOrEmpty() }
        //.map { Map.Entry(Rel.valueOf(it.parameters.get(1)), it) }

        //.flatMap { it.parameters.take(1) }
        //.filter { linkValue -> linkValue.parameters.all { it.name == LinkHeader.Rel.Next } }
        //.map { linkValue ->
        //    linkValue.uri
        //}
        for (value in parsedValues) {
            val rel = value?.parameters?.get(0)?.value
            rel?.let {
                var uri = value.uri
                while (uri.startsWith('<')) {
                    uri = uri.trimStart('<').trimEnd('>')
                }
                linkHeaderParamsMap[it] = uri
            }
        }
    }
}
