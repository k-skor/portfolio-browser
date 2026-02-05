package pl.krzyssko.portfoliobrowser.android.ui.compose.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.store.ProfileState

interface PrepareProfileActions {
    fun onDialogLoad()
    fun onComplete()
}

@Composable
fun PrepareProfile(
    modifier: Modifier = Modifier,
    title: String,
    profileState: StateFlow<ProfileState>,
    actions: PrepareProfileActions
) {
    val state by profileState.collectAsState()
    LaunchedEffect(state) {
        actions.onDialogLoad()
        when (state) {
            is ProfileState.ProfileCreated -> actions.onComplete()
            else -> {}
        }
    }
    Dialog({}) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Box(modifier = Modifier.weight(1f)) {
                Text(text = "Creating profile...")
            }
        }
    }
}