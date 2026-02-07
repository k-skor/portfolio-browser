package pl.krzyssko.portfoliobrowser.android.ui.compose.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme
import pl.krzyssko.portfoliobrowser.store.UserOnboardingProfileState

@Composable
fun PrepareProfile(
    modifier: Modifier = Modifier,
    title: String,
    profileState: StateFlow<UserOnboardingProfileState>,
    onComplete: () -> Unit
) {
    val state by profileState.collectAsState()
    val isDone = state is UserOnboardingProfileState.NewlyCreated
    Dialog({}) {
        Card(shape = MaterialTheme.shapes.medium) {
            Column(modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Box(modifier = Modifier.padding(vertical = 64.dp)) {
                    if (isDone) {
                        Text(text = "Hello ${(state as UserOnboardingProfileState.NewlyCreated).userName}!")
                    } else {
                        Text(text = "Creating profile...")
                    }
                }
                if (isDone) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                        TextButton(onClick = {
                            onComplete()
                        }) {
                            Text(text = "Explore")
                        }
                    }
                }
            }
        }
    }
}

@Preview(heightDp = 240)
@Composable
fun PrepareProfilePreview() {
    AppTheme {
        PrepareProfile(
            title = "Preparing Profile",
            profileState = MutableStateFlow(UserOnboardingProfileState.NewlyCreated("Michael")),
            onComplete = {}
        )
    }
}
