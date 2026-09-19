package com.expensetracker.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.local.dao.NoteDao
import com.expensetracker.app.data.local.dao.ReminderDao
import com.expensetracker.app.data.reminder.ReminderScheduler
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderDao: ReminderDao,
    private val noteDao: NoteDao,
    private val profileRepository: ProfileRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    private val activeProfileId = profileRepository.observeActiveProfileId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminders = activeProfileId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else reminderDao.observeForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes = activeProfileId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else noteDao.observeForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message

    fun saveReminder(
        id: Long?,
        title: String,
        note: String,
        triggerAt: Long,
        endDate: Long?,
        recurrence: String,
        intervalDays: Int,
    ) {
        val profileId = activeProfileId.value ?: return
        if (title.isBlank()) return
        if (endDate != null && endDate < triggerAt.startOfDay()) {
            _message.value = "End date cannot be before the start date"
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val reminder = ReminderEntity(
                id = id ?: 0L,
                profileId = profileId,
                title = title.trim(),
                note = note.trim(),
                triggerAtEpochMillis = triggerAt,
                endDateEpochMillis = endDate?.endOfDay(),
                recurrence = recurrence,
                customIntervalDays = intervalDays.coerceAtLeast(1),
                enabled = true,
                createdAtEpochMillis = now,
            )
            val saved = if (id == null) {
                val newId = reminderDao.insert(reminder)
                reminder.copy(id = newId)
            } else {
                reminderDao.update(reminder)
                reminder
            }
            scheduler.schedule(saved)
            _message.value = "Reminder saved"
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(enabled = !reminder.enabled)
            reminderDao.update(updated)
            if (updated.enabled) scheduler.schedule(updated) else scheduler.cancel(updated.id)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            scheduler.cancel(reminder.id)
            reminderDao.delete(reminder)
        }
    }

    fun saveNote(id: Long?, title: String, content: String) {
        val profileId = activeProfileId.value ?: return
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val note = NoteEntity(id ?: 0L, profileId, title.trim().ifBlank { "Untitled note" }, content.trim(), System.currentTimeMillis())
            if (id == null) noteDao.insert(note) else noteDao.update(note)
            _message.value = "Note saved"
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { noteDao.delete(note) }
    }

    fun clearMessage() { _message.value = null }

    private fun Long.startOfDay(): Long = java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.endOfDay(): Long = java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L
}
