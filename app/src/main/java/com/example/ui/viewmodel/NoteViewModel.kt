package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SortOrder {
    NEWEST, OLDEST, TITLE_ASC, TITLE_DESC, MODIFIED_DESC, MODIFIED_ASC
}

class NoteViewModel(
    private val app: Application,
    private val repository: NoteRepository,
    val settingsManager: SettingsManager
) : AndroidViewModel(app) {

    // Filter states
    val appTheme = MutableStateFlow(settingsManager.theme)
    val searchText = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedTag = MutableStateFlow("All")
    val sortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)

    // App Lock State
    private val _isAppLocked = MutableStateFlow(settingsManager.pinLockEnabled)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Active Notes Stream
    val notes: StateFlow<List<Note>> = combine(
        repository.activeNotes,
        searchText,
        selectedCategory,
        selectedTag,
        sortOrder
    ) { activeList, search, category, tag, sort ->
        var list = activeList

        // Apply Search
        if (search.isNotBlank()) {
            list = list.filter {
                it.title.contains(search, ignoreCase = true) ||
                        it.content.contains(search, ignoreCase = true) ||
                        it.tags.contains(search, ignoreCase = true)
            }
        }

        // Apply Category Filter
        if (category != "All") {
            list = list.filter { it.category == category }
        }

        // Apply Tag Filter
        if (tag != "All") {
            list = list.filter {
                it.tags.split(",").map { t -> t.trim().lowercase() }.contains(tag.lowercase())
            }
        }

        // Apply Sorting
        list = when (sort) {
            SortOrder.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOrder.OLDEST -> list.sortedBy { it.createdAt }
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOrder.MODIFIED_DESC -> list.sortedByDescending { it.modifiedAt }
            SortOrder.MODIFIED_ASC -> list.sortedBy { it.modifiedAt }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Archived Notes
    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trashed Notes
    val trashedNotes: StateFlow<List<Note>> = repository.trashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Categories
    val categories: StateFlow<List<String>> = repository.categories
        .map { list ->
            val defaultCats = listOf("General", "Work", "Personal", "Ideas", "Study")
            (defaultCats + list).distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("General", "Work", "Personal", "Ideas", "Study"))

    // All Tags (computed dynamically from notes)
    val tags: StateFlow<List<String>> = repository.activeNotes
        .map { list ->
            list.flatMap { note ->
                note.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PIN lock operations
    fun unlockApp(pin: String): Boolean {
        return if (pin == settingsManager.pinCode) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (settingsManager.pinLockEnabled) {
            _isAppLocked.value = true
        }
    }

    fun setPin(pin: String) {
        settingsManager.pinCode = pin
        settingsManager.pinLockEnabled = pin.isNotEmpty()
        _isAppLocked.value = false
    }

    fun disablePin() {
        settingsManager.pinCode = ""
        settingsManager.pinLockEnabled = false
        _isAppLocked.value = false
    }

    // CRUD note actions
    fun getNote(id: Int, callback: (Note?) -> Unit) {
        viewModelScope.launch {
            callback(repository.getNoteById(id))
        }
    }

    fun insertNote(note: Note, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertNote(note)
            onComplete(id.toInt())
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun togglePin(note: Note) {
        updateNote(note.copy(isPinned = !note.isPinned, modifiedAt = System.currentTimeMillis()))
    }

    fun toggleFavorite(note: Note) {
        updateNote(note.copy(isFavorite = !note.isFavorite, modifiedAt = System.currentTimeMillis()))
    }

    fun archiveNote(note: Note) {
        updateNote(note.copy(isArchived = true, isPinned = false, modifiedAt = System.currentTimeMillis()))
    }

    fun unarchiveNote(note: Note) {
        updateNote(note.copy(isArchived = false, modifiedAt = System.currentTimeMillis()))
    }

    fun trashNote(note: Note) {
        updateNote(note.copy(isTrashed = true, isPinned = false, modifiedAt = System.currentTimeMillis()))
    }

    fun restoreNote(note: Note) {
        updateNote(note.copy(isTrashed = false, modifiedAt = System.currentTimeMillis()))
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun duplicateNote(note: Note) {
        val duplicated = note.copy(
            id = 0,
            title = "${note.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isPinned = false
        )
        insertNote(duplicated)
    }

    // TXT Export
    fun exportAsTxt(note: Note) {
        try {
            val fileName = "${note.title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.txt"
            val file = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { out ->
                val text = "Title: ${note.title}\n" +
                        "Category: ${note.category}\n" +
                        "Created: ${formatDate(note.createdAt)}\n" +
                        "Modified: ${formatDate(note.modifiedAt)}\n" +
                        "Tags: ${note.tags}\n\n" +
                        note.content
                out.write(text.toByteArray())
            }

            // Share the TXT file
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export Note TXT").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
            Toast.makeText(app, "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(app, "TXT Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // PDF Export (Native)
    fun exportAsPdf(note: Note) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = TextPaint()

            // Header
            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText(note.title, 40f, 60f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.GRAY
            canvas.drawText("Category: ${note.category} | Created: ${formatDate(note.createdAt)}", 40f, 85f, paint)
            canvas.drawText("Tags: ${note.tags}", 40f, 100f, paint)

            // Line separator
            paint.color = android.graphics.Color.LTGRAY
            canvas.drawLine(40f, 115f, 555f, 115f, paint)

            // Content body
            textPaint.color = android.graphics.Color.BLACK
            textPaint.textSize = 12f

            val staticLayout = StaticLayout.Builder.obtain(note.content, 0, note.content.length, textPaint, 515)
                .build()

            canvas.save()
            canvas.translate(40f, 135f)
            staticLayout.draw(canvas)
            canvas.restore()

            pdfDocument.finishPage(page)

            val fileName = "${note.title.replace("[^a-zA-Z0-9]".toRegex(), "_")}.pdf"
            val file = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Share the PDF file
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export Note PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(chooser)
            Toast.makeText(app, "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(app, "PDF Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON Backup to File
    fun backupToBackupFile() {
        viewModelScope.launch {
            try {
                val allNotes = repository.getAllNotesDirect()
                val jsonArray = JSONArray()
                for (note in allNotes) {
                    val jsonObj = JSONObject().apply {
                        put("title", note.title)
                        put("content", note.content)
                        put("createdAt", note.createdAt)
                        put("modifiedAt", note.modifiedAt)
                        put("isPinned", note.isPinned)
                        put("isFavorite", note.isFavorite)
                        put("isArchived", note.isArchived)
                        put("isTrashed", note.isTrashed)
                        put("colorName", note.colorName)
                        put("category", note.category)
                        put("tags", note.tags)
                    }
                    jsonArray.put(jsonObj)
                }

                val backupString = jsonArray.toString(2)
                val fileName = "OfflineNotepad_Backup_${System.currentTimeMillis()}.json"
                val file = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                FileOutputStream(file).use { out ->
                    out.write(backupString.toByteArray())
                }

                val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Backup Notes JSON").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(chooser)
                Toast.makeText(app, "Backup saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(app, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Restore from Backup JSON
    fun restoreFromBackupJson(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(jsonString)
                var count = 0
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val note = Note(
                        title = jsonObj.optString("title", "Untitled"),
                        content = jsonObj.optString("content", ""),
                        createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = jsonObj.optLong("modifiedAt", System.currentTimeMillis()),
                        isPinned = jsonObj.optBoolean("isPinned", false),
                        isFavorite = jsonObj.optBoolean("isFavorite", false),
                        isArchived = jsonObj.optBoolean("isArchived", false),
                        isTrashed = jsonObj.optBoolean("isTrashed", false),
                        colorName = jsonObj.optString("colorName", "Default"),
                        category = jsonObj.optString("category", "General"),
                        tags = jsonObj.optString("tags", "")
                    )
                    repository.insertNote(note)
                    count++
                }
                Toast.makeText(app, "Successfully restored $count notes!", Toast.LENGTH_LONG).show()
                onComplete(true)
            } catch (e: Exception) {
                Toast.makeText(app, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class NoteViewModelFactory(
    private val app: Application,
    private val repository: NoteRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(app, repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
