package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin", fontWeight = FontWeight.Bold) },
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        IconButton(
                            onClick = { showEmptyTrashDialog = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty Trash", tint = Color.Red)
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
            if (trashedNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trash is empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes you delete will stay here. Restore or permanently erase them anytime.",
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
                    items(trashedNotes, key = { it.id }) { note ->
                        TrashGridItem(
                            note = note,
                            onRestore = { viewModel.restoreNote(note) },
                            onDeletePermanently = { viewModel.deleteNotePermanently(note) },
                            fontSizeStr = viewModel.settingsManager.fontSize,
                            fontFamilyStr = viewModel.settingsManager.fontFamily
                        )
                    }
                }
            }
        }
    }

    // Confirm Empty Trash Dialog
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash Bin?") },
            text = { Text("Are you absolutely sure you want to permanently delete all notes in the trash? This action is offline, final, and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete All Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashGridItem(
    note: Note,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    fontSizeStr: String,
    fontFamilyStr: String
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardBg = getNoteColor(note.colorName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showMenu = true },
                onLongClick = { showMenu = true }
            )
            .testTag("trash_note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Trashed",
                    tint = Color.Red.copy(alpha = 0.6f),
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Restore Note") },
                    onClick = { onRestore(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Restore, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete Permanently", color = Color.Red) },
                    onClick = { onDeletePermanently(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Color.Red) }
                )
            }
        }
    }
}
