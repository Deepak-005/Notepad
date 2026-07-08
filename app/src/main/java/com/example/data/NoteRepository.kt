package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<Note>> = noteDao.getTrashedNotes()
    val categories: Flow<List<String>> = noteDao.getCategories()

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    fun getNoteByIdFlow(id: Int): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun getAllNotesDirect(): List<Note> = noteDao.getAllNotesDirect()
}
