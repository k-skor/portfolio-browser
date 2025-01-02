package pl.krzyssko.portfoliobrowser.android.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.AccountScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreenActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.LoginActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppBar
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.Route
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

private val logging = getLogging()

class MainActivity : ComponentActivity() {
    private val projectViewModel: ProjectViewModel by viewModel()
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModel()
    private val profileViewModel: ProfileViewModel by viewModel()

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    projectViewModel.stateFlow.collect { render(it) }
                }
                launch {
                    projectDetailsViewModel.stateFlow.collect { render(it) }
                }
                launch {
                    profileViewModel.stateFlow.collect { render(it) }
                }
            }
        }
        getLogging().debug("HELLLOOOOOO!!!!")
        setContent {
            MyApplicationTheme {
                PortfolioApp(
                    modifier = Modifier.fillMaxSize(),
                    context = this,
                    listViewModel = projectViewModel,
                    detailsViewModel = projectDetailsViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }

    private fun render(state: ProjectsListState) {
        logging.debug("new state")
    }

    private fun render(state: ProjectState) {
        logging.debug("new state")
    }

    private fun render(state: ProfileState) {
        logging.debug("new state")
    }
}

@ExperimentalMaterial3Api
@Composable
fun PortfolioApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    context: Context,
    listViewModel: ProjectViewModel,
    detailsViewModel: ProjectDetailsViewModel,
    profileViewModel: ProfileViewModel
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppBar(
                onBackPressed = {
                    navController.popBackStack()
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = {
            AppContent(modifier, context, navController, listViewModel, detailsViewModel, profileViewModel, snackbarHostState, it)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    )
}

fun handleSideEffects(effect: UserSideEffects, navController: NavHostController, scope: CoroutineScope, snackHostState: SnackbarHostState, context: Context) {
    when (effect) {
        is UserSideEffects.NavigateTo -> navController.navigate(effect.route)
        is UserSideEffects.SyncSnack -> {
            scope.launch {
                val result = snackHostState.showSnackbar(effect.message, actionLabel = "Sync", withDismissAction = true, duration = SnackbarDuration.Long)
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        //(user as? User.Authenticated)?.let { listViewModel.syncProjects(it) }
                    }
                    else -> {}
                }
            }
        }
        is UserSideEffects.Toast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        is UserSideEffects.Trace -> logging.debug(effect.message)
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(modifier: Modifier = Modifier,
               context: Context,
               navController: NavHostController,
               listViewModel: ProjectViewModel,
               detailsViewModel: ProjectDetailsViewModel,
               profileViewModel: ProfileViewModel,
               snackHostState: SnackbarHostState,
               contentPaddingValues: PaddingValues
) {
    val scope = rememberCoroutineScope()
    val user by profileViewModel.userState.collectAsState()
    val isSourceAvailable by profileViewModel.isSourceAvailable.collectAsState()
    logging.debug("source state in UI=$isSourceAvailable")
    LaunchedEffect(isSourceAvailable, user) {
        if (isSourceAvailable && user is User.Authenticated) {
            val result = snackHostState.showSnackbar("Import from source?", actionLabel = "OK", withDismissAction = true, duration = SnackbarDuration.Long)
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    listViewModel.importProjects(profileViewModel.userState.value as User.Authenticated)
                }
                else -> {}
            }
        }
    }
    LaunchedEffect("sideEffects") {
        scope.launch {
            listViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, scope, snackHostState, context) }
        }
        scope.launch {
            detailsViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, scope, snackHostState, context) }
        }
        scope.launch {
            profileViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, scope, snackHostState, context) }
        }
    }
    Column {
        NavHost(
            navController = navController,
            startDestination = Route.ProjectsList,
            modifier = modifier
        ) {
            composable<Route.ProjectsList> {
                ListScreen(
                    modifier,
                    contentPaddingValues,
                    listViewModel.pagingFlow,
                    listViewModel.stateFlow,
                    detailsViewModel.stateFlow,
                    listViewModel.projectsState,
                    listViewModel.searchPhrase,
                    profileViewModel.userState,
                    object : ListScreenActions {
                    override fun onProjectClicked(name: String) {
                        detailsViewModel.loadProjectWith(name)
                    }

                    override fun onSearch(phrase: String) {
                        listViewModel.updateSearchPhrase(phrase)
                    }

                    override fun onClear() {
                        listViewModel.resetProjects()
                    }

                    override fun onAvatarClicked() {
                        navController.navigate(Route.Account)
                    }
                })
            }
            composable<Route.ProjectDetails> {
                DetailsScreen(modifier, contentPaddingValues, detailsViewModel.stateFlow)
            }
            composable<Route.Account> {
                AccountScreen(modifier, contentPaddingValues, profileViewModel.userState, object : LoginActions {
                    override fun onGitHubSignIn() {
                        profileViewModel.authenticateUser(context)
                    }

                    override fun onGitHubSignOut() {
                        profileViewModel.resetAuthentication()
                    }

                    override fun onGitHubLink() {
                        profileViewModel.linkUser(context)
                    }

                    override fun onEmailCreate(login: String, password: String) {
                        profileViewModel.createUser(context, login, password)
                    }

                    override fun onEmailSignIn(login: String, password: String) {
                        profileViewModel.authenticateUser(context, login, password)
                    }
                })
            }
        }
    }
}
