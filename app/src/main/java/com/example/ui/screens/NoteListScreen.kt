package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.SortOrder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()

    val searchText by viewModel.searchText.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Offline Notepad",
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(viewModel.settingsManager.fontFamily)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.testTag("toggle_layout_button")
                        ) {
                            Icon(
                                if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Toggle Grid/List"
                            )
                        }
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Notes")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Last Modified (Newest)") },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.MODIFIED_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Last Modified (Oldest)") },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.MODIFIED_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Created (Newest)") },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.NEWEST
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Title A-Z") },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.TITLE_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, null) }
                            )
                        }
                    }
                )

                // Search Bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.searchText.value = it },
                    placeholder = { Text("Search your notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchText.value = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_notes_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Horizontal Filters (Categories & Tags)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "All",
                            onClick = { viewModel.selectedCategory.value = "All" },
                            label = { Text("All Notes") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectedCategory.value = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(-1) },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("add_note_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (notes.isEmpty()) {
                EmptyNotesPlaceholder()
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteGridItem(
                                note = note,
                                onClick = { onNavigateToEditor(note.id) },
                                onTogglePin = { viewModel.togglePin(note) },
                                onToggleFavorite = { viewModel.toggleFavorite(note) },
                                onDuplicate = { viewModel.duplicateNote(note) },
                                onArchive = { viewModel.archiveNote(note) },
                                onTrash = { viewModel.trashNote(note) },
                                fontSizeStr = viewModel.settingsManager.fontSize,
                                fontFamilyStr = viewModel.settingsManager.fontFamily
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClick = { onNavigateToEditor(note.id) },
                                onTogglePin = { viewModel.togglePin(note) },
                                onToggleFavorite = { viewModel.toggleFavorite(note) },
                                onDuplicate = { viewModel.duplicateNote(note) },
                                onArchive = { viewModel.archiveNote(note) },
                                onTrash = { viewModel.trashNote(note) },
                                fontSizeStr = viewModel.settingsManager.fontSize,
                                fontFamilyStr = viewModel.settingsManager.fontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the '+' button below to create your very first secure offline note.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    fontSizeStr: String,
    fontFamilyStr: String
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardBg = getNoteColor(note.colorName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = getTitleStyle(fontSizeStr),
                    fontWeight = FontWeight.Bold,
                    fontFamily = getFontFamily(fontFamilyStr),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (note.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                style = getBodyStyle(fontSizeStr),
                fontFamily = getFontFamily(fontFamilyStr),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(note.category, fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp)
                )
                Text(
                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(note.modifiedAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                    onClick = { onTogglePin(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "Remove Favorite" else "Add Favorite") },
                    onClick = { onToggleFavorite(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = { onDuplicate(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                )
                DropdownMenuItem(
                    text = { Text("Archive") },
                    onClick = { onArchive(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = { onTrash(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    fontSizeStr: String,
    fontFamilyStr: String
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardBg = getNoteColor(note.colorName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
                        style = getTitleStyle(fontSizeStr),
                        fontWeight = FontWeight.Bold,
                        fontFamily = getFontFamily(fontFamilyStr),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    if (note.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Favorite, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = getBodyStyle(fontSizeStr),
                    fontFamily = getFontFamily(fontFamilyStr),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(note.category, fontSize = 9.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(note.modifiedAt)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        }

        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                    onClick = { onTogglePin(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "Remove Favorite" else "Add Favorite") },
                    onClick = { onToggleFavorite(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = { onDuplicate(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                )
                DropdownMenuItem(
                    text = { Text("Archive") },
                    onClick = { onArchive(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = { onTrash(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
            }
        }
    }
}

// Color matching helper
@Composable
fun getNoteColor(colorName: String): Color {
    val isDark = isSystemInDarkTheme()
    return when (colorName) {
        "Red" -> if (isDark) Color(0xFF6B1B1B) else Color(0xFFFFD1D1)
        "Orange" -> if (isDark) Color(0xFF6B3A0F) else Color(0xFFFFE4C4)
        "Yellow" -> if (isDark) Color(0xFF5A5215) else Color(0xFFFFF7C2)
        "Green" -> if (isDark) Color(0xFF1B4E28) else Color(0xFFD1F2D9)
        "Teal" -> if (isDark) Color(0xFF144D49) else Color(0xFFD1F2F0)
        "Blue" -> if (isDark) Color(0xFF183D5E) else Color(0xFFD4E6FC)
        "Purple" -> if (isDark) Color(0xFF431F52) else Color(0xFFE9D1F5)
        "Pink" -> if (isDark) Color(0xFF581D39) else Color(0xFFFCD5E2)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

fun getFontFamily(fontFamilyStr: String): FontFamily {
    return when (fontFamilyStr) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
}

fun getTitleStyle(fontSizeStr: String): androidx.compose.ui.text.TextStyle {
    return when (fontSizeStr) {
        "Small" -> androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
        "Large" -> androidx.compose.ui.text.TextStyle(fontSize = 20.sp, lineHeight = 26.sp)
        else -> androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 22.sp)
    }
}

fun getBodyStyle(fontSizeStr: String): androidx.compose.ui.text.TextStyle {
    return when (fontSizeStr) {
        "Small" -> androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
        "Large" -> androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
        else -> androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    }
}
