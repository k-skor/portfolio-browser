package pl.krzyssko.portfoliobrowser.android.ui.compose.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pl.krzyssko.portfoliobrowser.android.ui.theme.AppTheme

@Composable
fun ProviderImportDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = description)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun ProviderImportDialogPreview() {
    AppTheme {
        ProviderImportDialog(
            title = "Import Projects",
            description = "Import projects from a provider?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
