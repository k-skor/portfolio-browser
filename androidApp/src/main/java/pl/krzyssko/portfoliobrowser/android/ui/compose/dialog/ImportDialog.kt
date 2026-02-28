package pl.krzyssko.portfoliobrowser.android.ui.compose.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.store.UserOnboardingImportState

@Composable
fun ImportDialog(
    modifier: Modifier = Modifier,
    title: String,
    importState: StateFlow<UserOnboardingImportState>,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    val state by importState.collectAsState()
    val isDone = state is UserOnboardingImportState.ImportCompleted
    Dialog(
        onDismissRequest = onCancel
    ) {
        Card(shape = MaterialTheme.shapes.medium) {
            Column(modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Box(modifier = Modifier.padding(vertical = 64.dp)) {
                    when (state) {
                        is UserOnboardingImportState.ImportStarted -> {
                            Text(text = "Working...")
                        }
                        is UserOnboardingImportState.ImportProgress -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val data = (state as UserOnboardingImportState.ImportProgress)
                                val text = "${data.progress}/${data.total} Importing ${data.displayName}"
                                Text(text = text, style = MaterialTheme.typography.bodyMedium)
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                )
                            }
                        }
                        is UserOnboardingImportState.ImportCompleted -> {
                            Text(text = "Completed!")
                        }
                        is UserOnboardingImportState.ImportError -> {
                            Text(text = "Error 💀")
                        }
                        else -> {}
                    }
                }
                if (isDone) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                        TextButton(onClick = {
                            onComplete()
                        }) {
                            Text(text = "Close")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ImportDialogPreview() {
    MyApplicationTheme {
        ImportDialog(
            title = "Importing Data",
            importState = MutableStateFlow(UserOnboardingImportState.ImportProgress(50, 100, "Projects")),
            onCancel = {},
            onComplete = {}
        )
    }
}
