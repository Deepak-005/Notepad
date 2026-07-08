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
import com.example.ui.viewmodel.NoteViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
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
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Theme Dropdown Selector
            var showThemeMenu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("App Theme") },
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
                headlineContent = { Text("Font Size") },
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
                headlineContent = { Text("Font Style") },
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

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Security & Privacy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // App Lock PIN Toggle
            ListItem(
                headlineContent = { Text("App PIN Lock") },
                supportingContent = { Text(if (pinLockEnabled) "App requires 4-digit PIN on launch" else "Unsecured / No lock") },
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
                                Toast.makeText(context, "PIN Lock disabled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("pin_lock_switch")
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                "Data Management (100% Offline)",
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
                        Text("Export Backup (JSON)", fontWeight = FontWeight.Bold)
                        Text("Create a local JSON backup of all your notes to secure offline storage.", fontSize = 12.sp)
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
                        Text("Import Backup (JSON)", fontWeight = FontWeight.Bold)
                        Text("Select a local JSON backup file to import and merge your notes.", fontSize = 12.sp)
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
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit number to secure your notes when opening the app.")
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
                            Toast.makeText(context, "PIN code configured!", Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = "PIN must be exactly 4 digits long."
                        }
                    }
                ) {
                    Text("Enable Lock")
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
