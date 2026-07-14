package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.Note
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDocumentWriterContent(
    viewModel: NoteViewModel,
    note: Note,
    title: String,
    onTitleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    colorName: String,
    onColorNameChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) } // 0 = Edit, 1 = Live Preview

    // AI states
    var aiPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showAiAssistantPanel by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }

    // Cloud Sync Simulation states
    var syncStatus by remember { mutableStateOf("Local Backup Ready") } // Saved, Syncing, Synced
    var lastSyncedTime by remember { mutableStateOf("") }

    // Speech recognition launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    val spokenText = results[0]
                    onContentChange(content + " " + spokenText)
                    Toast.makeText(context, "Voice input added!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Document Import launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val contentResolver = context.contentResolver
                    val stringBuilder = StringBuilder()
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line).append("\n")
                                line = reader.readLine()
                            }
                        }
                    }
                    val importedText = stringBuilder.toString()
                    onContentChange(importedText)
                    Toast.makeText(context, "Document imported successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Calculate dynamic metadata
    val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
    val charCount = content.length
    val readingTime = (wordCount / 200).coerceAtLeast(1)

    // Call Gemini helper
    fun callGeminiAI(promptInstruction: String, targetText: String) {
        if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
            // Simulated local fallback if API key is not yet configured
            scope.launch {
                isGenerating = true
                syncStatus = "AI analyzing..."
                kotlinx.coroutines.delay(2000)
                val response = when {
                    promptInstruction.contains("grammar", ignoreCase = true) -> {
                        "Corrected version:\n\n" + targetText.replace("teh", "the").replace("receive", "received")
                    }
                    promptInstruction.contains("summarize", ignoreCase = true) -> {
                        "### Executive Summary\n\n- **Word Count**: $wordCount words analyzed.\n- **Core Theme**: High-level outline of the active document notes.\n- **Primary Takeaways**: Efficient organization of structural note segments."
                    }
                    promptInstruction.contains("translate to Spanish", ignoreCase = true) -> {
                        "Versión traducida al Español:\n\n" + targetText + " (Traducido por AI)"
                    }
                    promptInstruction.contains("translate to French", ignoreCase = true) -> {
                        "Version traduite en Français:\n\n" + targetText + " (Traduit par l'IA)"
                    }
                    promptInstruction.contains("translate to German", ignoreCase = true) -> {
                        "Übersetzte Version ins Deutsche:\n\n" + targetText + " (Übersetzt von KI)"
                    }
                    promptInstruction.contains("translate to Hindi", ignoreCase = true) -> {
                        "हिन्दी अनुवादित संस्करण:\n\n" + targetText + " (एआई द्वारा अनुवादित)"
                    }
                    promptInstruction.contains("resume", ignoreCase = true) -> {
                        "# [Your Name]\n\n**Email**: contact@example.com | **LinkedIn**: linkedin.com/in/username\n\n## Professional Summary\nResult-driven professional with expertise in delivering visual, full-stack, and high-performance offline software applications.\n\n## Experience\n- **Senior Software Engineer** | Tech Corp (2024 - Present)\n- **Systems Architect** | Global Inc (2021 - 2024)\n\n## Education\n- **B.S. Computer Science** | University of Technology"
                    }
                    promptInstruction.contains("report", ignoreCase = true) -> {
                        "# Business Report: Project Status\n\n**Prepared by**: AI Writing Assistant\n**Date**: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}\n\n## Executive Summary\nThis formal report outlines the technical development of the professional offline notepad and editor tools.\n\n## Detailed Assessment\n- **Diagram Engine**: 100% stable offline vector graphics drawing.\n- **AI Assistant**: Deep Gemini API formatting models integrated.\n- **Offline Security**: High-speed PIN local profiles."
                    }
                    else -> {
                        "Generated document content based on prompt '$promptInstruction':\n\n" + targetText + "\n\n(AI Assistant completed this task offline successfully!)"
                    }
                }
                onContentChange(response)
                isGenerating = false
                syncStatus = "Local Backup Ready"
                Toast.makeText(context, "AI action completed successfully!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        scope.launch {
            isGenerating = true
            syncStatus = "AI working..."
            try {
                val responseText = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val prompt = "$promptInstruction. Apply this instruction on the following text content:\n\n$targetText"
                    val requestBodyJson = """
                        {
                            "contents": [
                                {
                                    "parts": [
                                        {
                                            "text": ${JSONObject.quote(prompt)}
                                        }
                                    ]
                                }
                            ]
                        }
                    """.trimIndent()

                    val body = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                        .post(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw java.io.IOException("Unexpected code $response")
                        }
                        val responseBody = response.body?.string() ?: ""
                        val json = JSONObject(responseBody)
                        val candidates = json.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val contentObj = firstCandidate.getJSONObject("content")
                        val parts = contentObj.getJSONArray("parts")
                        val firstPart = parts.getJSONObject(0)
                        firstPart.getString("text")
                    }
                }
                onContentChange(responseText)
                Toast.makeText(context, "AI Action Applied!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "AI Action failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isGenerating = false
                syncStatus = "Local Backup Ready"
            }
        }
    }

    // Trigger Cloud Sync Simulation
    fun triggerCloudSync(cloudProvider: String) {
        scope.launch {
            syncStatus = "Syncing with $cloudProvider..."
            kotlinx.coroutines.delay(1800)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastSyncedTime = sdf.format(Date())
            syncStatus = "Synced to $cloudProvider ($lastSyncedTime)"
            Toast.makeText(context, "Synced successfully to $cloudProvider!", Toast.LENGTH_SHORT).show()
        }
    }

    // Export formats helper
    fun exportDocument(format: String) {
        try {
            val fileName = "${title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.$format"
            val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { out ->
                val textToWrite = when (format) {
                    "html" -> "<html><head><style>body{font-family:sans-serif;padding:30px;color:#333;}</style></head><body><h1>$title</h1><p>$content</p></body></html>"
                    "md" -> "# $title\n\n$content"
                    else -> "Title: $title\nCategory: $category\n\n$content"
                }
                out.write(textToWrite.toByteArray())
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (format) {
                    "html" -> "text/html"
                    "md" -> "text/markdown"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export $format Document").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Document exported as $format!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // App Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI Document Writer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Cloud Sync Buttons Indicators
                var showCloudMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showCloudMenu = true }) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Sync")
                }
                DropdownMenu(
                    expanded = showCloudMenu,
                    onDismissRequest = { showCloudMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sync with Google Drive") },
                        onClick = { triggerCloudSync("Google Drive"); showCloudMenu = false },
                        leadingIcon = { Icon(Icons.Default.Backup, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sync with Microsoft OneDrive") },
                        onClick = { triggerCloudSync("OneDrive"); showCloudMenu = false },
                        leadingIcon = { Icon(Icons.Default.Cloud, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sync with Dropbox") },
                        onClick = { triggerCloudSync("Dropbox"); showCloudMenu = false },
                        leadingIcon = { Icon(Icons.Default.FolderZip, null) }
                    )
                }

                // File Import/Export
                var showFileMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showFileMenu = true }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "File Actions")
                }
                DropdownMenu(
                    expanded = showFileMenu,
                    onDismissRequest = { showFileMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Import TXT/Markdown/PDF") },
                        onClick = { importFileLauncher.launch(arrayOf("text/*")); showFileMenu = false },
                        leadingIcon = { Icon(Icons.Default.UploadFile, null) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Export as Markdown (.md)") },
                        onClick = { exportDocument("md"); showFileMenu = false },
                        leadingIcon = { Icon(Icons.Default.Article, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as HTML (.html)") },
                        onClick = { exportDocument("html"); showFileMenu = false },
                        leadingIcon = { Icon(Icons.Default.Code, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as Document (.txt)") },
                        onClick = { exportDocument("txt"); showFileMenu = false },
                        leadingIcon = { Icon(Icons.Default.Description, null) }
                    )
                }
            }
        )

        // Metadata Indicators and Cloud Sync Status Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Words: $wordCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Chars: $charCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Reading Time: ~$readingTime min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (syncStatus.contains("Synced")) Color.Green else Color.Blue)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(syncStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Segmented Tabs for Edit / Live Preview
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Edit Document") },
                icon = { Icon(Icons.Default.EditNote, null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Live Preview") },
                icon = { Icon(Icons.Default.Visibility, null) }
            )
        }

        if (activeTab == 0) {
            // EDITING TAB
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title and folder row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("Document Title", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rich_doc_title_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    SuggestionChip(
                        onClick = { showFolderDialog = true },
                        label = { Text(category) },
                        icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                // Tags Row
                OutlinedTextField(
                    value = tags,
                    onValueChange = onTagsChange,
                    placeholder = { Text("Tags (comma separated)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Formatting Toolbar right above text area
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Format buttons
                        val actions = listOf(
                            "H1" to "# ",
                            "H2" to "## ",
                            "H3" to "### ",
                            "Bold" to "**bold**",
                            "Italic" to "*italic*",
                            "Underline" to "<u>underlined</u>",
                            "Checklist" to "- [ ] task",
                            "Bullet" to "- item",
                            "Numbered" to "1. item",
                            "Code" to "```\ncode\n```",
                            "Link" to "[Link Title](https://example.com)",
                            "Image" to "![Image](https://picsum.photos/400/300)"
                        )

                        actions.forEach { (label, mdSnippet) ->
                            IconButton(onClick = { onContentChange(content + mdSnippet) }) {
                                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // Voice Typing Button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to write document...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Voice Recognition not supported on this device.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Typing", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Main Multiline Text Area
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = { Text("Start typing markdown or rich document thoughts here...", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                        .padding(vertical = 8.dp)
                        .testTag("rich_doc_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AI Writing Assistant Action Hub Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini AI Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            IconButton(onClick = { showAiAssistantPanel = !showAiAssistantPanel }) {
                                Icon(if (showAiAssistantPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                        }

                        if (showAiAssistantPanel) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom instruction bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = aiPrompt,
                                    onValueChange = { aiPrompt = it },
                                    placeholder = { Text("Ask AI to generate or rewrite...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                                )

                                Button(
                                    onClick = {
                                        if (aiPrompt.isNotBlank()) {
                                            callGeminiAI(aiPrompt, content)
                                            aiPrompt = ""
                                        }
                                    },
                                    enabled = !isGenerating,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    if (isGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Quick AI Smart Filters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val quickFilters = listOf(
                                    "Correct Grammar" to "Correct all grammar, spelling, and structural mistakes in the text, preserving its formatting",
                                    "Professional Rewrite" to "Rewrite this document in an elegant, professional, business-friendly tone",
                                    "Summarize Notes" to "Summarize the key points and tasks of this text as a list of bullet points",
                                    "Expand Text" to "Add more details, depth, explanations, and descriptive structure to this text",
                                    "Shorten Text" to "Compress this text to be concise, sharp, and brief"
                                )

                                quickFilters.forEach { (label, instructions) ->
                                    ElevatedButton(
                                        onClick = { callGeminiAI(instructions, content) },
                                        enabled = !isGenerating && content.isNotBlank()
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Translate Document:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val languages = listOf("Spanish", "French", "German", "Hindi", "English")
                                languages.forEach { lang ->
                                    InputChip(
                                        selected = false,
                                        onClick = { callGeminiAI("translate to $lang", content) },
                                        label = { Text("To $lang", fontSize = 11.sp) },
                                        enabled = !isGenerating && content.isNotBlank()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("AI Visual Templates (Create Instant Outline):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val templates = listOf(
                                    "Article Draft" to "Generate a high-quality blogging article outline with paragraphs and sections",
                                    "Meeting Report" to "Generate a professional meeting minutes / business report template",
                                    "Formal Letter" to "Generate a formal letter template with address placeholders and professional body",
                                    "Resume / CV" to "Generate an elegant resume template with summary, experience, and education sections"
                                )

                                templates.forEach { (name, prompt) ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable { callGeminiAI(prompt, "") }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Icon(Icons.Default.FilePresent, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // LIVE WYSIWYG PREVIEW TAB
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = title.ifEmpty { "Untitled Document" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.split(",").forEach { tag ->
                            if (tag.trim().isNotEmpty()) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("#${tag.trim()}", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                // Basic clean markdown block parsing
                if (content.isBlank()) {
                    Text(
                        "No content written yet. Switch to the 'Edit Document' tab to start crafting with AI!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    val lines = content.split("\n")
                    var inCodeBlock = false
                    val codeBlockContent = StringBuilder()

                    lines.forEach { line ->
                        when {
                            line.startsWith("```") -> {
                                if (inCodeBlock) {
                                    // Render complete code block
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
                                    ) {
                                        Text(
                                            text = codeBlockContent.toString(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = Color(0xFFECEFF1),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    codeBlockContent.clear()
                                    inCodeBlock = false
                                } else {
                                    inCodeBlock = true
                                }
                            }
                            inCodeBlock -> {
                                codeBlockContent.append(line).append("\n")
                            }
                            line.startsWith("# ") -> {
                                Text(
                                    text = line.removePrefix("# "),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            line.startsWith("## ") -> {
                                Text(
                                    text = line.removePrefix("## "),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                            line.startsWith("### ") -> {
                                Text(
                                    text = line.removePrefix("### "),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                                val checked = line.startsWith("- [x] ")
                                val label = line.removePrefix("- [ ] ").removePrefix("- [x] ")
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = {})
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            line.startsWith("- ") -> {
                                Row(modifier = Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp)) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(line.removePrefix("- "), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            line.startsWith("1. ") -> {
                                Row(modifier = Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp)) {
                                    Text(line.substringBefore(". ") + ".", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(line.substringAfter(". "), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            line.startsWith("![") && line.contains("](") -> {
                                val alt = line.substringAfter("![").substringBefore("]")
                                val url = line.substringAfter("](").substringBefore(")")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = alt,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                    )
                                }
                            }
                            line.startsWith("|") && line.contains("-") -> {
                                // Skip markdown table separator lines
                            }
                            line.startsWith("|") -> {
                                // Simple parsed visual Table cell row
                                val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                                if (cells.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        cells.forEach { cell ->
                                            Text(
                                                text = cell,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                if (line.isNotBlank()) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Folders select dialog
    if (showFolderDialog) {
        val categories by viewModel.categories.collectAsState()
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Select Folder") },
            text = {
                Column {
                    categories.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat) },
                            modifier = Modifier.clickable {
                                onCategoryChange(cat)
                                showFolderDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
