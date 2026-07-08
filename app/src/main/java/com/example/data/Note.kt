package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val colorName: String = "Default", // Default, Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink
    val category: String = "General",
    val tags: String = "" // Comma-separated tags
) {
    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size

    val charCount: Int
        get() = content.length
}
