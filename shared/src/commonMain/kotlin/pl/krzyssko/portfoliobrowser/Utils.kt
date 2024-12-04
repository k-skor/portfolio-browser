package pl.krzyssko.portfoliobrowser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pl.krzyssko.portfoliobrowser.store.StackColorMap
import kotlin.random.Random

class InfiniteColorPicker(currentColorMap: StackColorMap = emptyMap()) {

    private val colorMapFlow = MutableStateFlow(currentColorMap)
    val colorMapStateFlow = colorMapFlow.asStateFlow()

    private fun update(key: String, color: Int) {
        colorMapFlow.update { colorMapFlow.value + (key to color) }
    }

    fun pick(key: String): Int {
        if (!colorMapFlow.value.contains(key)) {
            Random.nextInt().also {
                // alpha 100%
                val color = it or (0xFF shl 24)
                update(key, color)
            }
        }
        return colorMapFlow.value[key]!!
    }
}
