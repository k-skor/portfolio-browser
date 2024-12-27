package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.Avatar
import pl.krzyssko.portfoliobrowser.data.Paging
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsListState

interface ListScreenActions {
    fun onProjectClicked(name: String?)
    fun onSearch(phrase: String)
    fun onClear()
    fun onAvatarClicked()
}

@ExperimentalMaterial3Api
@Composable
fun ListScreen(
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues,
    pagingFlow: Flow<PagingData<Project>>,
    listFlow: StateFlow<ProjectsListState>,
    detailsState: StateFlow<ProjectState>,
    profileState: StateFlow<ProfileState>,
    actions: ListScreenActions
) {
    val list by listFlow.collectAsState()
    val details by detailsState.collectAsState()
    val user by profileState.collectAsState()
    val userFlow = profileState.filter { it is ProfileState.Authenticated }.map { it as ProfileState.Authenticated }
    val projectsFlow = listFlow.filter { it is ProjectsListState.Ready }.map { it as ProjectsListState.Ready }

    val textFieldState = rememberTextFieldState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier.padding(top = contentPaddingValues.calculateTopPadding()),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { isTraversalGroup = true }) {
            SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = textFieldState.text.toString(),
                        onQueryChange = {
                            textFieldState.setTextAndPlaceCursorAtEnd(it)
                        },
                        onSearch = {
                            expanded = false
                            actions.onSearch(it)
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text("Search projects") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = { Avatar(Modifier.size(30.dp), userFlow) { actions.onAvatarClicked() } },
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    repeat(4) { idx ->
                        val resultText = "Suggestion $idx"
                        ListItem(
                            headlineContent = { Text(resultText) },
                            supportingContent = { Text("Additional info") },
                            leadingContent = { Icon(Icons.Filled.Star, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier =
                            Modifier
                                .clickable {
                                    textFieldState.setTextAndPlaceCursorAtEnd(resultText)
                                    expanded = false
                                }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (list !is ProjectsListState.Ready && user !is ProfileState.Authenticated) {
                actions.onClear()
                return@Box
            }

            val phrase by projectsFlow.map { it.searchPhrase }.collectAsState(null)
            val projects = projectsFlow.map { it.projects.flatMap { project -> project.value } }
            val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

            var projectClicked by remember { mutableStateOf("") }

            LaunchedEffect(phrase) {
                lazyPagingItems.refresh()
            }

            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }
            Column(modifier.verticalScroll(rememberScrollState())) {
                LazyVerticalStaggeredGrid(
                    modifier = modifier
                        .fillMaxSize()
                        .weight(1.0f),
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.id }) { index ->
                        val item = lazyPagingItems[index] ?: return@items
                        Card(
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = {
                                if (projectClicked.isEmpty()) {
                                    actions.onProjectClicked(item.name)
                                    projectClicked = item.name
                                }
                            }) {
                            Box(Modifier.alpha(if ((details is ProjectState.Loading) && item.name == projectClicked) 0.22f else 1f), contentAlignment = Alignment.Center) {
                                ProjectOverview(modifier = modifier, item, projects.filter { index < it.size }.map { it[index] })
                                if ((details is ProjectState.Loading) && item.name == projectClicked) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                            .wrapContentHeight(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }
                }
                // TODO: LazyPagingItems interface doesn't allow to specifically fetch a page
                //if (!isLastPage.value) {
                //    OutlinedButton(
                //        onClick = { },
                //        modifier = Modifier
                //            .fillMaxWidth()
                //            .wrapContentWidth(Alignment.CenterHorizontally)
                //    ) {
                //        Text("Load more")
                //    }
                //}
                if (lazyPagingItems.loadState.append == LoadState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectOverview(modifier: Modifier = Modifier, item: Project, projectState: Flow<Project>) {
    val project by projectState.collectAsState(null)
    Column(modifier) {
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
        val stack = project?.stack ?: emptyList()
        if (stack.isNotEmpty()) {
            val sum =
                stack.map { it.lines }
                    .reduce { sum, lines -> sum + lines }
            Column {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
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

private val fakeData: List<Project> = listOf(
    Project(
        id = 1,
        name = "Title 1",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor.",
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")

    ),
    Project(
        id = 2,
        name = "Title 2",
        description = null,
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
    ),
    Project(
        id = 3,
        name = "Title 3",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        stack = listOf(),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
    )
)
val pagingData = PagingData.from(fakeData)
val fakeDataFlow = MutableStateFlow(pagingData)
val fakeList = ProjectsListState.Ready(projects = mapOf(null to fakeData), paging = Paging())
val fakeListFlow = MutableStateFlow(fakeList)
val fakeUser = ProfileState.Authenticated(User(Profile("0123", "name", "email@dot.com", null, false)))
val fakeUserFlow = MutableStateFlow(fakeUser)

@ExperimentalMaterial3Api
@Preview(widthDp = 320)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        darkColorScheme()
        ListScreen(
            modifier = Modifier.fillMaxSize(),
            contentPaddingValues = PaddingValues(),
            listFlow = fakeListFlow,
            detailsState = fakeDetailsFlow,
            profileState = fakeUserFlow,
            pagingFlow = fakeDataFlow,
            actions = object : ListScreenActions {
                override fun onProjectClicked(name: String?) {
                    TODO("Not yet implemented")
                }

                override fun onSearch(phrase: String) {
                    TODO("Not yet implemented")
                }

                override fun onAvatarClicked() {
                    TODO("Not yet implemented")
                }

                override fun onClear() {
                    TODO("Not yet implemented")
                }
            })
    }
}
