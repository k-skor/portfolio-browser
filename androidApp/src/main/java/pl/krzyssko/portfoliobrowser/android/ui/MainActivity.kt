package pl.krzyssko.portfoliobrowser.android.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.google.relay.compose.thenIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import pl.krzyssko.portfoliobrowser.BuildConfig
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Resource
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.Route
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class MainActivity : ComponentActivity() {
    private val projectViewModel: ProjectViewModel by viewModel()
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModel()
    private val log: Logging by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    projectViewModel.stateFlow.collect { render(it) }
                }
                launch {
                    projectViewModel.sideEffectFlow.collect { handleSideEffect(it) }
                }
            }
        }
        getLogging().debug("HELLLOOOOOO!!!!")
        setContent {
            PortfolioApp(modifier = Modifier.fillMaxSize(), coroutineScope = lifecycleScope, listViewModel = projectViewModel, detailsViewModel = projectDetailsViewModel)
        }
    }

    private fun render(state: pl.krzyssko.portfoliobrowser.store.State.ProjectsListState) {

    }

    private fun handleSideEffect(sideEffects: UserSideEffects) {
        when (sideEffects) {
            is UserSideEffects.Trace -> log.debug(sideEffects.message)
            is UserSideEffects.Toast -> Toast.makeText(this, sideEffects.message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope,
    listViewModel: ProjectViewModel,
    detailsViewModel: ProjectDetailsViewModel
) {
    val lazyPagingItems = listViewModel.pagingFlow.collectAsLazyPagingItems()
    //val sideEffectsHandler = detailsViewModel.sideEffectsFlow.collectAsState(UserSideEffects.None)
    val details = detailsViewModel.stateFlow.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(BuildConfig.TAG)
                }
            )
        }) { innerPadding ->
        NavHost(navController = navController, startDestination = Route.ProjectsList, modifier = modifier) {
            composable<Route.ProjectsList> {
                ListScreen(modifier, innerPadding, lazyPagingItems, details) { name ->
                    name?.let {
                        detailsViewModel.loadProjectWith(name)
                        coroutineScope.launch {
                            detailsViewModel.sideEffectsFlow.collect { effect ->
                                if (effect is UserSideEffects.NavigateTo) {
                                    navController.navigate(route = Route.ProjectDetails)
                                }
                            }
                        }
                    }
                }
            }
            composable<Route.ProjectDetails> {
                DetailsScreen(modifier, details)
            }
        }
    }
}

@Composable
fun ListScreen(modifier: Modifier = Modifier, innerPadding: PaddingValues, lazyPagingItems: LazyPagingItems<Project>, detailsState: State<pl.krzyssko.portfoliobrowser.store.State.ProjectState>, onFetchDetails: (name: String?) -> Unit) {
    val projectClicked = remember { mutableStateOf("") }
        Surface(modifier = modifier.padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            if (lazyPagingItems.loadState.refresh == LoadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }
            Column(modifier) {
                LazyVerticalStaggeredGrid(
                    modifier = modifier
                        .fillMaxSize()
                        .weight(1.0f),
                    columns = StaggeredGridCells.Fixed(2),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(
                        count = lazyPagingItems.itemCount,
                        key = lazyPagingItems.itemKey { it.id }) { index ->
                        val item = lazyPagingItems[index] ?: return@items
                        Card(
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = {
                                if (projectClicked.value.isEmpty()) {
                                    onFetchDetails(item.name)
                                    projectClicked.value = item.name
                                }
                            }) {
                            Box(Modifier.thenIf(detailsState.value.loading && item.name == projectClicked.value) {
                                Modifier.alpha(
                                    0.22f
                                )
                            }, contentAlignment = Alignment.Center) {
                                Column {

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
                                    val sum =
                                        item.stack.map { it.lines }
                                            .reduce { sum, lines -> sum + lines }
                                    Column {
                                        Row(
                                            modifier = modifier
                                                .fillMaxWidth()
                                                .padding(4.dp)
                                        ) {
                                            for (stackIt in item.stack) {
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
                                            for (stackIt in item.stack) {
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
                                if (detailsState.value.loading && item.name == projectClicked.value) {
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

@Composable
fun DetailsScreen(modifier: Modifier = Modifier, state: State<pl.krzyssko.portfoliobrowser.store.State.ProjectState>) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        val item = state.value.project
        val textModifier = modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .padding(bottom = 12.dp)
        AsyncImage(
            model = when (item?.icon) {
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
            text = "${item?.name}",
            modifier = textModifier,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        if (!item?.description.isNullOrEmpty()) {
            Text(
                text = "${item?.description}",
                modifier = textModifier,
                fontSize = 18.sp
            )
        }
        item?.stack?.let { stack ->
            val sum =
                stack.map { it.lines }.reduce { sum, lines -> sum + lines }
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

val fakeData: List<Project> = listOf(
    Project(
        id = 1,
        name = "Title 1",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tristique nibh nec augue cursus, in consectetur augue ultricies. Morbi finibus viverra mi, eu condimentum elit egestas condimentum. Aenean leo magna, semper nec arcu eget, facilisis molestie arcu. Quisque cursus fringilla luctus. Maecenas ut auctor leo, nec consectetur dolor.",
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg")

    ),
    Project(
        id = 2,
        name = "",
        description = null,
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
    ),
    Project(
        id = 3,
        name = "Title 3",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        stack = listOf(Stack(name = "Kotlin", lines =  6342, color = 0x00DA02B8 or (0xFF shl 24)), Stack(name = "Java", lines =  1287, color = 0x3F0AB7C3 or (0xFF shl 24))),
        icon = Resource.NetworkResource("https://github.githubassets.com/favicons/favicon.svg"),
    )
)
val pagingData = PagingData.from(fakeData)
val fakeDataFlow = MutableStateFlow(pagingData)
val fakeDetails= pl.krzyssko.portfoliobrowser.store.State.ProjectState(loading = true, project = fakeData[0])

@Preview(widthDp = 320)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        val details = remember { mutableStateOf(fakeDetails) }
        ListScreen(modifier = Modifier.fillMaxSize(), innerPadding = PaddingValues(), detailsState = details, lazyPagingItems = fakeDataFlow.collectAsLazyPagingItems()) { name -> }
        //DetailsScreen(modifier = Modifier.fillMaxSize(), MutableStateFlow(fakeDetails).collectAsState())
    }
}
