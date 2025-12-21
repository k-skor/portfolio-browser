package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import android.icu.util.Calendar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.R
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppImage
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.Categories
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.FloatingBackButton
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.store.ProjectState

interface DetailsEditActions {
    fun onFavorite(favorite: Boolean)
    fun onTogglePublic(public: Boolean)
}

interface DetailsActions {
    fun onShare(project: Project)
    fun onNavigate(url: String)
    fun onNavigateBack()
}

@Composable
fun Loading() {

}

@Composable
fun LoadingError(throwable: Throwable? = null) {

}

val loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor."

@Composable
fun DetailsReady(modifier: Modifier = Modifier, project: Project, actions: DetailsActions) {
    //val item = state.project
    val item = project

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        item.image?.let {
            AppImage(Modifier.fillMaxWidth(), it, "Project image")
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Row {
                IconButton(onClick = {

                }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Add to favorites")
                }
                IconButton(onClick = {

                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share project")
                }
                IconButton(onClick = {

                }) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Open source",
                        modifier = Modifier.rotate(90f)
                    )
                }
            }
        }
        Text(
            text = item.createdByName.uppercase(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = item.name,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        var description = item.description
        if (description.isNullOrEmpty()) {
            description = loremIpsum
        }
        //Text(
        //    text = description,
        //    fontSize = 18.sp
        //)
        var descriptionState by remember { mutableStateOf(description) }
        TextField(
            value = description,
            onValueChange = { description = it },
            enabled = true
        )
        Column {
            Text(
                text = "Programming languages",
                fontSize = 16.sp
            )
            Categories(Modifier.padding(4.dp), item.stack)
        }
        Column {
            Text(
                text = "Year of creation",
                fontSize = 16.sp
            )
            val calendar = Calendar.getInstance().apply {
                timeInMillis = item.createdOn
            }
            Text(
                text = calendar.get(Calendar.YEAR).toString(),
                fontSize = 30.sp
            )
        }
        Column {
            Text(
                text = "Co-authors",
                fontSize = 16.sp
            )
            Row {
                item.coauthors.forEach {
                    Text(
                        text = it,
                        fontSize = 30.sp
                    )
                }
            }
        }
        Row {
            Text(text = "Set project\nas public:", fontSize = 16.sp)
            Switch(modifier = Modifier.padding(start = 4.dp), checked = item.public, onCheckedChange = {
                //actions.onTogglePublic(it)
            }, enabled = false)
        }
    }
}

@Composable
fun DetailsScreen(modifier: Modifier = Modifier, contentPaddingValues: PaddingValues, stateFlow: StateFlow<ProjectState>, actions: DetailsActions) {
    val state by stateFlow.collectAsState()
    Box {
        when (state) {
            is ProjectState.Loading -> Loading()
            is ProjectState.Loaded -> DetailsReady(modifier = modifier, project = (state as ProjectState.Loaded).project, actions)
            is ProjectState.Error -> LoadingError((state as ProjectState.Error).reason)
            else -> LoadingError()
        }
        FloatingBackButton(Modifier.padding(top = 8.dp, start = 8.dp)) { actions.onNavigateBack() }
    }
}


private val fakeData = Project(
    id = 1.toString(),
    name = "Title 1",
    description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor.",
    stack = listOf(
        Stack(name = "Kotlin", percent = 67f, color = 0x00DA02B8 or (0xFF shl 24)),
        Stack(name = "Java", percent = 33f, color = 0x3F0AB7C3 or (0xFF shl 24))
    ),
    image = Resource.LocalResource(R.drawable.default_img),
    createdBy = "0123456",
    createdByName = "Krzysztof Skórcz",
    createdOn = 11234567890
)
val fakeDetails = ProjectState.Loaded(fakeData)
val fakeDetailsFlow = MutableStateFlow(fakeDetails)

@Preview(widthDp = 320)
@Composable
fun DetailsPreview() {
    MyApplicationTheme {
        DetailsScreen(modifier = Modifier.fillMaxSize(), PaddingValues(), fakeDetailsFlow, object : DetailsActions {
            override fun onShare(project: Project) {
                TODO("Not yet implemented")
            }

            override fun onNavigate(url: String) {
                TODO("Not yet implemented")
            }

            override fun onNavigateBack() {
                TODO("Not yet implemented")
            }
        })
    }
}
