package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.ProjectOverview
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack

interface ListScreenActions {
    fun onProjectDetails(project: Project)
    fun onSearch(phrase: String)
    fun onClear()
    fun onAvatarClicked()
}

enum class ListViewType {
    List,
    Grid
}

@ExperimentalMaterial3Api
@Composable
fun ListScreen(
    modifier: Modifier = Modifier,
    viewType: ListViewType,
    pagingFlow: Flow<PagingData<Project>>,
    projectsFlow: StateFlow<List<Project>>,
    phraseFlow: StateFlow<String?>,
    stackFlow: StateFlow<List<String>> = MutableStateFlow(listOf("Kotlin", "Java", "TypeScript", "Bash", "HTML")),
    actions: ListScreenActions
) {
    val projects by projectsFlow.collectAsState()
    val categories by stackFlow.collectAsState()

    val textFieldState = rememberTextFieldState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Surface(
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
                        //trailingIcon = { Avatar(Modifier.size(30.dp), userFlow) { actions.onAvatarClicked() } },
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                tonalElevation = if (scrollState.value > 0) 3.dp else 0.dp
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

            val phrase by phraseFlow.collectAsState()
            val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

            var loadingState by remember { mutableStateOf(false) }

            LaunchedEffect(phrase) {
                //Log.d("MainActivity", "ListScreen: refreshing list phrase=$phrase, isSignedIn=$isSignedIn, hasValidProject=$hasValidProject")
                //lazyPagingItems.refresh()
            }

            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }
            Column(
                modifier
                    .verticalScroll(scrollState)
                    .padding(top = SearchBarDefaults.InputFieldHeight + 16.dp)
                    .semantics { traversalIndex = 1f }) {
                Column {
                    Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    LazyRow {
                        items(
                            count = categories.size) { index ->
                            var selected by remember { mutableStateOf(false) }
                            FilterChip(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                label = { Text(categories[index]) },
                                onClick = {
                                    selected = !selected
                                    //actions.onSearch(categories[index])
                                },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Checked",
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                },
                                selected = selected
                            )
                        }
                    }
                }
                if (lazyPagingItems.loadState.refresh.endOfPaginationReached && lazyPagingItems.itemCount == 0) {
                    OutlinedButton(onClick = {

                    }, modifier = Modifier.fillMaxWidth(0.5f)) {
                        Text("Create first project")
                    }
                }
                when(viewType) {
                    ListViewType.Grid -> {
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
                                if (index == 0) {
                                    Text("Most popular projects", fontSize = 24.sp)
                                } else {
                                    val item = lazyPagingItems[index - 1] ?: return@items
                                    ProjectOverview(
                                        modifier = modifier,
                                        item = item,
                                        stack = if (index - 1 < projects.size) projects[index - 1].stack else emptyList(),
                                        onItemClick = {
                                            loadingState = true
                                            actions.onProjectDetails(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    ListViewType.List -> {
                        LazyColumn(
                            modifier = modifier
                                .fillMaxSize()
                                .weight(1.0f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id }) { index ->
                                if (index == 0) {
                                    Text("Most popular projects", fontSize = 24.sp)
                                } else {
                                    val item = lazyPagingItems[index] ?: return@items
                                    ProjectOverview(
                                        modifier = modifier,
                                        item = item,
                                        stack = if (index < projects.size) projects[index].stack else emptyList(),
                                        onItemClick = {
                                            loadingState = true
                                            actions.onProjectDetails(item)
                                        }
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
                if (lazyPagingItems.loadState.append == LoadState.Loading || loadingState) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val fakeData: List<Project> = listOf(
    Project(
        id = 1.toString(),
        name = "Title 1",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor.",
        stack = listOf(Stack(name = "Kotlin", percent =  67f, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", percent =  33f, color = 0x3F0AB7C3 or (0xFF shl 24))),
        image = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
        createdBy = "ABCD1234",
        createdByName = "k-skor",
        createdOn = 11234567890
    ),
    Project(
        id = 2.toString(),
        name = "Title 2",
        description = null,
        stack = listOf(Stack(name = "Kotlin", percent =  67f, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", percent =  33f, color = 0x3F0AB7C3 or (0xFF shl 24))),
        image = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
        createdBy = "ABCD1234",
        createdByName = "k-skor",
        createdOn = 11234567890
    ),
    Project(
        id = 3.toString(),
        name = "Title 3",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        stack = emptyList(),
        image = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
        createdBy = "ABCD1234",
        createdByName = "k-skor",
        createdOn = 11234567890
    )
)
val pagingData = PagingData.from(fakeData)
val fakeDataFlow = MutableStateFlow(pagingData)

@ExperimentalMaterial3Api
@Preview(widthDp = 320)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        darkColorScheme()
        ListScreen(
            modifier = Modifier.fillMaxSize(),
            viewType = ListViewType.Grid,
            pagingFlow = fakeDataFlow,
            projectsFlow = MutableStateFlow(fakeData),
            phraseFlow = MutableStateFlow(""),
            actions = object : ListScreenActions {
                override fun onProjectDetails(project: Project) {
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
