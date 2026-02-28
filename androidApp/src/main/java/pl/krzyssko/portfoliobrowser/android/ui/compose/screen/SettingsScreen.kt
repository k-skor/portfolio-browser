package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.R
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppTitle
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme
import pl.krzyssko.portfoliobrowser.data.User

interface SettingsActions {
    fun onLogin()
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, userState: StateFlow<User>, actions: SettingsActions) {
    val user by userState.collectAsState()
    val loginIconRes = if (user is User.Authenticated) R.drawable.baseline_logout_24 else R.drawable.baseline_login_24
    val loginText = if (user is User.Authenticated) "Logout" else "Login"
    Column(modifier.background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTitle()
        LazyColumn(modifier = Modifier.padding(top = 32.dp, start = 8.dp, end = 8.dp)) {
            item {
                FilledTonalButton(onClick = {
                    actions.onLogin()
                }, shape = RectangleShape, contentPadding = PaddingValues(0.dp)) {
                    ListItem(
                        leadingContent = { Icon(painter = painterResource(loginIconRes), contentDescription = loginText) },
                        headlineContent = { Text(loginText) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val fakeUser = User.Guest

@Preview(widthDp = 320, heightDp = 640)
@Composable
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(Modifier.fillMaxSize(), MutableStateFlow(fakeUser), object : SettingsActions {
            override fun onLogin() {
                TODO("Not yet implemented")
            }
        })
    }
}
