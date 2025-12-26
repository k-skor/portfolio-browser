package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.data.Profile

@Composable
fun Avatar(modifier: Modifier = Modifier, profileState: StateFlow<Profile>, default: @Composable () -> Unit) {
    val profile by profileState.collectAsState()
    profile.avatarUrl?.let {
        AsyncImage(
            model = it,
            modifier = modifier.size(30.dp).clip(CircleShape),
            contentDescription = "Avatar",
            contentScale = ContentScale.FillWidth
        )
    } ?: default()
}