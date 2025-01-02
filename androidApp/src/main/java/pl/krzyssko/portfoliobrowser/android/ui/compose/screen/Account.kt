package pl.krzyssko.portfoliobrowser.android.ui.compose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.krzyssko.portfoliobrowser.android.MyApplicationTheme
import pl.krzyssko.portfoliobrowser.data.User

interface LoginActions {
    fun onGitHubSignIn()
    fun onGitHubSignOut()
    fun onGitHubLink()
    fun onEmailCreate(login: String, password: String)
    fun onEmailSignIn(login: String, password: String)
}

enum class Error {
    RequirementsNotMet,
    Confirmation
}

@Composable
fun AccountScreen(modifier: Modifier = Modifier, contentPaddingValues: PaddingValues,
                  userFlow: StateFlow<User>, actions: LoginActions) {
    val user by userFlow.collectAsState()
    val isSignedIn = user is User.Authenticated
    Surface(
        modifier = modifier.padding(top = contentPaddingValues.calculateTopPadding()),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSignedIn) {
                Button(onClick = {
                    actions.onGitHubSignOut()
                }) {
                    Text("Sign out")
                }
                Button(onClick = {
                    actions.onGitHubLink()
                }) {
                    Text("Link with GitHub")
                }
            } else {
                var createOrSignIn by remember { mutableStateOf(true) }
                Text("Login with:", style = MaterialTheme.typography.labelMedium)
                Button(onClick = {
                    actions.onGitHubSignIn()
                }) {
                    Text("GitHub sign in")
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Login")
                    Switch(createOrSignIn, onCheckedChange = { createOrSignIn = it })
                    Text("Create")
                }
                val login = remember { mutableStateOf<String?>(null) }
                val password = remember { mutableStateOf<String?>(null) }
                val confirm = remember { mutableStateOf<String?>(null) }
                val loginError = remember { mutableStateOf<Error?>(null) }
                val passwordError = remember { mutableStateOf<Error?>(null) }
                val confirmationError = remember { mutableStateOf<Error?>(null) }
                if (createOrSignIn) {
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
                Button(onClick = {
                    val rawLogin by login
                    val loginValue = rawLogin?.trim()?.lowercase()
                    loginError.value = null
                    passwordError.value = null
                    confirmationError.value = null
                    if (loginValue == null || !Regex("^((?!\\.)[\\w-_.]*[^.])(@\\w+)(\\.\\w+(\\.\\w+)?[^.\\W])$").matches(loginValue as CharSequence)) {
                        loginError.value = Error.RequirementsNotMet
                    }
                    val passwordValue by password
                    if (passwordValue.isNullOrEmpty() || passwordValue?.length?.compareTo(32)!! > 0) {
                        passwordError.value = Error.RequirementsNotMet
                    }
                    if (passwordError.value != null || loginError.value != null) {
                        return@Button
                    }
                    if (createOrSignIn) {
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
                }) {
                    Text(if (createOrSignIn) "Create account" else "Sign in")
                }
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
    Column(Modifier.verticalScroll(rememberScrollState())) {
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
    Column {
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

@Preview(widthDp = 320, heightDp = 320)
@Composable
fun AccountPreview() {
    MyApplicationTheme {
        val stateFlow = MutableStateFlow(fakeUser)
        AccountScreen(Modifier.fillMaxSize(), PaddingValues(), stateFlow, object : LoginActions {
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
        })
    }
}