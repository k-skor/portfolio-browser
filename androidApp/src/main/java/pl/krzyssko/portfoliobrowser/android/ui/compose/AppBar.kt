package pl.krzyssko.portfoliobrowser.android.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.TopAppBarExpandedHeight
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import pl.krzyssko.portfoliobrowser.BuildConfig


/**
 * Experimental for JVM
 */
@ExperimentalMaterial3Api
@Composable
fun AppBar(scrollBehavior: TopAppBarScrollBehavior, onBackPressed: (() -> Unit)? = null) {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Text(BuildConfig.TAG)
        },
        expandedHeight = TopAppBarExpandedHeight,
        navigationIcon = {
            onBackPressed?.let {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}
