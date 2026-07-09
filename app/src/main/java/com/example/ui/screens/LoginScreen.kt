package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Lock
import com.example.ui.viewmodel.UserProfile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    val profiles by viewModel.userProfiles.collectAsState()
    var profileToAuthorize by remember { mutableStateOf<UserProfile?>(null) }
    var pinInputText by remember { mutableStateOf("") }
    var pinInputError by remember { mutableStateOf(false) }

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf(0) }
    var phoneError by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }

    // Start countdown timer when OTP is sent
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Brand / Top Icon
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.size(96.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Login Logo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "mobile_login".localize(appLanguage),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure your notepad offline or online with modern phone authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Local Profiles Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "choose_profile_msg".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profiles.forEach { profile ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (profile.id == "secure" && viewModel.settingsManager.pinLockEnabled) {
                                            profileToAuthorize = profile
                                            pinInputText = ""
                                            pinInputError = false
                                        } else {
                                            viewModel.selectProfile(profile.id)
                                            viewModel.loginWithPhone(profile.name)
                                            Toast.makeText(context, "Logged in as ${profile.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(8.dp)
                                    .testTag("login_profile_btn_${profile.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(profile.colorHex)))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.emoji,
                                        fontSize = 28.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "or_text".localize(appLanguage),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Number Card Layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "phone_number".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() || char == '+' || char == '-' || char == ' ' }) {
                                phoneNumber = it
                                phoneError = ""
                            }
                        },
                        placeholder = { Text("phone_placeholder".localize(appLanguage)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_number_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (phoneError.isNotEmpty()) {
                        Text(
                            text = phoneError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button: Send Verification Code
                    Button(
                        onClick = {
                            val digitsOnly = phoneNumber.filter { it.isDigit() }
                            if (digitsOnly.length < 8) {
                                phoneError = "invalid_phone".localize(appLanguage)
                            } else {
                                scope.launch {
                                    // Generate simulated 6-digit OTP
                                    generatedOtp = (100000..999999).random().toString()
                                    isOtpSent = true
                                    countdownSeconds = 30
                                    
                                    val alertMsg = "sms_simulated_alert".localize(appLanguage).format(generatedOtp)
                                    Toast.makeText(context, alertMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = phoneNumber.isNotEmpty() && countdownSeconds == 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_otp_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (countdownSeconds > 0) "Resend in ${countdownSeconds}s" else "send_otp".localize(appLanguage),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated OTP Verification Section
            AnimatedVisibility(
                visible = isOtpSent,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "verification_code".localize(appLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    otpCode = it
                                    otpError = ""
                                }
                            },
                            placeholder = { Text("otp_placeholder".localize(appLanguage)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "OTP Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (otpError.isNotEmpty()) {
                            Text(
                                text = otpError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Help tip showing the generated code so user doesn't get stuck
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "Simulated Code Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Code received: $generatedOtp",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (otpCode == generatedOtp) {
                                    viewModel.loginWithPhone(phoneNumber)
                                    Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    otpError = "invalid_otp".localize(appLanguage)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("verify_otp_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "verify_login".localize(appLanguage),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secondary option: Skip / Guest Mode
            OutlinedButton(
                onClick = {
                    viewModel.loginWithPhone("Guest Mode")
                    Toast.makeText(context, "Continuing in Guest Mode", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("skip_login_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "skip_login".localize(appLanguage),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (profileToAuthorize != null) {
        AlertDialog(
            onDismissRequest = { profileToAuthorize = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "enter_pin_title".localize(appLanguage))
                }
            },
            text = {
                Column {
                    Text(
                        text = "enter_pin_msg".localize(appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInputText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                pinInputText = it
                                pinInputError = false
                            }
                        },
                        label = { Text("verification_code".localize(appLanguage)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = pinInputError,
                        modifier = Modifier.fillMaxWidth().testTag("profile_pin_input")
                    )
                    if (pinInputError) {
                        Text(
                            text = "incorrect_pin".localize(appLanguage),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInputText == viewModel.settingsManager.pinCode) {
                            val targetProfile = profileToAuthorize!!
                            viewModel.selectProfile(targetProfile.id)
                            viewModel.loginWithPhone(targetProfile.name)
                            Toast.makeText(context, "Logged in as ${targetProfile.name}", Toast.LENGTH_SHORT).show()
                            profileToAuthorize = null
                        } else {
                            pinInputError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_profile_pin_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToAuthorize = null }) {
                    Text("cancel".localize(appLanguage))
                }
            }
        )
    }
}
