package com.chehartemple.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chehartemple.app.R
import com.chehartemple.app.data.api.ApiErrorParser
import com.chehartemple.app.data.api.DeviceUtils
import com.chehartemple.app.data.api.RetrofitClient
import com.chehartemple.app.data.model.AuthRequest
import com.chehartemple.app.data.model.SignupRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID = "322132120457-8tj6djij0pa6o1bpq0a6qm4h2kgo20pr.apps.googleusercontent.com"

private val BLOCKED_DOMAINS = setOf(
    "yopmail.com","mailinator.com","guerrillamail.com","tempmail.com","temp-mail.org",
    "throwaway.email","fakeinbox.com","trashmail.com","10minutemail.com","dispostable.com",
    "maildrop.cc","getnada.com","sharklasers.com","guerrillamail.net","mailnesia.com",
    "burnermail.io","grr.la","spam4.me","emailfake.com","tempmailo.com"
)

private fun isDisposableEmail(email: String): Boolean {
    val domain = email.substringAfter("@", "").lowercase()
    return domain in BLOCKED_DOMAINS
}

private fun getPasswordErrors(password: String): List<String> {
    val errors = mutableListOf<String>()
    if (password.length < 8) errors.add("At least 8 characters")
    if (!password.any { it.isUpperCase() }) errors.add("One uppercase letter")
    if (!password.any { it.isLowerCase() }) errors.add("One lowercase letter")
    if (!password.any { it.isDigit() }) errors.add("One digit")
    return errors
}

@Composable
fun AuthScreen(onLoginSuccess: (accessToken: String, refreshToken: String, sessionToken: String?) -> Unit, onSkip: () -> Unit = {}) {
    var screenState by remember { mutableStateOf("login") } // login, signup, otp
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Resend cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown--
        }
    }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loading = true
                scope.launch {
                    try {
                        val deviceInfo = DeviceUtils.getDeviceInfo(context)
                        val body = mapOf<String, Any>(
                            "idToken" to idToken,
                            "deviceInfo" to mapOf(
                                "deviceId" to (deviceInfo.deviceId ?: ""),
                                "deviceName" to (deviceInfo.deviceName ?: ""),
                                "deviceModel" to (deviceInfo.deviceModel ?: ""),
                                "osName" to (deviceInfo.osName ?: ""),
                                "osVersion" to (deviceInfo.osVersion ?: ""),
                                "appVersion" to (deviceInfo.appVersion ?: "")
                            )
                        )
                        val response = RetrofitClient.api.googleLogin(body)
                        onLoginSuccess(response.accessToken, response.refreshToken, response.sessionToken)
                    } catch (e: Exception) {
                        message = ApiErrorParser.parse(e)
                    }
                    loading = false
                }
            } else {
                message = "Failed to get ID token from Google"
            }
        } catch (e: ApiException) {
            message = when (e.statusCode) {
                10 -> "Developer error: SHA-1 not configured in Google Cloud Console"
                7 -> "Network error. Check internet connection"
                12500 -> "Google sign-in failed. Update Google Play Services"
                12501 -> "Google sign-in cancelled"
                else -> "Google sign-in error code: ${e.statusCode}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9F0))) {

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.splash_icon),
            contentDescription = "Chehar Maa",
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Chehar Temple", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF800000))
        Spacer(Modifier.height(4.dp))

        when (screenState) {
            "otp" -> {
                Text("Verify Your Email", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B5B4B))
                Spacer(Modifier.height(8.dp))
                Text("OTP sent to $email", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = { Text("Enter 6-digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading
                )

                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = if (message.contains("success", true)) Color(0xFF2E7D32) else Color(0xFFB22222), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        loading = true; message = ""
                        scope.launch {
                            try {
                                val response = RetrofitClient.api.verifyOtp(mapOf("email" to email, "otp" to otp))
                                message = response.message
                                // After successful verification, go to login
                                delay(1500)
                                screenState = "login"
                                message = "Email verified! Please login."
                                otp = ""
                            } catch (e: Exception) { message = ApiErrorParser.parse(e) }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading && otp.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFFFD700), strokeWidth = 2.dp)
                    else Text("Verify OTP", fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD700))
                }

                Spacer(Modifier.height(14.dp))

                // Resend OTP button with cooldown
                TextButton(
                    onClick = {
                        loading = true; message = ""
                        scope.launch {
                            try {
                                val response = RetrofitClient.api.resendOtp(mapOf("email" to email))
                                message = response.message
                                resendCooldown = 60
                            } catch (e: Exception) { message = ApiErrorParser.parse(e) }
                            loading = false
                        }
                    },
                    enabled = !loading && resendCooldown == 0
                ) {
                    Text(
                        if (resendCooldown > 0) "Resend OTP in ${resendCooldown}s" else "Resend OTP",
                        color = if (resendCooldown > 0) Color.Gray else Color(0xFF800000)
                    )
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { screenState = "login"; message = "" }, enabled = !loading) {
                    Text("Back to Login", color = Color(0xFF800000))
                }
            }

            else -> {
                val isLogin = screenState == "login"
                Text(if (isLogin) "Welcome back" else "Create account", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B5B4B))
                Spacer(Modifier.height(28.dp))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        message = ""
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Google", color = Color(0xFF333333), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(16.dp))

                // Divider
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Divider(Modifier.weight(1f), color = Color(0xFFE0E0E0))
                    Text("  or  ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Divider(Modifier.weight(1f), color = Color(0xFFE0E0E0))
                }

                Spacer(Modifier.height(16.dp))

                if (!isLogin) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !loading)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !loading)
                if (!isLogin && email.contains("@") && isDisposableEmail(email)) {
                    Text("⚠️ Temporary/disposable emails are not allowed.", color = Color(0xFFB22222), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading
                )
                if (!isLogin) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !loading)
                }
                if (!isLogin && password.isNotEmpty() && getPasswordErrors(password).isNotEmpty()) {
                    Text("Password needs: ${getPasswordErrors(password).joinToString(", ")}", color = Color(0xFFB22222), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp))
                }

                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = if (message.contains("success", true) || message.contains("verified", true)) Color(0xFF2E7D32) else Color(0xFFB22222), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        loading = true; message = ""
                        scope.launch {
                            try {
                                if (isLogin) {
                                    val deviceInfo = DeviceUtils.getDeviceInfo(context)
                                    val response = RetrofitClient.api.login(AuthRequest(email, password, deviceInfo))
                                    onLoginSuccess(response.accessToken, response.refreshToken, response.sessionToken)
                                } else {
                                    val response = RetrofitClient.api.signup(SignupRequest(name, email, password, mobile))
                                    message = response.message
                                    // Navigate to OTP screen
                                    screenState = "otp"
                                    resendCooldown = 60
                                }
                            } catch (e: Exception) { message = ApiErrorParser.parse(e) }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !loading && if (!isLogin) !isDisposableEmail(email) && getPasswordErrors(password).isEmpty() else true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFFFD700), strokeWidth = 2.dp)
                    else Text(if (isLogin) "Login" else "Sign Up", fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD700))
                }

                Spacer(Modifier.height(14.dp))
                TextButton(onClick = { screenState = if (isLogin) "signup" else "login"; message = "" }, enabled = !loading) {
                    Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login", color = Color(0xFF800000))
                }
            }
        }
    }
    } // Box
}
