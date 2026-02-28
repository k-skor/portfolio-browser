package pl.krzyssko.portfoliobrowser.android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import pl.krzyssko.portfoliobrowser.android.ui.MainActivity.Companion.TAG
import pl.krzyssko.portfoliobrowser.android.ui.compose.dialog.AccountsMergeDialog
import pl.krzyssko.portfoliobrowser.android.ui.compose.dialog.ErrorDialog
import pl.krzyssko.portfoliobrowser.android.ui.compose.dialog.ImportDialog
import pl.krzyssko.portfoliobrowser.android.ui.compose.dialog.PrepareProfile
import pl.krzyssko.portfoliobrowser.android.ui.compose.dialog.ProviderImportDialog
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ImportActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreenActions
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
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectsListViewModel
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.LoginState
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

private val logging = getLogging()

class MainActivity : ComponentActivity() {
    private val projectsListViewModel: ProjectsListViewModel by viewModel()
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModel()
    private val profileViewModel: ProfileViewModel by viewModel()

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //launch {
                //    projectViewModel.stateFlow.collect { render(it) }
                //}
                //launch {
                //    projectDetailsViewModel.stateFlow.collect { render(it) }
                //}
                //launch {
                //    profileViewModel.projectsImportOnboardingStep.stateFlow.collect { render(it) }
                //}
                //launch {
                //    profileViewModel.stateFlow.collect { render(it) }
                //}
            }
        }
        getLogging().debug("HELLLOOOOOO!!!!")
        setContent {
            AppTheme {
                PortfolioApp(
                    modifier = Modifier.fillMaxSize(),
                    context = this,
                    lifecycle = lifecycle,
                    listViewModel = projectsListViewModel,
                    detailsViewModel = projectDetailsViewModel,
                    profileViewModel = profileViewModel,
                    navController = rememberNavController()
                )
            }
        }
    }

    //private fun render(state: ProjectsListState) {
    //    logging.debug("new state")
    //}

    //private fun render(state: ProjectState) {
    //    logging.debug("new state")
    //}

    //private fun render(state: UserOnboardingImportState) {
    //    logging.debug("new state ${state::class}")
    //    logging.debug("lifecycle=${lifecycle.currentState}")
    //    when (state) {
    //        is UserOnboardingImportState.ImportCompleted -> projectViewModel.refreshProjectsList()
    //        else -> {}
    //    }
    //}

    companion object {
        const val TAG = "MainActivity"
    }
}

