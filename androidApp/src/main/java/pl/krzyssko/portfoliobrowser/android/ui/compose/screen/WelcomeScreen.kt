package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppTitle
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme

interface WelcomeActions {
    fun onLogin()
    fun onRegister()
    fun onGuestSignIn()
}

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, actions: WelcomeActions) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        AppTitle()

        Column(Modifier.fillMaxWidth(0.5f).wrapContentHeight()) {
            Button({
                actions.onRegister()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign up")
            }
            OutlinedButton({
                actions.onLogin()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Login")
            }
        }

        Column(Modifier.wrapContentHeight().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalDivider(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp))
            TextButton({
                actions.onGuestSignIn()
            }) {
                Text("Continue as guest")
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 640)
@Composable
fun WelcomePreview() {
    AppTheme {
        WelcomeScreen(actions = object : WelcomeActions {
            override fun onLogin() {}

            override fun onRegister() {}

            override fun onGuestSignIn() {}
        })
    }
}