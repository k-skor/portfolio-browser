package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.R
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppImage
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.CategoryList
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.FloatingBackButton
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.store.ProjectState

interface DetailsEditActions {
}

interface DetailsActions {
    fun onFavorite(project: Project, favorite: Boolean)
    fun onShare(project: Project)
    fun onNavigate(url: String)
    fun onNavigateBack()
    fun onTogglePublic(public: Boolean)
}

@Composable
fun Loading() {

}

@Composable
fun LoadingError(throwable: Throwable? = null) {

}

@Composable
fun DetailsReady(modifier: Modifier = Modifier, project: Project, isSignedIn: Boolean, actions: DetailsActions) {
    //val item = state.project
    var favoriteState by remember { mutableStateOf(project.followers.isNotEmpty()) }

    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        project.image?.let {
            AppImage(Modifier.fillMaxWidth(), it, "Project image")
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Row {
                if (isSignedIn) {
                    IconButton(onClick = {
                        favoriteState = !favoriteState
                        actions.onFavorite(project, favoriteState)
                    }) {
                        Icon(if (favoriteState) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Add to favorites")
                    }
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
            text = project.createdByName.uppercase(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = project.name,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        //var description = project.description
        //if (description.isNullOrEmpty()) {
        //    description = loremIpsum
        //}
        //Text(
        //    text = description,
        //    fontSize = 18.sp
        //)
        //var descriptionState by remember { mutableStateOf(description) }
        Text(project.description.orEmpty())
        Column {
            Text(
                text = "Programming languages",
                style = MaterialTheme.typography.titleSmall
            )
            CategoryList(Modifier.padding(4.dp), project.stack)
        }
        Column {
            Text(
                text = "Year of creation",
                style = MaterialTheme.typography.titleSmall
            )
            val calendar = Calendar.getInstance().apply {
                timeInMillis = project.createdOn
            }
            Text(calendar.get(Calendar.YEAR).toString())
        }
        Column {
            Text(
                text = "Co-authors",
                style = MaterialTheme.typography.titleSmall
            )
            Row {
                project.coauthors.forEach {
                    Text(
                        text = it,
                        fontSize = 30.sp
                    )
                }
            }
        }
        Row {
            Text("Set project\nas public:")
            Switch(modifier = Modifier.padding(start = 4.dp), checked = project.public, onCheckedChange = {
                //actions.onTogglePublic(it)
            }, enabled = false)
        }
    }
}

@Composable
fun DetailsScreen(modifier: Modifier = Modifier, stateFlow: StateFlow<ProjectState>, isSignedIn: Boolean, actions: DetailsActions) {
    val state by stateFlow.collectAsState()
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box {
            when (state) {
                is ProjectState.Loading -> Loading()
                is ProjectState.Loaded -> DetailsReady(modifier = Modifier.padding(horizontal = 8.dp), project = (state as ProjectState.Loaded).project, isSignedIn = isSignedIn, actions = actions)
                is ProjectState.Error -> LoadingError((state as ProjectState.Error).reason)
            }
            FloatingBackButton(Modifier.padding(top = 8.dp, start = 8.dp)) { actions.onNavigateBack() }
        }
    }
}

val loremIpsum = LoremIpsum(50).values.joinToString()

val fakeProject = Project(
    id = 1.toString(),
    name = "Title 1",
    description = loremIpsum,
    stack = listOf(
        Stack(name = "Kotlin", percent = 67f, color = 0x00DA02B8 or (0xFF shl 24)),
        Stack(name = "Java", percent = 33f, color = 0x3F0AB7C3 or (0xFF shl 24))
    ),
    image = Resource.LocalResource(R.drawable.default_img),
    createdBy = "0123456",
    createdByName = "Krzysztof Skórcz",
    createdOn = 11234567890
)

val fakeDetails = ProjectState.Loaded(fakeProject)
val fakeDetailsFlow = MutableStateFlow(fakeDetails)

@Preview(widthDp = 320)
@Composable
fun DetailsPreview() {
    AppTheme {
        DetailsScreen(stateFlow = fakeDetailsFlow, isSignedIn = true, actions = object : DetailsActions {
            override fun onFavorite(project: Project, favorite: Boolean) {
                TODO("Not yet implemented")
            }

            override fun onTogglePublic(public: Boolean) {
                TODO("Not yet implemented")
            }

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
