package pl.krzyssko.portfoliobrowser.android.ui.compose.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun AppTitle(modifier: Modifier = Modifier) {
    Text("Portfolio-\nBrowser", modifier = modifier.padding(top = 32.dp), fontSize = 36.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
}
