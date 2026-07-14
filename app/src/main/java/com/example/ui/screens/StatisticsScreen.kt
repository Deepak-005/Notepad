package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val activeNotes by viewModel.allActiveNotes.collectAsState()
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()

    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val profiles by viewModel.userProfiles.collectAsState()
    val activeProfile = profiles.find { it.id == activeProfileId }

    // Computations
    val totalActive = activeNotes.size
    val totalPinned = activeNotes.count { it.isPinned }
    val totalArchived = archivedNotes.size
    val totalTrashed = trashedNotes.size

    val totalWords = activeNotes.sumOf { it.wordCount }
    val totalChars = activeNotes.sumOf { it.charCount }
    val avgWords = if (totalActive > 0) totalWords / totalActive else 0

    // Category Distribution
    val categoryCounts = remember(activeNotes) {
        activeNotes.groupBy { it.category }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    // Tag Counts
    val tagCounts = remember(activeNotes) {
        activeNotes.flatMap { note ->
            note.tags.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        }
        .groupBy { it }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(10)
    }

    // Color Theme Distribution
    val colorCounts = remember(activeNotes) {
        activeNotes.groupBy { it.colorName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "statistics".localize(appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontFamily = getFontFamily(viewModel.settingsManager.fontFamily)
                    )
                },
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
                    } else {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Dashboard (Grid of counts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "stats_total".localize(appLanguage),
                    value = totalActive.toString(),
                    icon = Icons.Outlined.Description,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "stats_pinned".localize(appLanguage),
                    value = totalPinned.toString(),
                    icon = Icons.Outlined.PushPin,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "stats_archived".localize(appLanguage),
                    value = totalArchived.toString(),
                    icon = Icons.Outlined.Archive,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "stats_trashed".localize(appLanguage),
                    value = totalTrashed.toString(),
                    icon = Icons.Outlined.Delete,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Word & Text Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "general".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricRow(
                            label = "stats_words".localize(appLanguage),
                            value = totalWords.toString()
                        )
                        MetricRow(
                            label = "stats_chars".localize(appLanguage),
                            value = totalChars.toString()
                        )
                        MetricRow(
                            label = "stats_avg_words".localize(appLanguage),
                            value = avgWords.toString()
                        )
                    }
                }
            }

            // Category Distribution Chart
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "stats_categories".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryCounts.isEmpty()) {
                        Text(
                            text = "no_content_written".localize(appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val maxCount = categoryCounts.maxOf { it.second }.toFloat()
                        categoryCounts.forEach { (cat, count) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                               ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$count (${String.format("%.1f", (count.toFloat() / totalActive) * 100)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tag Cloud Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "stats_tags".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (tagCounts.isEmpty()) {
                        Text(
                            text = "all_tags".localize(appLanguage) + " (" + "0" + ")",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Tag flow layout
                        OptInFlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            spacing = 8.dp
                        ) {
                            tagCounts.forEach { (tag, count) ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("#$tag ($count)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.LocalActivity,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Color distribution
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "stats_colors".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (colorCounts.isEmpty()) {
                        Text(
                            text = "no_content_written".localize(appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorCounts.forEach { (colorName, count) ->
                                val dotColor = getNoteColor(colorName)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor.copy(alpha = 0.8f)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

// Custom flow row layout so we don't rely on experimental layout APIs that might fail compilation
@Composable
fun OptInFlowRow(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val rowList = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowHeights = mutableListOf<Int>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        for (measurable in measurables) {
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            if (currentRowWidth + placeable.width + spacingPx > constraints.maxWidth && currentRow.isNotEmpty()) {
                rowList.add(currentRow)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + spacingPx
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rowList.add(currentRow)
            rowHeights.add(currentRowHeight)
        }

        val totalHeight = rowHeights.sum() + maxOf(0, rowList.size - 1) * spacingPx
        val layoutWidth = constraints.maxWidth

        layout(layoutWidth, totalHeight) {
            var currentY = 0
            rowList.forEachIndexed { rowIndex, row ->
                var currentX = 0
                row.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY)
                    currentX += placeable.width + spacingPx
                }
                currentY += rowHeights[rowIndex] + spacingPx
            }
        }
    }
}
