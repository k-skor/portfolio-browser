package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import pl.krzyssko.portfoliobrowser.data.Stack

@Composable
fun Categories(modifier: Modifier = Modifier, stack: List<Stack>) {

    if (stack.isNotEmpty()) {
        val sum =
            stack.map { it.percent }.reduce { sum, lines -> sum + lines }
        Column(modifier) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 4.dp
                )
            ) {
                for (item in stack) {
                    AssistChip(
                        onClick = { },
                        modifier = Modifier.padding(end = 4.dp),
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(item.color)),
                        label = { Text(text = item.name) },
                    )
                }
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)) {
                for (stackIt in stack) {
                    val weight = stackIt.percent / sum
                    if (weight > 0) {
                        Surface(
                            modifier = Modifier
                                .height(4.dp)
                                .weight(weight),
                            color = Color(stackIt.color),
                            shape = RectangleShape
                        ) { }
                    }
                }
            }
            //Column(
            //    modifier = Modifier.padding(
            //        horizontal = 4.dp,
            //        vertical = 4.dp
            //    )
            //) {
            //    for (stackIt in stack) {
            //        Row {
            //            Surface(
            //                modifier = Modifier
            //                    .size(6.dp)
            //                    .align(Alignment.CenterVertically),
            //                color = Color(stackIt.color),
            //                shape = CircleShape
            //            ) { }
            //            Text(
            //                modifier = modifier.padding(start = 4.dp),
            //                text = stackIt.name
            //            )
            //        }
            //    }
            //}
        }
    }
}