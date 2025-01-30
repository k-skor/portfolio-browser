package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.User

@Composable
fun Avatar(modifier: Modifier = Modifier, profileState: StateFlow<Profile>, onAvatarClicked: () -> Unit) {
    val profile by profileState.collectAsState()
    //Box(
    //    Modifier
    //        .clickable(onClick = onAvatarClicked)
    //        .then(
    //            modifier.border(
    //                BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    //                CircleShape
    //            )
    //        ), contentAlignment = Alignment.Center
    //) {
    //}
    profile.avatarUrl?.let {
        AsyncImage(
            model = it,
            modifier = modifier.size(30.dp).clip(CircleShape),
            contentDescription = "Avatar",
            contentScale = ContentScale.FillWidth
        )
    } ?: Icon(Icons.Default.AccountCircle, contentDescription = "Avatar", modifier = modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurface)
}