package com.rk.git

import android.annotation.SuppressLint
import android.content.Context
import android.util.Patterns
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rk.libcommons.alpineHomeDir
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.toast
import com.rk.libcommons.toastCatching
import com.rk.settings.Settings
import com.rk.xededitor.ui.components.InputDialog
import com.rk.xededitor.ui.components.SettingsToggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout

@SuppressLint("AuthLeak")
@Composable
fun SettingsGitScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var isGithub by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("root") }
    var email by remember { mutableStateOf("example@mail.com") }
    var token by remember { mutableStateOf("") }
    var gitUrl by remember { mutableStateOf("github.com") }
    var isLoading by remember { mutableStateOf(true) }


    var showEmailDialog by remember { mutableStateOf(false) }
    var showUserNameDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showGithubUrlDialog by remember { mutableStateOf(false) }

    var inputEmail by remember { mutableStateOf("") }
    var inputUserName by remember { mutableStateOf("") }
    var inputToken by remember { mutableStateOf("") }
    var inputGitUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val gitConfig = loadGitConfig(context)
        username = gitConfig.first
        email = gitConfig.second
        token = getToken(context)

        inputEmail = gitConfig.second
        inputUserName = gitConfig.first
        inputToken = token
        isGithub = Settings.github

        isLoading = false
    }

    PreferenceLayout(label = "Git", backArrowVisible = true) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {

            PreferenceGroup {
                SettingsToggle(
                    label = "Github",
                    description = "Github Domain",
                    default = Settings.github,
                    sideEffect = {
                        Settings.github = it
                        isGithub = it
                    }
                )

                SettingsToggle(label = "Username",
                    description = username,
                    showSwitch = false,
                    default = false,
                    sideEffect = {
                        showUserNameDialog = true
                    })

                SettingsToggle(label = "Email",
                    description = email,
                    showSwitch = false,
                    default = false,
                    sideEffect = {
                        showEmailDialog = true
                    })

                SettingsToggle(label = "Token/Password",
                    description = "Token/Password",
                    showSwitch = false,
                    default = false,
                    sideEffect = {
                        showTokenDialog = true
                    })

                if (!isGithub){
                    SettingsToggle(
                        label = "Custom git domain",
                        description = gitUrl,
                        showSwitch = false,
                        default = false,
                        sideEffect = {
                            showGithubUrlDialog = true
                        }
                    )
                }

            }





            if (showGithubUrlDialog){
                InputDialog(
                    title = "Custom git domain",
                    inputLabel = "github.com",
                    inputValue = inputGitUrl,
                    onInputValueChange = { text ->
                        inputGitUrl = text
                    },
                    onConfirm = {
                        runCatching {
                            updateConfig(context, username,inputGitUrl)
                            gitUrl = inputGitUrl
                            Settings.git_url = gitUrl
                            updateCredentials(context,username,token,inputGitUrl)
                        }.onFailure { toast(it.message) }
                        showGithubUrlDialog = false
                    },
                    onDismiss = {
                        showGithubUrlDialog = false
                        inputGitUrl = gitUrl
                    },
                )
            }

            if (showEmailDialog) {
                InputDialog(
                    title = "Email",
                    inputLabel = "example@email.com",
                    inputValue = inputEmail,
                    onInputValueChange = { text ->
                        inputEmail = text
                    },
                    onConfirm = {
                        runCatching {
                            if (isValidEmail(inputEmail)) {
                                updateConfig(context, username, inputEmail)
                                email = inputEmail
                            } else {
                                inputEmail = email
                                toast("Invalid Email")
                            }
                        }.onFailure { toast(it.message) }
                        showEmailDialog = false
                    },
                    onDismiss = {
                        showEmailDialog = false
                        inputEmail = email
                    },
                )
            }

            if (showUserNameDialog) {
                InputDialog(
                    title = "UserName",
                    inputLabel = "UserName",
                    inputValue = inputUserName,
                    onInputValueChange = { text ->
                        inputUserName = text
                    },
                    onConfirm = {
                        runCatching {
                            if (username.contains(" ").not()) {
                                updateConfig(context, inputUserName, email)
                                username = inputUserName
                            } else {
                                inputUserName = username
                                toast("Invalid Username")
                            }

                        }.onFailure { toast(it.message) }

                        showUserNameDialog = false
                    },
                    onDismiss = {
                        showUserNameDialog = false
                        inputUserName = username
                    },
                )
            }

            if (showTokenDialog) {
                activity?.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
                )


                InputDialog(
                    title = "Token/Password",
                    inputLabel = "Token/Password",
                    inputValue = inputToken,
                    onInputValueChange = { text ->
                        inputToken = text
                    },
                    onConfirm = {
                        toastCatching {
                            if (inputToken.isBlank().not()){
                                updateCredentials(context, username, inputToken,gitUrl)
                                token = inputToken
                            }
                        }
                        showTokenDialog = false
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    },
                    onDismiss = {
                        showTokenDialog = false
                        inputToken = token
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    },
                )

            }
        }
    }
}

suspend fun loadGitConfig(context: Context): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        val config = alpineHomeDir().child(".gitconfig")
        if (config.exists()) {
            runCatching {
                val text = config.readText()
                val matchResult =
                    Regex("""\[user]\s*name\s*=\s*(\S+)\s*email\s*=\s*(\S+)""").find(text)
                val name = matchResult?.groupValues?.get(1) ?: "root"
                val email = matchResult?.groupValues?.get(2) ?: "example@mail.com"
                return@withContext Pair(name, email)
            }.getOrElse {
                Pair("root", "example@mail.com")
            }
        } else {
            Pair("root", "example@mail.com")
        }
    }
}

private fun updateConfig(context: Context, username: String, email: String) {
    val config = alpineHomeDir().child(".gitconfig").createFileIfNot()
    config.writeText(
        """[user]
 name = $username
 email = $email
[color]
 ui = true
 status = true
 branch = true
 diff = true
 interactive = true
[credential]
 helper = store
"""
    )
}

private fun updateCredentials(context: Context, username: String, token: String,gitUrl:String) {
    val cred = alpineHomeDir().child(".git-credentials").createFileIfNot()
    cred.writeText("https://$username:$token@$gitUrl")
}

private inline fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

suspend fun getToken(context: Context): String {
    return withContext(Dispatchers.IO) {
        val gitUrl = Settings.git_url
        val cred = alpineHomeDir().child(".git-credentials")
        if (cred.exists()) {
            val regex = """https://([^:]+):([^@]+)@$gitUrl""".toRegex()
            val matchResult = regex.find(cred.readText())
            return@withContext matchResult?.groupValues?.get(2) ?: ""
        }
        ""
    }
}