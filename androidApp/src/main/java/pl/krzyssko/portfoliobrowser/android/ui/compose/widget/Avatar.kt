package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun Avatar(modifier: Modifier = Modifier, avatarUrl: String) {
    AsyncImage(
        model = avatarUrl,
        modifier = modifier.size(30.dp).clip(CircleShape),
        contentDescription = "Avatar",
        contentScale = ContentScale.FillWidth
    )
}