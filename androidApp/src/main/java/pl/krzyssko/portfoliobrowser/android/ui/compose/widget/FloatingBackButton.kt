package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FloatingBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(onClick = {
        onClick()
    }, modifier = modifier.size(60.dp), shape = CircleShape, containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)) {
        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
    }
}