package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var tags by remember { mutableStateOf("") }
    var colorName by remember { mutableStateOf("Default") }

    var isPinned by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val focusManager = LocalFocusManager.current

    // Initialize Note
    LaunchedEffect(noteId) {
        if (noteId != -1) {
            viewModel.getNote(noteId) { loadedNote ->
                loadedNote?.let {
                    note = it
                    title = it.title
                    content = it.content
                    category = it.category
                    tags = it.tags
                    colorName = it.colorName
                    isPinned = it.isPinned
                    isFavorite = it.isFavorite
                }
            }
        } else {
            // New note init
            val newNote = Note(
                title = "",
                content = "",
                category = "General",
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            note = newNote
        }
    }

    // Auto-save logic
    LaunchedEffect(title, content, category, tags, colorName, isPinned, isFavorite) {
        val currentNote = note ?: return@LaunchedEffect
        // Only save if there are actual edits or if it's a new note and title/content is not empty
        if (title != currentNote.title || content != currentNote.content ||
            category != currentNote.category || tags != currentNote.tags ||
            colorName != currentNote.colorName || isPinned != currentNote.isPinned ||
            isFavorite != currentNote.isFavorite
        ) {
            isSaving = true
            delay(1500) // Debounce save
            val updated = currentNote.copy(
                title = title,
                content = content,
                category = category,
                tags = tags,
                colorName = colorName,
                isPinned = isPinned,
                isFavorite = isFavorite,
                modifiedAt = System.currentTimeMillis()
            )
            viewModel.insertNote(updated) { newId ->
                if (noteId == -1 && note?.id == 0) {
                    note = updated.copy(id = newId)
                } else {
                    note = updated
                }
            }
            isSaving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (noteId == -1) "New Note" else "Edit Note",
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(viewModel.settingsManager.fontFamily)
                        )
                        if (isSaving) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = "Saved Offline",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Icon(
                            if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview"
                        )
                    }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export as TXT") },
                            onClick = {
                                note?.let { viewModel.exportAsTxt(it.copy(title = title, content = content, category = category, tags = tags)) }
                                showMoreMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Description, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as PDF") },
                            onClick = {
                                note?.let { viewModel.exportAsPdf(it.copy(title = title, content = content, category = category, tags = tags)) }
                                showMoreMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = {
                                note?.let { viewModel.duplicateNote(it.copy(title = title, content = content, category = category, tags = tags)) }
                                showMoreMenu = false
                                onNavigateBack()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Delete Note", color = Color.Red) },
                            onClick = {
                                note?.let { viewModel.trashNote(it) }
                                showMoreMenu = false
                                onNavigateBack()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(getNoteColor(colorName))
        ) {
            // Main input fields area (scrollable if needed)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Category Picker Button and Color Dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { showCategoryDialog = true },
                        label = { Text("Folder: $category") },
                        icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) }
                    )

                    // Compact Color Picker dots
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Default", "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Purple", "Pink").forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(getNoteColor(col))
                                    .clickable { colorName = col }
                                    .padding(2.dp)
                            ) {
                                if (colorName == col) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                // Title Input
                if (!isPreviewMode) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(viewModel.settingsManager.fontFamily)
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input")
                    )
                } else {
                    Text(
                        text = title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = getFontFamily(viewModel.settingsManager.fontFamily),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // Tags Box
                if (!isPreviewMode) {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        placeholder = { Text("Tags (comma separated, e.g. life, work)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    if (tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
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
                }

                // Counts Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val words = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                    val chars = content.length
                    Text("Words: $words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Characters: $chars", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Content Editor Input or Live Preview
                if (!isPreviewMode) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Start typing notes...", style = MaterialTheme.typography.bodyLarge) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = getFontFamily(viewModel.settingsManager.fontFamily),
                            fontSize = when (viewModel.settingsManager.fontSize) {
                                "Small" -> 14.sp
                                "Large" -> 18.sp
                                else -> 16.sp
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("note_content_input")
                    )
                } else {
                    MarkdownView(content = content, fontFamilyStr = viewModel.settingsManager.fontFamily)
                }
            }

            // Rich Text formatting Helper bar (only in edit mode)
            if (!isPreviewMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { content += "**Bold**" }) { Icon(Icons.Default.FormatBold, "Bold") }
                    IconButton(onClick = { content += "*Italic*" }) { Icon(Icons.Default.FormatItalic, "Italic") }
                    IconButton(onClick = { content += "<u>Underline</u>" }) { Icon(Icons.Default.FormatUnderlined, "Underline") }
                    IconButton(onClick = { content += "\n# Heading\n" }) { Icon(Icons.Default.Title, "Heading") }
                    IconButton(onClick = { content += "\n- " }) { Icon(Icons.Default.FormatListBulleted, "Bullet List") }
                    IconButton(onClick = { content += "\n1. " }) { Icon(Icons.Default.FormatListNumbered, "Numbered List") }
                    IconButton(onClick = { content += "\n- [ ] " }) { Icon(Icons.Default.CheckBox, "Checklist") }
                    IconButton(onClick = { content += "\n```\nCode block\n```\n" }) { Icon(Icons.Default.Code, "Code Block") }
                    IconButton(onClick = { content += "\n> Quote\n" }) { Icon(Icons.Default.FormatQuote, "Quote") }
                    IconButton(onClick = { content += " [Link](https://google.com) " }) { Icon(Icons.Default.Link, "Link") }

                    // Quick Emojis
                    listOf("📝", "❤️", "💡", "📌", "✅", "⚠️", "🚀", "📅", "🔒").forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .clickable { content += emoji }
                                .padding(8.dp),
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Category Selector Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Choose Folder Category") },
            text = {
                Column {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    category = cat
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat)
                        }
                    }

                    // Create dynamic category input
                    var newCat by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = newCat,
                        onValueChange = { newCat = it },
                        label = { Text("Create New Category") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (newCat.isNotBlank()) {
                                    category = newCat.trim()
                                    showCategoryDialog = false
                                }
                            }) {
                                Icon(Icons.Default.Add, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MarkdownView(content: String, fontFamilyStr: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        if (content.isEmpty()) {
            Text("No content written yet.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        } else {
            // Super simple custom rendering of Markdown tags in Jetpack Compose
            val lines = content.split("\n")
            lines.forEach { line ->
                when {
                    line.startsWith("# ") -> {
                        Text(
                            text = line.substring(2),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(fontFamilyStr),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    line.startsWith("## ") -> {
                        Text(
                            text = line.substring(3),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(fontFamilyStr),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    line.startsWith("- [ ] ") -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = {})
                            Text(line.substring(6), style = MaterialTheme.typography.bodyLarge, fontFamily = getFontFamily(fontFamilyStr))
                        }
                    }
                    line.startsWith("- [x] ") -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = true, onCheckedChange = {})
                            Text(line.substring(6), style = MaterialTheme.typography.bodyLarge, fontFamily = getFontFamily(fontFamilyStr))
                        }
                    }
                    line.startsWith("- ") -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("•", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            Text(line.substring(2), style = MaterialTheme.typography.bodyLarge, fontFamily = getFontFamily(fontFamilyStr))
                        }
                    }
                    line.startsWith("> ") -> {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = line.substring(2),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = getFontFamily(fontFamilyStr)
                            )
                        }
                    }
                    else -> {
                        // Render standard inline formatting simply
                        var textToShow = line
                        // Clean bold / italics brackets for readability in render view
                        textToShow = textToShow.replace("**", "")
                        textToShow = textToShow.replace("*", "")
                        textToShow = textToShow.replace("<u>", "").replace("</u>", "")
                        Text(
                            text = textToShow,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = getFontFamily(fontFamilyStr),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
