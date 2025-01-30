package pl.krzyssko.portfoliobrowser.android.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ImportActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreenActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListViewType
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.LoginActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.LoginScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ProfileActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ProfileScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.SettingsActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.SettingsScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.WelcomeActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.WelcomeScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.Avatar
import pl.krzyssko.portfoliobrowser.android.ui.navigation.topLevelRoutes
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.auth.AuthLinkFailedException
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.ProfileState
import pl.krzyssko.portfoliobrowser.store.ProjectState
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
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
    var showSearchBarState by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            //AppBar(
            //    onBackPressed = {
            //        navController.popBackStack()
            //    },
            //    scrollBehavior = scrollBehavior
            //)
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = {
                            when (topLevelRoute.route) {
                                Route.Profile -> Avatar(Modifier.size(30.dp), profileViewModel.profileState) { }
                                else -> Icon(topLevelRoute.icon, topLevelRoute.name, tint = MaterialTheme.colorScheme.onSurface)
                            } },
                        label = { Text(topLevelRoute.name) },
                        selected = currentDestination?.hierarchy?.any { it.route == topLevelRoute.route.toString() } == true,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                }
            }
        },
        content = {
            AppContent(modifier, context, navController, listViewModel, detailsViewModel, profileViewModel, snackbarHostState, it)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    )
}

fun handleSideEffects(effect: UserSideEffects, navController: NavHostController, context: Context) {
    when (effect) {
        is UserSideEffects.NavigateTo -> navController.navigate(effect.route)
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
    val userState by profileViewModel.stateFlow.collectAsState()
    logging.debug("source state in UI=$isSourceAvailable")
    logging.debug("user state in UI=$user")
    LaunchedEffect(isSourceAvailable, user) {
        if (isSourceAvailable && user is User.Authenticated) {
            scope.launch {
                val result = snackHostState.showSnackbar("Import projects from source?", actionLabel = "OK", withDismissAction = true, duration = SnackbarDuration.Indefinite)
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        navController.navigate(Route.Login(ViewType.SourceSelection))
                    }
                    else -> {}
                }
            }
        }
    }
    LaunchedEffect(userState) {
        if (userState is ProfileState.AuthenticationFailed && (userState as ProfileState.AuthenticationFailed).reason is AuthLinkFailedException) {
            scope.launch {
                val result = snackHostState.showSnackbar("Current state will be lost, continue?", actionLabel = "OK", withDismissAction = true, duration = SnackbarDuration.Indefinite)
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        profileViewModel.authenticateUser(activity = context, forceSignIn = true)
                    }
                    else -> {}
                }
            }
        }
    }
    LaunchedEffect("sideEffects") {
        scope.launch {
            listViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, context) }
        }
        scope.launch {
            detailsViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, context) }
        }
        scope.launch {
            profileViewModel.sideEffectsFlow.collect { handleSideEffects(it, navController, context) }
        }
    }
    Column(modifier.padding(top = contentPaddingValues.calculateTopPadding(), bottom = contentPaddingValues.calculateBottomPadding())) {
        NavHost(
            navController = navController,
            startDestination = Route.Welcome,
            modifier = modifier
        ) {
            composable<Route.Welcome> {
                WelcomeScreen(modifier, contentPaddingValues, object : WelcomeActions {
                    override fun onLogin() {
                        navController.navigate(Route.Login(ViewType.Login))
                    }

                    override fun onRegister() {
                        navController.navigate(Route.Login(ViewType.Register))
                    }

                    override fun onGuestSignIn() {
                        profileViewModel.authenticateGuest()
                    }
                })
            }
            composable<Route.Login> { backStackEntry ->
                val route: Route.Login = backStackEntry.toRoute()
                LoginScreen(modifier, contentPaddingValues, route.viewType, profileViewModel.userState, object : LoginActions {
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
                }, object : WelcomeActions {
                    override fun onLogin() {
                        navController.navigate(Route.Login(ViewType.Login))
                    }

                    override fun onRegister() {
                        navController.navigate(Route.Login(ViewType.Register))
                    }

                    override fun onGuestSignIn() {
                        profileViewModel.authenticateGuest()
                    }
                }, object : ImportActions {
                    override fun importFromSource(source: Source) {
                        profileViewModel.authenticateUser(context, true)
                        scope.launch {
                            profileViewModel.userState.collect {
                                if (it is User.Authenticated) {
                                    listViewModel.importProjects(profileViewModel.userState)
                                }
                            }
                        }
                    }

                    override fun cancelImport() {
                        navController.popBackStack()
                    }
                })
            }
            navigation<Route.Home>(startDestination = Route.Projects) {
                composable<Route.Projects> {
                    ListScreen(
                        modifier,
                        ListViewType.List,
                        listViewModel.pagingFlow,
                        listViewModel.projectsState,
                        listViewModel.searchPhrase,
                        MutableStateFlow(listOf("Kotlin", "Java", "TypeScript", "Bash", "HTML")),
                        object : ListScreenActions {
                            override fun onProjectDetails(project: Project) {
                                detailsViewModel.loadProjectDetails(project)
                                navController.navigate(Route.Details(project.createdBy, project.id))
                            }

                            override fun onSearch(phrase: String) {
                                listViewModel.updateSearchPhrase(phrase)
                            }

                            override fun onClear() {
                                listViewModel.resetProjects()
                            }

                            override fun onAvatarClicked() {
                                navController.navigate(Route.Profile)
                            }
                        })
                }
                composable<Route.Details> {
                    DetailsScreen(modifier, contentPaddingValues, detailsViewModel.stateFlow, object : DetailsActions {
                        override fun onShare(project: Project) {
                            TODO("Not yet implemented")
                        }

                        override fun onNavigate(url: String) {
                            TODO("Not yet implemented")
                        }

                        override fun onNavigateBack() {
                            navController.popBackStack()
                        }
                    })
                }
                composable<Route.Profile> {
                    ProfileScreen(profileState = profileViewModel.profileState, userState = profileViewModel.userState, actions = object : ProfileActions {
                        override fun onLogin() {
                            navController.navigate(Route.Login(ViewType.Login))
                        }

                        override fun onProjectDetails(project: Project) {
                            detailsViewModel.loadProjectDetails(project)
                            navController.navigate(Route.Details(project.createdBy, project.id))
                        }

                        override fun onSaveProfile(profile: Profile) {

                        }

                        override fun onNavigateBack() {
                            navController.popBackStack()
                        }
                    }, portfolio = emptyList())
                }
                composable<Route.Settings> {
                    SettingsScreen(modifier, profileViewModel.userState, object : SettingsActions {
                        override fun onLogin() {
                            navController.navigate(Route.Login(ViewType.Login))
                        }
                    })
                }
            }
        }
    }
}
