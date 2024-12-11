package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.store.ProjectsListState

interface LoginActions {
    fun onGitHubSignIn()
    fun onGitHubSignOut()
}

@Composable
fun AccountScreen(modifier: Modifier = Modifier, contentPaddingValues: PaddingValues,
                  listFlow: StateFlow<ProjectsListState>, actions: LoginActions) {
    val list by listFlow.collectAsState()
    var isSignedIn by remember { mutableStateOf(false) }
    isSignedIn = when(list) {
        is ProjectsListState.Ready, is ProjectsListState.Authenticated -> true
        is ProjectsListState.Idling, is ProjectsListState.Initialized -> false
        else -> isSignedIn
    }
    Surface(
        modifier = modifier.padding(top = contentPaddingValues.calculateTopPadding()),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Login with:", style = MaterialTheme.typography.labelMedium)
            Button(onClick = {
                if (isSignedIn) {
                    actions.onGitHubSignOut()
                } else {
                    actions.onGitHubSignIn()
                }
            }) {
                Text(if (isSignedIn) "GitHut sign out" else "GitHub sign in")
            }
        }
    }
}

val fakeState = ProjectsListState.Idling

@Preview(widthDp = 320)
@Composable
fun AccountPreview() {
    MyApplicationTheme {
        val stateFlow = MutableStateFlow(fakeState)
        AccountScreen(Modifier.fillMaxSize(), PaddingValues(), stateFlow, object : LoginActions {
            override fun onGitHubSignIn() {
                TODO("Not yet implemented")
            }
            override fun onGitHubSignOut() {
                TODO("Not yet implemented")
            }
        })
    }
}