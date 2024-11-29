package pl.krzyssko.portfoliobrowser

import pl.krzyssko.portfoliobrowser.store.StackColorMap
import kotlin.random.Random

class InfiniteColorPicker(currentColorMap: StackColorMap) {
    val colorMap: MutableMap<String, Int> by lazy {
        currentColorMap.toMutableMap()
    }

    fun pick(key: String): Int {
        if (colorMap.contains(key)) {
            return colorMap[key]!!
        }
        return Random.nextInt().also {
            // alpha 100%
            val color = it or (0xFF shl 24)
            colorMap[key] = color
        }
    }
}
