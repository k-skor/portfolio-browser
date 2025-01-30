package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack


@Composable
fun ProjectOverview(modifier: Modifier = Modifier, item: Project, stack: List<Stack>, onItemClick: (item: Project) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = {
            onItemClick(item)
        }) {
        Box(contentAlignment = Alignment.Center) {
            Column(modifier) {
                val textModifier = modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .padding(bottom = 12.dp)
                AsyncImage(
                    model = when (item.image) {
                        is Resource.NetworkResource -> (item.image as Resource.NetworkResource).url
                        is Resource.LocalResource -> (item.image as Resource.LocalResource).res.toString()
                        else -> null
                    },
                    modifier = modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentDescription = "Project image",
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    text = item.name,
                    modifier = textModifier,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!item.description.isNullOrEmpty()) {
                    Text(
                        text = "${item.description}",
                        modifier = textModifier,
                        fontSize = 18.sp
                    )
                }
                if (stack.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            for (stackIt in stack) {
                                val weight = stackIt.percent / 100.0f
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
                        Column(
                            modifier = modifier.padding(
                                horizontal = 4.dp,
                                vertical = 4.dp
                            )
                        ) {
                            for (stackIt in stack) {
                                Row {
                                    Surface(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .align(Alignment.CenterVertically),
                                        color = Color(stackIt.color),
                                        shape = CircleShape
                                    ) { }
                                    Text(
                                        modifier = modifier.padding(start = 4.dp),
                                        text = stackIt.name
                                    )
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(bottom = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
