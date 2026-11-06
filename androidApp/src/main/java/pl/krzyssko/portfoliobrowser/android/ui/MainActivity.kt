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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import pl.krzyssko.portfoliobrowser.R
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
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

private val logging = getLogging()

class MainActivity : ComponentActivity() {
    private val profileViewModel: ProfileViewModel by viewModel()
    private val projectViewModel: ProjectViewModel by viewModel()

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
                AppContent(
                    modifier = Modifier.fillMaxSize(),
                    context = this,
                    listViewModel = projectViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.welcome()
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
fun HomeScaffold(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
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
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
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
                                    val profile by profileViewModel.profile.collectAsState(null)
                                    profile?.avatarUrl?.let {
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
                        selected = currentDestination?.hasRoute(topLevelRoute.route::class) == true,
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
        },
        content = content
    )
}

fun handleSideEffects(
    effect: UserSideEffects,
    parentNavController: NavHostController? = null,
    homeNavController: NavHostController? = null,
    context: Context? = null) {
    when (effect) {
        is UserSideEffects.NavigateTo -> handleNavigation(effect, parentNavController, homeNavController)
        is UserSideEffects.Toast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        is UserSideEffects.Trace -> logging.debug(effect.message)
        is UserSideEffects.Error -> effect.throwable?.printStackTrace()
        else -> {}
    }
}

fun handleNavigation(
    effect: UserSideEffects.NavigateTo,
    parentNavController: NavHostController? = null,
    homeNavController: NavHostController? = null,
) {
    when (effect.route) {
        is Route.HomeRoute -> {
            homeNavController?.navigate(effect.route) {
                when (effect.route) {
                    is Route.Home -> {
                        popUpTo(Route.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                    else -> {}
                }
            }
        }
        else -> {
            parentNavController?.navigate(effect.route) {
                when (effect.route) {
                    is Route.Welcome -> {
                        popUpTo(Route.HomeRoot) { inclusive = true }
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    context: Context,
    navController: NavHostController = rememberNavController(),
    listViewModel: ProjectViewModel,
    profileViewModel: ProfileViewModel
) {
    profileViewModel.login.collectSideEffect {
        handleSideEffects(
            it,
            parentNavController = navController,
            context = context
        )
    }
    NavHost(
        navController = navController,
        startDestination = Route.HomeRoot,
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
        navigation<Route.LoginRoot>(startDestination = Route.Login(ViewType.Login)) {
            composable<Route.Login> { backStackEntry ->
                val route: Route.Login = backStackEntry.toRoute()
                val isSignedIn by profileViewModel.isSignedIn.collectAsState(false)
                LoginScreen(
                    modifier,
                    route.viewType,
                    isSignedIn,
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
            dialog<Route.AccountsMerge> {
                AccountsMergeDialog(
                    title = stringResource(id = R.string.accounts_merge),
                    description = stringResource(id = R.string.account_exists_message),
                    onConfirm = {
                        //  TODO: CLear GitHub token
                        profileViewModel.authenticateUser(activity = context, forceSignIn = true)
                        navController.popBackStack()
                    },
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
            }
        }
        navigation<Route.HomeRoot>(startDestination = Route.HomeScaffold) {
            composable<Route.HomeScaffold> {
                val homeNavController = rememberNavController()
                profileViewModel.onboarding.collectSideEffect {
                    handleSideEffects(
                        it,
                        parentNavController = navController,
                        homeNavController = homeNavController,
                        context = context
                    )
                }
                HomeScaffold(
                    modifier = modifier,
                    profileViewModel = profileViewModel,
                    navController = homeNavController
                ) { contentPadding ->
                    Column(
                        modifier.padding(
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding()
                        )
                    ) {
                        HomeContent(
                            modifier = modifier,
                            context = context,
                            parentNavController = navController,
                            navController = homeNavController,
                            projectViewModel = listViewModel,
                            profileViewModel = profileViewModel
                        )
                    }
                }
            }
            navigation<Route.Onboarding>(startDestination = Route.PrepareProfile) {
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
                    val onboarding by profileViewModel.onboarding.collectAsState()
                    ImportDialog(
                        title = "Importing Projects",
                        state = onboarding,
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
                dialog<Route.PrepareProfile> {
                    val onboarding by profileViewModel.onboarding.collectAsState()
                    PrepareProfile(
                        title = "Preparing Profile",
                        state = onboarding,
                        onComplete = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
        dialog<Route.Error> { backStackEntry ->
            val route: Route.Error = backStackEntry.toRoute()
            ErrorDialog(route.title, route.message) {
                navController.popBackStack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    context: Context,
    parentNavController: NavHostController,
    navController: NavHostController,
    projectViewModel: ProjectViewModel,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier
    ) {
        navigation<Route.Home>(startDestination = Route.List) {
            composable<Route.List> {
                val searchPhrase by projectViewModel.searchPhrase.filterNotNull().collectAsState("")
                LaunchedEffect(Unit) {
                    projectViewModel.clearFilters()
                }
                //val filterState by listViewModel.filterState.collectAsState()
                //LaunchedEffect(filterState) {
                //    listViewModel.refreshProjectsList()
                //}
                projectViewModel.list.collectSideEffect {
                    handleSideEffects(
                        it,
                        parentNavController = parentNavController,
                        homeNavController = navController,
                        context = context
                    )
                }
                ListScreen(
                    modifier,
                    projectViewModel.pagingFlow,
                    searchPhrase,
                    listOf(
                        "Kotlin",
                        "Java",
                        "TypeScript",
                        "Bash",
                        "HTML"
                    ),
                    object : ListScreenActions {
                        override fun onProjectDetails(project: Project) {
                            projectViewModel.select(project)
                        }

                        override fun onSearch(phrase: String) {
                            projectViewModel.search(phrase)
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
                val isSignedIn by profileViewModel.isSignedIn.collectAsState(false)
                projectViewModel.details.collectSideEffect {
                    handleSideEffects(
                        it,
                        parentNavController = parentNavController,
                        homeNavController = navController,
                        context = context
                    )
                }
                val details by projectViewModel.details.collectAsState()
                DetailsScreen(
                    modifier,
                    details,
                    isSignedIn,
                    object : DetailsActions {
                        override fun onFavorite(favorite: Boolean) {
                            projectViewModel.toggleFavorite(favorite)
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
        }
        composable<Route.Profile> {
            val profile by profileViewModel.userProfile.collectAsState()
            profileViewModel.userProfile.collectSideEffect {
                handleSideEffects(
                    it,
                    parentNavController = parentNavController,
                    homeNavController = navController,
                    context = context
                )
            }
            ProfileScreen(
                Modifier,
                profile,
                portfolio = emptyList(),
                object : ProfileActions {
                    override fun onLogin() {
                        parentNavController.navigate(Route.Login(ViewType.Login))
                    }

                    override fun onProjectDetails(project: Project) {
                        projectViewModel.select(project)
                    }

                    override fun onSaveProfile(profile: Profile) {

                    }

                    override fun onNavigateBack() {
                        navController.popBackStack()
                    }
                })
        }
        composable<Route.Favorites> {
            val searchPhrase by projectViewModel.searchPhrase.collectAsState(null)
            LaunchedEffect(Unit) {
                // TODO: switch to snapshot flow
                projectViewModel.selectFeatured(true)
            }
            ListScreen(
                modifier,
                projectViewModel.pagingFlow,
                initialPhrase = searchPhrase ?: "",
                listOf(
                        "Kotlin",
                        "Java",
                        "TypeScript",
                        "Bash",
                        "HTML"
                    ),
                object : ListScreenActions {
                    override fun onProjectDetails(project: Project) {
                        projectViewModel.select(project)
                    }

                    override fun onSearch(phrase: String) {
                        projectViewModel.search(phrase)
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
            val isSignedIn by profileViewModel.isSignedIn.collectAsState(false)
            SettingsScreen(
                modifier,
                isSignedIn,
                object : SettingsActions {
                    override fun onLogin() {
                        parentNavController.navigate(Route.Login(ViewType.Login))
                    }
                })
        }
    }
}
