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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.viewmodel.UserProfile
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
import com.example.ui.localization.localize
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
    val appLanguage by viewModel.appLanguage.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()

    val searchText by viewModel.searchText.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val profiles by viewModel.userProfiles.collectAsState()

    var profileToAuthorize by remember { mutableStateOf<UserProfile?>(null) }
    var pinInputText by remember { mutableStateOf("") }
    var pinInputError by remember { mutableStateOf(false) }

    var profileToCustomize by remember { mutableStateOf<UserProfile?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var editEmojiText by remember { mutableStateOf("") }
    var editColorHex by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                LargeTopAppBar(
                    title = {
                        Text(
                            "app_name".localize(appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontFamily = getFontFamily(viewModel.settingsManager.fontFamily)
                        )
                    },
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        ) {
                            profiles.forEach { profile ->
                                val isActive = activeProfileId == profile.id
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(profile.colorHex)))
                                        .border(
                                            width = if (isActive) 2.5.dp else 1.dp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (profile.id == "secure" && viewModel.settingsManager.pinLockEnabled) {
                                                    profileToAuthorize = profile
                                                    pinInputText = ""
                                                    pinInputError = false
                                                } else {
                                                    viewModel.selectProfile(profile.id)
                                                }
                                            },
                                            onLongClick = {
                                                profileToCustomize = profile
                                                editNameText = profile.name
                                                editEmojiText = profile.emoji
                                                editColorHex = profile.colorHex
                                            }
                                        )
                                        .testTag("profile_icon_${profile.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.emoji,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
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
                                text = { Text("sort_modified_desc".localize(appLanguage)) },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.MODIFIED_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("sort_modified_asc".localize(appLanguage)) },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.MODIFIED_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("sort_newest".localize(appLanguage)) },
                                onClick = {
                                    viewModel.sortOrder.value = SortOrder.NEWEST
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("sort_title_asc".localize(appLanguage)) },
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
                    placeholder = { Text("search_placeholder".localize(appLanguage)) },
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
                            label = { Text("all_notes".localize(appLanguage)) }
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
                EmptyNotesPlaceholder(appLanguage)
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
                                fontFamilyStr = viewModel.settingsManager.fontFamily,
                                appLanguage = appLanguage
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
                                fontFamilyStr = viewModel.settingsManager.fontFamily,
                                appLanguage = appLanguage
                            )
                        }
                    }
                }
            }
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
                            viewModel.selectProfile(profileToAuthorize!!.id)
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

    if (profileToCustomize != null) {
        AlertDialog(
            onDismissRequest = { profileToCustomize = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "customize_profile_title".localize(appLanguage))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text("profile_name_label".localize(appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name")
                    )

                    Text(
                        text = "profile_emoji_label".localize(appLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val emojiPresets = listOf("😊", "💼", "🔒", "🚀", "🎨", "🎮", "🌟", "📚", "🏖️", "🐱")
                        items(emojiPresets) { emoji ->
                            val isSelected = editEmojiText == emoji
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editEmojiText = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }

                    Text(
                        text = "profile_color_label".localize(appLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val colorPresets = listOf(
                            "#42A5F5" to "Blue",
                            "#26A69A" to "Teal",
                            "#AB47BC" to "Purple",
                            "#FF7043" to "Coral",
                            "#9CCC65" to "Green",
                            "#FFCA28" to "Amber",
                            "#EC407A" to "Pink"
                        )
                        items(colorPresets) { (hex, name) ->
                            val isSelected = editColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.outline else Color.White.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { editColorHex = hex }
                            )
                        }
                    }
                    
                    Text(
                        text = "long_press_customize".localize(appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.isNotBlank() && editEmojiText.isNotBlank()) {
                            viewModel.updateProfile(
                                id = profileToCustomize!!.id,
                                name = editNameText,
                                emoji = editEmojiText,
                                colorHex = editColorHex
                            )
                            profileToCustomize = null
                        }
                    },
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("save".localize(appLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToCustomize = null }) {
                    Text("cancel".localize(appLanguage))
                }
            }
        )
    }
}

@Composable
fun EmptyNotesPlaceholder(appLanguage: String) {
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
            text = "no_notes_title".localize(appLanguage),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "no_notes_desc".localize(appLanguage),
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
    fontFamilyStr: String,
    appLanguage: String
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
                    text = note.title.ifEmpty { "untitled".localize(appLanguage) },
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
                    text = { Text(if (note.isPinned) "unpin".localize(appLanguage) else "pin".localize(appLanguage)) },
                    onClick = { onTogglePin(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "remove_favorite".localize(appLanguage) else "add_favorite".localize(appLanguage)) },
                    onClick = { onToggleFavorite(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
                DropdownMenuItem(
                    text = { Text("duplicate".localize(appLanguage)) },
                    onClick = { onDuplicate(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                )
                DropdownMenuItem(
                    text = { Text("archive".localize(appLanguage)) },
                    onClick = { onArchive(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("delete".localize(appLanguage), color = Color.Red) },
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
    fontFamilyStr: String,
    appLanguage: String
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
                        text = note.title.ifEmpty { "untitled".localize(appLanguage) },
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
                    text = { Text(if (note.isPinned) "unpin".localize(appLanguage) else "pin".localize(appLanguage)) },
                    onClick = { onTogglePin(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.PushPin, null) }
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "remove_favorite".localize(appLanguage) else "add_favorite".localize(appLanguage)) },
                    onClick = { onToggleFavorite(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
                DropdownMenuItem(
                    text = { Text("duplicate".localize(appLanguage)) },
                    onClick = { onDuplicate(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                )
                DropdownMenuItem(
                    text = { Text("archive".localize(appLanguage)) },
                    onClick = { onArchive(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Archive, null) }
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("delete".localize(appLanguage), color = Color.Red) },
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
