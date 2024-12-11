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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.AccountScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.DetailsScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreen
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.ListScreenActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.screen.LoginActions
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppBar
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectDetailsViewModel
import pl.krzyssko.portfoliobrowser.android.viewModel.ProjectViewModel
import pl.krzyssko.portfoliobrowser.platform.Logging
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.store.ProjectsListState
import pl.krzyssko.portfoliobrowser.store.Route
import pl.krzyssko.portfoliobrowser.store.UserSideEffects

class MainActivity : ComponentActivity() {
    private val projectViewModel: ProjectViewModel by viewModel()
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModel()
    private val logging: Logging by inject()

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    projectViewModel.stateFlow.collect { render(it) }
                }
                launch {
                    projectViewModel.sideEffectsFlow.collect { handleSideEffect(it) }
                }
            }
        }
        getLogging().debug("HELLLOOOOOO!!!!")
        setContent {
            MyApplicationTheme {
                PortfolioApp(
                    modifier = Modifier.fillMaxSize(),
                    context = this,
                    coroutineScope = lifecycleScope,
                    listViewModel = projectViewModel,
                    detailsViewModel = projectDetailsViewModel
                )
            }
        }
    }

    private fun render(state: ProjectsListState) {
        logging.debug("new state")
    }

    private fun handleSideEffect(sideEffects: UserSideEffects) {
        when (sideEffects) {
            is UserSideEffects.Trace -> logging.debug(sideEffects.message)
            is UserSideEffects.Toast -> Toast.makeText(this, sideEffects.message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun PortfolioApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    context: Context,
    coroutineScope: CoroutineScope,
    listViewModel: ProjectViewModel,
    detailsViewModel: ProjectDetailsViewModel
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
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
            AppContent(modifier, context, navController, coroutineScope, listViewModel, detailsViewModel, it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(modifier: Modifier = Modifier,
               context: Context,
               navController: NavHostController,
               coroutineScope: CoroutineScope,
               listViewModel: ProjectViewModel,
               detailsViewModel: ProjectDetailsViewModel,
               contentPaddingValues: PaddingValues
) {
    LaunchedEffect("navigation") {
        listViewModel.sideEffectsFlow.collect {
            when (it) {
                is UserSideEffects.NavigateTo -> navController.navigate(it.route)
                else -> {}
            }
        }
    }
    Column {
        NavHost(
            navController = navController,
            startDestination = Route.ProjectsList,
            modifier = modifier
        ) {
            composable<Route.ProjectsList> {
                ListScreen(modifier, contentPaddingValues, listViewModel.pagingFlow, listViewModel.stateFlow, detailsViewModel.stateFlow, object : ListScreenActions {
                    override fun onProjectClicked(name: String?) {
                        name?.let {
                            detailsViewModel.loadProjectWith(name)
                            // TODO: reduce state
                            coroutineScope.launch {
                                detailsViewModel.sideEffectsFlow.collect { effect ->
                                    if (effect is UserSideEffects.NavigateTo) {
                                        navController.navigate(route = Route.ProjectDetails)
                                    }
                                }
                            }
                        }
                    }

                    override fun onSearch(phrase: String) {
                        listViewModel.updateSearchPhrase(phrase)
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
                AccountScreen(modifier, contentPaddingValues, listViewModel.stateFlow, object : LoginActions {
                    override fun onGitHubSignIn() {
                        listViewModel.authenticateUser(context)
                    }

                    override fun onGitHubSignOut() {
                        listViewModel.resetAuthentication()
                    }
                })
            }
        }
    }
}
