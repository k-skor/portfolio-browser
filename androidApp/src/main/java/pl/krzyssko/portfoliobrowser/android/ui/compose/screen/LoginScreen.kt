package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.android.ui.compose.widget.AppTitle
import pl.krzyssko.portfoliobrowser.data.Account
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.data.User
import pl.krzyssko.portfoliobrowser.navigation.ViewType

interface LoginActions {
    fun onGitHubSignIn()
    fun onGitHubSignOut()
    fun onGitHubLink()
    fun onEmailCreate(login: String, password: String)
    fun onEmailSignIn(login: String, password: String)
    fun onDeleteAccount()
}

interface ImportActions {
    fun importFromSource(source: Source)
    fun cancelImport()
}

enum class Error {
    RequirementsNotMet,
    Confirmation
}

@Composable
fun Divider() {
    HorizontalDivider(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp))
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues,
    viewType: ViewType,
    userFlow: StateFlow<User>,
    actions: LoginActions,
    welcomeActions: WelcomeActions,
    importActions: ImportActions
) {
    val user by userFlow.collectAsState()
    val isSignedIn = user is User.Authenticated
    //Box(
    //    modifier = modifier
    //) {
    //}
    Column(verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppTitle()
            if (isSignedIn && viewType != ViewType.SourceSelection) {
                Text("You are currently signed in.", Modifier.padding(vertical = 16.dp))
                Column(Modifier.fillMaxWidth(0.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = {
                        actions.onGitHubSignOut()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out")
                    }
                    Button(onClick = {
                        actions.onDeleteAccount()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete account")
                    }
                }
                return
            }
            when (viewType) {
                ViewType.Login -> {
                    Text("Login to your account via:", Modifier.padding(vertical = 16.dp))
                }
                ViewType.Register -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sign up to your account via:")
                        Text("Signing up via GitHub will allow you to directly download your project libraries.")
                    }
                }
                ViewType.SourceSelection -> {
                    Text("Select source to import projects:", Modifier.padding(vertical = 16.dp))
                }
            }
            Column(Modifier.fillMaxWidth(0.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    if (viewType == ViewType.SourceSelection) {
                        importActions.importFromSource(Source.GitHub)
                    } else {
                        actions.onGitHubSignIn()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "GitHub")
                        Text(" GitHub")
                    }
                }
            }
        }

        when (viewType) {
            ViewType.Login -> LoginFlowScreen(actions = actions)
            ViewType.Register -> RegisterFlowScreen(actions = actions)
            else -> { }
        }

        Column(
            Modifier
                .wrapContentHeight()
                .padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Divider()
            when (viewType) {
                ViewType.Login -> {
                    Row {
                        Text("Don't have an account? ", modifier = Modifier.alignBy { it.measuredHeight })
                        TextButton({
                            welcomeActions.onRegister()
                        }) {
                            Text("Sign up now")
                        }
                    }
                }
                ViewType.Register -> {
                    Row {
                        TextButton({
                            welcomeActions.onGuestSignIn()
                        }, modifier = Modifier.alignByBaseline()) {
                            Text("Continue as a guest")
                        }
                        Text(" or ")
                        TextButton({
                            welcomeActions.onLogin()
                        }, modifier = Modifier.alignByBaseline()) {
                            Text("Log in")
                        }
                    }
                }
                ViewType.SourceSelection -> {
                    Row {
                        Text("Or ", modifier = Modifier.alignByBaseline())
                        TextButton({
                            importActions.cancelImport()
                        }, modifier = Modifier.alignBy({ it.measuredHeight / 2 })) {
                            Text("skip", Modifier.padding(0.dp))
                        }
                        Text(" this step.", modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
    }
}

@Composable
fun LoginFlowScreen(modifier: Modifier = Modifier, actions: LoginActions) {
    EmailForm(modifier, false, actions)
}

@Composable
fun RegisterFlowScreen(modifier: Modifier = Modifier, actions: LoginActions) {
    EmailForm(modifier, isRegisterFlow = true, actions = actions)
}

@Composable
fun EmailForm(modifier: Modifier = Modifier, isRegisterFlow: Boolean, actions: LoginActions) {
    Column(modifier.wrapContentHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ////var createOrSignIn by remember { mutableStateOf(true) }
        ////Text("Login with:", style = MaterialTheme.typography.labelMedium)
        //Button(onClick = {
        //    actions.onGitHubSignIn()
        //}) {
        //    Text("GitHub sign in")
        //}
        Divider()
        Text("Or ${if (isRegisterFlow) "Sign up" else "Login"} with e-mail")
        //Row(verticalAlignment = Alignment.CenterVertically) {
        //    Text("Login")
        //    Switch(createOrSignIn, onCheckedChange = { createOrSignIn = it })
        //    Text("Create")
        //}
        val login = remember { mutableStateOf<String?>(null) }
        val password = remember { mutableStateOf<String?>(null) }
        val confirm = remember { mutableStateOf<String?>(null) }
        val loginError = remember { mutableStateOf<Error?>(null) }
        val passwordError = remember { mutableStateOf<Error?>(null) }
        val confirmationError = remember { mutableStateOf<Error?>(null) }
        if (isRegisterFlow) {
            EmailCreate(
                modifier,
                loginState = login,
                passwordState = password,
                confirmState = confirm,
                passwordValidationError = passwordError,
                confirmationValidationError = confirmationError
            )
        } else {
            EmailSignIn(modifier, loginState = login, passwordState = password, passwordError)
        }
        Column(Modifier.fillMaxWidth(0.5f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                val rawLogin by login
                val loginValue = rawLogin?.trim()?.lowercase()
                loginError.value = null
                passwordError.value = null
                confirmationError.value = null
                if (loginValue == null || !Regex("^((?!\\.)[\\w-_.]*[^.])(@\\w+)(\\.\\w+(\\.\\w+)?[^.\\W])$").matches(
                        loginValue as CharSequence
                    )
                ) {
                    loginError.value = Error.RequirementsNotMet
                }
                val passwordValue by password
                if (passwordValue.isNullOrEmpty() || passwordValue?.length?.compareTo(32)!! > 0) {
                    passwordError.value = Error.RequirementsNotMet
                }
                if (passwordError.value != null || loginError.value != null) {
                    return@Button
                }
                if (isRegisterFlow) {
                    val confirmValue by confirm
                    if (passwordValue != confirmValue) {
                        passwordError.value = Error.Confirmation
                        confirmationError.value = Error.Confirmation
                        return@Button
                    }
                    actions.onEmailCreate(loginValue!!, passwordValue!!)
                } else {
                    actions.onEmailSignIn(loginValue!!, passwordValue!!)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isRegisterFlow) "Create account" else "Sign in")
            }
        }
    }
}

@Composable
fun EmailCreate(
    modifier: Modifier = Modifier,
    loginState: MutableState<String?>,
    passwordState: MutableState<String?>,
    confirmState: MutableState<String?>,
    passwordValidationError: MutableState<Error?>,
    confirmationValidationError: MutableState<Error?>
) {
    var login by loginState
    var password by passwordState
    var confirm by confirmState
    val passwordError by passwordValidationError
    val confirmationError by confirmationValidationError
    Column(modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = login.orEmpty(), onValueChange = {
            login = it
        }, label = {
            Text("Login")
        })
        OutlinedTextField(value = password.orEmpty(), onValueChange = {
            password = it
        }, label = {
            Text("Password")
        },
            isError = passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        OutlinedTextField(value = confirm.orEmpty(), onValueChange = {
            confirm = it
        }, label = {
            Text("Confirm password")
        },
            isError = confirmationError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@Composable
fun EmailSignIn(modifier: Modifier = Modifier, loginState: MutableState<String?>, passwordState: MutableState<String?>, passwordValidationError: MutableState<Error?>
) {
    var login by loginState
    var password by passwordState
    val passwordError by passwordValidationError
    Column(modifier) {
        OutlinedTextField(value = login.orEmpty(), onValueChange = {
            login = it
        }, label = {
            Text("Login")
        })
        OutlinedTextField(value = password.orEmpty(), onValueChange = {
            password = it
        }, label = {
            Text("Password")
        },
            isError = passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

//private val fakeUser = User.Guest
private val fakeUser = User.Authenticated(
    account = Account("1", "Krzysztof", "krzy.skorcz@gmail.com", "https://avatars.githubusercontent.com/u/1025101?v=4", true, false),
)

@Preview(widthDp = 320, heightDp = 640)
@Composable
fun LoginPreview() {
    MyApplicationTheme {
        val stateFlow = MutableStateFlow(fakeUser)
        LoginScreen(Modifier.fillMaxSize(), PaddingValues(), ViewType.Login, stateFlow, object : LoginActions {
            override fun onGitHubSignIn() {
                TODO("Not yet implemented")
            }
            override fun onGitHubSignOut() {
                TODO("Not yet implemented")
            }

            override fun onGitHubLink() {
                TODO("Not yet implemented")
            }

            override fun onEmailCreate(login: String, password: String) {
                TODO("Not yet implemented")
            }

            override fun onEmailSignIn(login: String, password: String) {
                TODO("Not yet implemented")
            }

            override fun onDeleteAccount() {
                TODO("Not yet implemented")
            }
        }, object: WelcomeActions {
            override fun onLogin() {
                TODO("Not yet implemented")
            }

            override fun onRegister() {
                TODO("Not yet implemented")
            }

            override fun onGuestSignIn() {
                TODO("Not yet implemented")
            }
        }, object : ImportActions {
            override fun importFromSource(source: Source) {
                TODO("Not yet implemented")
            }

            override fun cancelImport() {
                TODO("Not yet implemented")
            }
        })
    }
}