package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import pl.krzyssko.portfoliobrowser.data.Resource

@Composable
fun AppImage(
    modifier: Modifier = Modifier,
    image: Resource,
    contentDescription: String = "App image",
    contentScale: ContentScale = ContentScale.FillWidth
) {
    when (image) {
        is Resource.NetworkResource -> AsyncImage(
            model = image.url,
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = contentScale
        )
        is Resource.LocalResource -> Image(
            painter = painterResource(image.res),
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = contentScale
        )
    }
}