package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val profiles by viewModel.userProfiles.collectAsState()
    val activeProfile = profiles.find { it.id == activeProfileId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("archive".localize(appLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (activeProfile != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(activeProfile.colorHex))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activeProfile.emoji, fontSize = 18.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (archivedNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "no_archived_notes".localize(appLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "no_archived_notes_desc".localize(appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
                        ArchiveGridItem(
                            note = note,
                            onClick = { onNavigateToEditor(note.id) },
                            onUnarchive = { viewModel.unarchiveNote(note) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveGridItem(
    note: Note,
    onClick: () -> Unit,
    onUnarchive: () -> Unit,
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
            .testTag("archive_note_card_${note.id}"),
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
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archived",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                style = getBodyStyle(fontSizeStr),
                fontFamily = getFontFamily(fontFamilyStr),
                maxLines = 4,
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
                    text = { Text("unarchive".localize(appLanguage)) },
                    onClick = { onUnarchive(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Unarchive, null) }
                )
                DropdownMenuItem(
                    text = { Text("delete".localize(appLanguage), color = Color.Red) },
                    onClick = { onTrash(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                )
            }
        }
    }
}
