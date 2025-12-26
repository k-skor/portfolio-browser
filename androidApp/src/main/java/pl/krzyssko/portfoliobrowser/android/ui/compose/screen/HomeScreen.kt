package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pl.krzyssko.portfoliobrowser.android.ui.AppContent
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.Avatar
import pl.krzyssko.portfoliobrowser.android.ui.handleSideEffects
import pl.krzyssko.portfoliobrowser.android.ui.navigation.topLevelRoutes
import pl.krzyssko.portfoliobrowser.android.viewModel.ProfileViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.Route
import pl.krzyssko.portfoliobrowser.navigation.ViewType
import pl.krzyssko.portfoliobrowser.store.ProfileState

//fun NavGraphBuilder.HomeNavGraph() {
//    navigation<Route.Home>(startDestination = Route.Projects) {
//        HomeScreen()
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier,
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
    //LaunchedEffect(userState) {
    //    if (userState is ProfileState.AuthenticationFailed && (userState as ProfileState.AuthenticationFailed).reason is AuthLinkFailedException) {
    //        scope.launch {
    //            val result = snackHostState.showSnackbar("Current state will be lost, continue?", actionLabel = "OK", withDismissAction = true, duration = SnackbarDuration.Indefinite)
    //            when (result) {
    //                SnackbarResult.ActionPerformed -> {
    //                    profileViewModel.authenticateUser(activity = context, forceSignIn = true)
    //                }
    //                else -> {}
    //            }
    //        }
    //    }
    //}
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearchBarState by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                topLevelRoutes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = {
                            val defaultIcon: @Composable () -> Unit = {
                                Icon(topLevelRoute.icon, topLevelRoute.name, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            when (topLevelRoute.route) {
                                Route.Profile -> Avatar(Modifier.size(30.dp), profileViewModel.profileState, defaultIcon)
                                else -> defaultIcon()
                            }
                        },
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        Box(Modifier.padding(it)) {
            NavHost(
                navController = navController,
                startDestination = Route.Projects,
                modifier = modifier
            ) {
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
                    DetailsScreen(modifier, contentPaddingValues, detailsViewModel.stateFlow, detailsViewModel.projectState, object : DetailsActions {
                        override fun onFavorite(favorite: Boolean) {
                            scope.launch {
                                (user as? User.Authenticated)?.let { user ->
                                    val profile = profileViewModel.profileState.value
                                    val follower = Follower(
                                        uid = user.account.id,
                                        name = "${profile.firstName} ${profile.lastName}"
                                    )
                                    detailsViewModel.toggleFavorite(favorite, follower)
                                }
                            }
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
