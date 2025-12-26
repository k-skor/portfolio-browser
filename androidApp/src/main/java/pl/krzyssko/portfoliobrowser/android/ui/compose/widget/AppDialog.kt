package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun AppDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirmation: (() -> Unit)?,
    onDismiss: (() -> Unit)? = onDismissRequest,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(modifier = modifier.wrapContentHeight(),
        icon = {
            Icon(icon, contentDescription = "Dialog icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = onConfirmation?.let {
            {
                TextButton(onClick = {
                    onConfirmation()
                }) {
                    Text("Confirm")
                }
            }
        } ?: {},
        dismissButton = onDismiss?.let {
            {
                TextButton(onClick = {
                    onDismissRequest()
                }) {
                    Text("Dismiss")
                }
            }
        }
    )
}

@Composable
fun ErrorDialog(modifier: Modifier,
                onDismissRequest: () -> Unit,
                throwable: Throwable?
                ) {
    AppDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        onConfirmation = null,
        dialogTitle = "Error",
        dialogText = throwable?.message ?: "Something went wrong...",
        icon = Icons.Default.Warning
    )
}