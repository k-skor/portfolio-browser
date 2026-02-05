package pl.krzyssko.portfoliobrowser.android.ui.compose.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.store.UserOnboardingImportState

@Composable
fun ImportDialog(
    modifier: Modifier = Modifier,
    title: String,
    importState: StateFlow<UserOnboardingImportState>,
    onCancel: () -> Unit,
) {
    val state by importState.collectAsState()
    Dialog(
        onDismissRequest = onCancel
    ) {
        Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    is UserOnboardingImportState.ImportStarted -> {
                        Text(text = "Importing...")
                    }
                    is UserOnboardingImportState.ImportProgress -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceAround,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                        Text(text = "Error :-(")
                    }
                    else -> {}
                }
            }
        }
    }
}
