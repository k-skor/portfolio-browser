package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import pl.krzyssko.portfoliobrowser.data.Resource

@Composable
fun AppImage(modifier: Modifier = Modifier, image: Resource, contentDescription: String = "App image") {
    when (image) {
        is Resource.NetworkResource -> AsyncImage(
            model = image .url,
            modifier = modifier
                .fillMaxWidth(),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillWidth
        )
        is Resource.LocalResource -> Image(
            painter = painterResource(image.res),
            modifier = modifier
                .fillMaxWidth(),
            contentDescription = contentDescription
        )
    }
}