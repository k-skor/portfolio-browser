package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.fakeProject
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme
import pl.krzyssko.portfoliobrowser.android.ui.theme.overlineSmallStyle
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack


@Composable
fun ProjectOverview(modifier: Modifier = Modifier, item: Project, stack: List<Stack>, onItemClick: (item: Project) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = {
            onItemClick(item)
        }) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item.image?.let {
                    AppImage(Modifier
                        .width(120.dp)
                        .fillMaxHeight(), it, "Project image",
                        ContentScale.FillHeight)
                }
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Overline",
                        style = overlineSmallStyle
                    )
                    Text(
                        text = item.name,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!item.description.isNullOrEmpty()) {
                        Text(
                            text = "${item.description}",
                            fontSize = 18.sp
                        )
                    }
                }
            }
            if (stack.isNotEmpty()) {
                CategoryList(stack = stack)
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Preview(widthDp = 480, heightDp = 360)
@Composable
fun ProjectOverviewPreview() {
    AppTheme {
        ProjectOverview(
            item = fakeProject,
            stack = fakeProject.stack
        ) { }
    }
}
