package pl.krzyssko.portfoliobrowser.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.krzyssko.portfoliobrowser.BuildConfig
import pl.krzyssko.portfoliobrowser.Greeting

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val greeting = Greeting().greet()
        setContent {
            MyApp(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier, names: List<String> = listOf("Android", "Compose")) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(modifier) {
            for (name in names) {
                GreetingView(name, modifier)
            }
        }
    }
}

@Composable
fun GreetingView(name: String, modifier: Modifier = Modifier) {
    val expanded = remember { mutableStateOf(false) }
    val extraPadding = if (expanded.value) 48.dp else 0.dp
    Surface(color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
        Row(modifier = Modifier.padding(24.dp)) {
            Column(modifier = Modifier.weight(1f).padding(bottom = extraPadding)) {
                Text(text = "Hello,")
                Text(text = "$name!")
            }
            ElevatedButton(onClick = { expanded.value = !expanded.value }) {
                Text(if (expanded.value) "Show less" else "Show more")
            }
        }
    }
}

@Preview(widthDp = 320)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        MyApp()
    }
}