@ExperimentalMaterial3Api
@Composable
fun PortfolioApp(
    modifier: Modifier = Modifier,
    context: Context,
    lifecycle: Lifecycle,
    listViewModel: ProjectsListViewModel,
    detailsViewModel: ProjectDetailsViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavHostController
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showSearchBarState by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            //AppBar(
            //    onBackPressed = {
            //        navController.popBackStack()
            //    },
            //    scrollBehavior = scrollBehavior
            //)
        },
        bottomBar =
            {
            val userState by profileViewModel.userLogin.stateFlow.collectAsState()
            if (userState is LoginState.Authenticated) {
                val profile by profileViewModel.profileState.collectAsState()
                NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    topLevelRoutes.forEach { topLevelRoute ->
                        NavigationBarItem(
                            icon = {
                                val defaultIcon: @Composable () -> Unit = {
                                    Icon(
                                        topLevelRoute.icon,
                                        topLevelRoute.name,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                when (topLevelRoute.route) {
                                    Route.Profile -> {
                                        (profile as? Profile.Loaded)?.avatarUrl?.let {
                                            Avatar(
                                                Modifier.size(30.dp),
                                                it
                                            )
                                        } ?: defaultIcon()
                                    }

                                    else -> defaultIcon()
                                }
                            },
                            label = { Text(topLevelRoute.name) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == topLevelRoute.route::class.qualifiedName
                            } == true,
                            onClick = {
                                Log.d(TAG, "PortfolioApp: navigate to route=${topLevelRoute.route}")
                                navController.navigate(topLevelRoute.route) {
                                    //popUpTo(Route.Home) {
                                    //    saveState = true
                                    //}
                                    launchSingleTop = true
                                    //restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        content = {
            AppContent(
                modifier,
                context,
                lifecycle,
                navController,
                listViewModel,
                detailsViewModel,
                profileViewModel,
                it
            )
        }
    )
}

fun handleSideEffects(effect: UserSideEffects, navController: NavHostController, context: Context) {
    when (effect) {
        is UserSideEffects.NavigateTo -> {
            navController.navigate(effect.route) {
                when (effect.route) {
                    is Route.Home -> {
                        popUpTo(Route.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                    is Route.Welcome -> popUpTo(Route.Home) { inclusive = true }
                    else -> {}
                }
            }
        }
        is UserSideEffects.Toast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        is UserSideEffects.Trace -> logging.debug(effect.message)
        is UserSideEffects.Error -> {
            effect.throwable?.printStackTrace()
        }
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(modifier: Modifier = Modifier,
               context: Context,
               lifecycle: Lifecycle,
               navController: NavHostController,
               listViewModel: ProjectsListViewModel,
               detailsViewModel: ProjectDetailsViewModel,
               profileViewModel: ProfileViewModel,
               contentPaddingValues: PaddingValues
) {
    val userState by profileViewModel.userState.collectAsState()
    val isSignedIn = userState is User.Authenticated
    LaunchedEffect(Unit) {
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                merge(
                    listViewModel.sideEffectsFlow,
                    detailsViewModel.sideEffectsFlow,
                    profileViewModel.sideEffectsFlow
                ).collect {
                    handleSideEffects(it, navController, context)
                }
            }
        }
    }
    Column(
        modifier.padding(
            top = contentPaddingValues.calculateTopPadding(),
            bottom = contentPaddingValues.calculateBottomPadding()
        )
    ) {
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = modifier
        ) {
            composable<Route.Welcome> {
                WelcomeScreen(modifier, object : WelcomeActions {
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
                LoginScreen(
                    modifier,
                    route.viewType,
                    profileViewModel.userState,
                    object : LoginActions {
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

                    override fun onDeleteAccount() {
                        profileViewModel.deleteAccount()
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
                        profileViewModel.startImportFromSource(context, source)
                    }

                    override fun cancelImport() {
                        profileViewModel.cancelImport()
                        navController.popBackStack()
                    }
                })
            }
            navigation<Route.Home>(startDestination = Route.List) {
                composable<Route.List> {
                    LaunchedEffect(Unit) {
                        listViewModel.clearFilters()
                    }
                    ListScreen(
                        modifier,
                        listViewModel.pagingFlow,
                        listViewModel.searchPhrase,
                        MutableStateFlow(listOf("Kotlin", "Java", "TypeScript", "Bash", "HTML")),
                        object : ListScreenActions {
                            override fun onProjectDetails(project: Project) {
                                detailsViewModel.getProjectDetails(project)
                            }

                            override fun onSearch(phrase: String) {
                                listViewModel.updateSearchPhrase(phrase)
                            }

                            override fun onClear() {
                                //listViewModel.clearProjects()
                            }

                            override fun onAvatarClicked() {
                                navController.navigate(Route.Profile)
                            }
                        })
                }
                composable<Route.Details> {
                    DetailsScreen(modifier, detailsViewModel.stateFlow, isSignedIn, object : DetailsActions {
                        override fun onFavorite(project: Project, favorite: Boolean) {
                            detailsViewModel.toggleFavorite(favorite)
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
                            navController.popBackStack()
                        }
                    })
                }
                composable<Route.Profile> {
                    ProfileScreen(Modifier, profileViewModel.profileEdition.stateFlow, portfolio = emptyList(), object : ProfileActions {
                        override fun onLogin() {
                            navController.navigate(Route.Login(ViewType.Login))
                        }

                        override fun onProjectDetails(project: Project) {
                            detailsViewModel.getProjectDetails(project)
                        }

                        override fun onSaveProfile(profile: Profile) {

                        }

                        override fun onNavigateBack() {
                            navController.popBackStack()
                        }
                    })
                }
                composable<Route.Favorites> {
                    LaunchedEffect(Unit) {
                        listViewModel.updateOnlyFeatured(true)
                    }
                    ListScreen(
                        modifier,
                        listViewModel.pagingFlow,
                        listViewModel.searchPhrase,
                        MutableStateFlow(listOf("Kotlin", "Java", "TypeScript", "Bash", "HTML")),
                        object : ListScreenActions {
                            override fun onProjectDetails(project: Project) {
                                detailsViewModel.getProjectDetails(project)
                            }

                            override fun onSearch(phrase: String) {
                                listViewModel.updateSearchPhrase(phrase)
                            }

                            override fun onClear() {
                                //listViewModel.clearProjects()
                            }

                            override fun onAvatarClicked() {
                                navController.navigate(Route.Profile)
                            }
                        })
                }
                composable<Route.Settings> {
                    SettingsScreen(modifier, profileViewModel.userState, object : SettingsActions {
                        override fun onLogin() {
                            navController.navigate(Route.Login(ViewType.Login))
                        }
                    })
                }
                dialog<Route.AccountsMerge> {
                    AccountsMergeDialog(
                        title = "Accounts Merge",
                        description = "New account will be override existing state. Continue?",
                        onConfirm = {
                            profileViewModel.linkUser(context)
                            navController.popBackStack()
                        },
                        onDismiss = {
                            navController.popBackStack()
                        }
                    )
                }
                dialog<Route.ProviderImport> {
                    ProviderImportDialog(
                        title = "Import Projects",
                        description = "Import projects from a provider?",
                        onConfirm = {
                            navController.popBackStack()
                            profileViewModel.openImportPage()
                        },
                        onDismiss = {
                            navController.popBackStack()
                        }
                    )
                }
                dialog<Route.ProviderImportOngoing> {
                    ImportDialog(
                        title = "Importing Projects",
                        importState = profileViewModel.projectsImportOnboarding.stateFlow,
                        onCancel = {
                            profileViewModel.cancelImport()
                            navController.popBackStack()
                        },
                        onComplete = {
                            listViewModel.refreshProjectsList()
                            navController.popBackStack()
                        }
                    )
                }
                dialog<Route.Error> { backStackEntry ->
                    val route: Route.Error = backStackEntry.toRoute()
                    ErrorDialog(route.title, route.message) {
                        navController.popBackStack()
                    }
                }
                dialog<Route.PrepareProfile> {
                    PrepareProfile(
                        title = "Preparing Profile",
                        profileState = profileViewModel.profileCreationOnboarding.stateFlow,
                        onComplete = {
                            profileViewModel.profileCreated()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
