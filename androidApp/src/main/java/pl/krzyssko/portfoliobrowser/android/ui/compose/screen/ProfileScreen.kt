package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.Categories
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.ContactList
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.FloatingBackButton
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.ProjectOverview
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Profile
import pl.krzyssko.portfoliobrowser.data.ProfileRole
import pl.krzyssko.portfoliobrowser.data.Project
import pl.krzyssko.portfoliobrowser.data.Stack
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.data.isEmpty
import pl.krzyssko.portfoliobrowser.data.toExperience

interface ProfileActions {
    fun onLogin()
    fun onProjectDetails(project: Project)
    fun onSaveProfile(profile: Profile)
    fun onNavigateBack()
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, profileState: StateFlow<Profile>, userState: StateFlow<User>, portfolio: List<Project>, actions: ProfileActions) {
    val profile by profileState.collectAsState()
    val rawUser by userState.collectAsState()
    if (rawUser is User.Guest || profile.isEmpty()) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Please log in and complete your profile to view this page.")
            Button(onClick = {
                actions.onLogin()
            }, modifier = Modifier.fillMaxWidth(0.5f)) {
                Text("Login")
            }
        }
        return
    }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Row {
                        profile.role.forEach {
                            AssistChip(onClick = { }, label = { Text(it.toString()) })
                        }
                    }
                    Text("${profile.firstName} ${profile.lastName}", fontSize = 24.sp)
                    Text("creates things", fontSize = 16.sp)
                }
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .weight(0.4f)
                        .aspectRatio(0.75f),
                    contentScale = ContentScale.FillHeight
                )
            }
            Column {
                Text("Assets:", fontSize = 16.sp)
                Categories(
                    stack = listOf(
                        Stack("Kotlin", 0.5f),
                        Stack("Java", 0.3f),
                        Stack("Python", 0.2f)
                    )
                )
            }
            Column {
                Text("Years of experience:", fontSize = 16.sp)
                Text(profile.experience.toExperience(), fontSize = 24.sp)
            }
            Column {
                Text("Location", fontSize = 16.sp)
                Text(profile.location, fontSize = 24.sp)
            }
            Column {
                Text("Contact:", fontSize = 16.sp)
                ContactList(contact = profile.contact)
            }
            Column {
                Text("About:", fontSize = 16.sp)
                Text(
                    "Name Surname is a Software Engineer experienced in many different areas of software engineering. \n" +
                            "His primary but not limited to expertise are mobile applications. He worked with different languages, platforms, cloud providers (mainly Azure) and technologies. Krzysztof's secondary focus area is IoT technologies and Edge Computing. He participated in numerous projects in various roles building, designing and presenting cutting-edge software as a senior or leader. Constantly improving his skill set, recently expanding it on Machine Learning.",
                    fontSize = 16.sp
                )
            }
            Text("My portfolio", fontSize = 24.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                portfolio.forEach {
                    ProjectOverview(
                        item = it,
                        stack = it.stack,
                        onItemClick = { item -> actions.onProjectDetails(item) })
                }
            }
        }
        FloatingBackButton(Modifier.padding(top = 8.dp, start = 8.dp)) { actions.onNavigateBack() }
    }
}

private val fakeUser = User.Authenticated(
    account = Account("1", "Krzysztof", "krzy.skorcz@gmail.com", "https://avatars.githubusercontent.com/u/1025101?v=4", true, false),
)
//private val fakeUser = User.Guest
private val fakeProfile = Profile(
    firstName = "Krzysztof",
    lastName = "Skorcz",
    alias = "k-skor",
    role = listOf(ProfileRole.Developer),
    location = "Poznań, Poland",
    contact = emptyList(),
    experience = 10,
)

@Preview(widthDp = 320, heightDp = 640)
@Composable
fun ProfilePreview() {
    MyApplicationTheme {
        ProfileScreen(
            userState = MutableStateFlow(fakeUser),
            profileState = MutableStateFlow(fakeProfile),
            portfolio = emptyList(),
            actions = object : ProfileActions {
                override fun onLogin() {
                    TODO("Not yet implemented")
                }

                override fun onProjectDetails(project: Project) {
                    TODO("Not yet implemented")
                }

                override fun onSaveProfile(profile: Profile) {
                    TODO("Not yet implemented")
                }

                override fun onNavigateBack() {
                    TODO("Not yet implemented")
                }
            })
    }
}