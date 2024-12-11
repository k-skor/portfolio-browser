package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.store.ProjectState

@Composable
fun DetailsScreen(modifier: Modifier = Modifier, contentPaddingValues: PaddingValues, stateFlow: StateFlow<ProjectState>) {
    val state by stateFlow.collectAsState()
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(top = contentPaddingValues.calculateTopPadding())) {
        val item = (state as? ProjectState.Ready)?.project ?: return
        val textModifier = modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .padding(bottom = 12.dp)
        AsyncImage(
            model = when (item.icon) {
                is Resource.NetworkResource -> (item.icon as Resource.NetworkResource).url
                is Resource.LocalResource -> (item.icon as Resource.LocalResource).name
                else -> null
            },
            modifier = modifier
                .fillMaxWidth(),
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
        if (item.stack.isNotEmpty()) {
            val sum =
                item.stack.map { it.lines }.reduce { sum, lines -> sum + lines }
            val stack = item.stack
            Column {
                Row(modifier = modifier
                    .fillMaxWidth()
                    .padding(4.dp)) {
                    for (stackIt in stack) {
                        val weight = stackIt.lines.toFloat() / sum
                        Surface(
                            modifier = Modifier
                                .height(4.dp)
                                .weight(weight),
                            color = Color(stackIt.color),
                            shape = RectangleShape
                        ) { }
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
        }
    }
}


private val fakeData = Project(
        id = 1,
        name = "Title 1",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor.",
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")

    )
val fakeDetails = ProjectState.Ready(fakeData)
val fakeDetailsFlow = MutableStateFlow(fakeDetails)

@Preview(widthDp = 320)
@Composable
fun DetailsPreview() {
    MyApplicationTheme {
        DetailsScreen(modifier = Modifier.fillMaxSize(), PaddingValues(), fakeDetailsFlow)
    }
}
