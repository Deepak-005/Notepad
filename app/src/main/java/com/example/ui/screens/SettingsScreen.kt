package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.Language
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.UpdateState
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    var theme by remember { mutableStateOf(viewModel.settingsManager.theme) }
    var fontSize by remember { mutableStateOf(viewModel.settingsManager.fontSize) }
    var fontFamily by remember { mutableStateOf(viewModel.settingsManager.fontFamily) }
    var pinLockEnabled by remember { mutableStateOf(viewModel.settingsManager.pinLockEnabled) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    // JSON file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
                inputStream?.close()

                viewModel.restoreFromBackupJson(stringBuilder.toString()) { success ->
                    if (success) {
                        Toast.makeText(context, "Notes restored successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("settings".localize(appLanguage), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "appearance".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Theme Dropdown Selector
            var showThemeMenu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("theme".localize(appLanguage)) },
                supportingContent = { Text(theme) },
                leadingContent = { Icon(Icons.Default.Palette, null) },
                modifier = Modifier
                    .clickable { showThemeMenu = true }
                    .testTag("theme_setting_item"),
                trailingContent = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                            listOf("System", "Light", "Dark").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        theme = t
                                        viewModel.settingsManager.theme = t
                                        viewModel.appTheme.value = t
                                        showThemeMenu = false
                                        Toast.makeText(context, "Theme set to $t", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // Font Size Dropdown Selector
            var showFontSizeMenu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("font_size".localize(appLanguage)) },
                supportingContent = { Text(fontSize) },
                leadingContent = { Icon(Icons.Default.FormatSize, null) },
                modifier = Modifier.clickable { showFontSizeMenu = true },
                trailingContent = {
                    Box {
                        IconButton(onClick = { showFontSizeMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showFontSizeMenu, onDismissRequest = { showFontSizeMenu = false }) {
                            listOf("Small", "Medium", "Large").forEach { fs ->
                                DropdownMenuItem(
                                    text = { Text(fs) },
                                    onClick = {
                                        fontSize = fs
                                        viewModel.settingsManager.fontSize = fs
                                        showFontSizeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // Font Family Dropdown Selector
            var showFontFamilyMenu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("font_style".localize(appLanguage)) },
                supportingContent = { Text(fontFamily) },
                leadingContent = { Icon(Icons.Default.FontDownload, null) },
                modifier = Modifier.clickable { showFontFamilyMenu = true },
                trailingContent = {
                    Box {
                        IconButton(onClick = { showFontFamilyMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showFontFamilyMenu, onDismissRequest = { showFontFamilyMenu = false }) {
                            listOf("Sans-serif", "Serif", "Monospace").forEach { ff ->
                                DropdownMenuItem(
                                    text = { Text(ff) },
                                    onClick = {
                                        fontFamily = ff
                                        viewModel.settingsManager.fontFamily = ff
                                        showFontFamilyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // Language Dropdown Selector
            var showLanguageMenu by remember { mutableStateOf(false) }
            val currentLanguageObj = remember(appLanguage) { Language.fromCode(appLanguage) }
            ListItem(
                headlineContent = { Text("language".localize(appLanguage)) },
                supportingContent = { Text(currentLanguageObj.displayName) },
                leadingContent = { Icon(Icons.Default.Language, null) },
                modifier = Modifier
                    .clickable { showLanguageMenu = true }
                    .testTag("language_setting_item"),
                trailingContent = {
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                            Language.entries.forEach { langObj ->
                                DropdownMenuItem(
                                    text = { Text(langObj.displayName) },
                                    onClick = {
                                        viewModel.settingsManager.language = langObj.code
                                        viewModel.appLanguage.value = langObj.code
                                        showLanguageMenu = false
                                        Toast.makeText(context, "${langObj.displayName} selected", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "account".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val loggedInPhone by viewModel.loggedInPhone.collectAsState()
            val phoneText = if (loggedInPhone.isNotEmpty() && loggedInPhone != "Guest Mode") loggedInPhone else "guest_user".localize(appLanguage)

            ListItem(
                headlineContent = { Text("mobile_login".localize(appLanguage)) },
                supportingContent = { Text("login_status".localize(appLanguage).format(phoneText)) },
                leadingContent = { Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = {
                    Button(
                        onClick = {
                            viewModel.logout()
                            Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("logout".localize(appLanguage))
                    }
                },
                modifier = Modifier.testTag("account_status_item")
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "security".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // App Lock PIN Toggle
            ListItem(
                headlineContent = { Text("pin_lock".localize(appLanguage)) },
                supportingContent = { Text(if (pinLockEnabled) "pin_lock_desc".localize(appLanguage) else "no_lock".localize(appLanguage)) },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                trailingContent = {
                    Switch(
                        checked = pinLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                viewModel.disablePin()
                                pinLockEnabled = false
                                Toast.makeText(context, "pin_disabled".localize(appLanguage), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("pin_lock_switch")
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "data_management".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Backup button
            Card(
                onClick = { viewModel.backupToBackupFile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("backup_notes_button"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("backup_json".localize(appLanguage), fontWeight = FontWeight.Bold)
                        Text("backup_json_desc".localize(appLanguage), fontSize = 12.sp)
                    }
                }
            }

            // Restore button
            Card(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("restore_notes_button"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("restore_json".localize(appLanguage), fontWeight = FontWeight.Bold)
                        Text("restore_json_desc".localize(appLanguage), fontSize = 12.sp)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "about_and_updates".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("app_update_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("app_version".localize(appLanguage), fontWeight = FontWeight.Bold)
                            Text("v$appVersion", fontSize = 14.sp)
                        }

                        // Check / Status UI elements
                        when (updateState) {
                            is UpdateState.Idle, is UpdateState.UpToDate -> {
                                TextButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.testTag("check_updates_button")
                                ) {
                                    Text("check_updates".localize(appLanguage))
                                }
                            }
                            is UpdateState.Checking -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("checking_updates".localize(appLanguage), fontSize = 12.sp)
                                }
                            }
                            else -> {}
                        }
                    }

                    if (updateState is UpdateState.UpToDate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("up_to_date".localize(appLanguage), color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Download / Install UI components
                    when (updateState) {
                        is UpdateState.UpdateAvailable -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "update_available".localize(appLanguage),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "changelog_title".localize(appLanguage),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "changelog_details".localize(appLanguage),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.downloadAndInstallUpdate() },
                                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("download_update_button")
                                    ) {
                                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("download_install".localize(appLanguage), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            val progress = (updateState as UpdateState.Downloading).progress
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("downloading_update".localize(appLanguage), fontSize = 12.sp)
                                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        is UpdateState.Installing -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text("installing_update".localize(appLanguage), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        is UpdateState.Installed -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Update Installed!",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "update_installed_msg".localize(appLanguage),
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.resetUpdateState()
                                            Toast.makeText(context, "App simulated restart successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("restart_app_button")
                                    ) {
                                        Text("restart_app".localize(appLanguage), fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Keyboard Shortcuts Help",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text("• Ctrl + N : Create New Note\n• Ctrl + S : Save Note\n• Ctrl + F : Search Notes\n• Esc : Navigate Back", fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinLockEnabled = viewModel.settingsManager.pinLockEnabled
            },
            title = { Text("pin_setup_title".localize(appLanguage)) },
            text = {
                Column {
                    Text("pin_setup_desc".localize(appLanguage))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                            }
                        },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError.isNotEmpty()) {
                        Text(pinError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setPin(pinInput)
                            pinLockEnabled = true
                            showPinDialog = false
                            pinInput = ""
                            pinError = ""
                            Toast.makeText(context, "pin_lock_enabled".localize(appLanguage), Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = "enter_4_digit".localize(appLanguage)
                        }
                    }
                ) {
                    Text("save".localize(appLanguage))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinLockEnabled = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
