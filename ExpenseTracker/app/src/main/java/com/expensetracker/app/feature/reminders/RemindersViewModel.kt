package com.expensetracker.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.entity.NoteEntity
import com.expensetracker.app.data.entity.ReminderEntity
import com.expensetracker.app.data.local.dao.NoteDao
import com.expensetracker.app.data.local.dao.ReminderDao
import com.expensetracker.app.data.local.dao.WaterDao
import com.expensetracker.app.data.entity.WaterSettingsEntity
import com.expensetracker.app.data.entity.WaterIntakeEntity
import com.expensetracker.app.data.reminder.WaterReminderScheduler
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
    private val waterDao: WaterDao,
    private val waterScheduler: WaterReminderScheduler,
) : ViewModel() {
    private val activeProfileId = profileRepository.observeActiveProfileId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminders = activeProfileId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else reminderDao.observeForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waterSettings = activeProfileId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(null) else waterDao.observeSettings(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val waterIntake = activeProfileId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else waterDao.observeIntake(id)
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


    fun saveWaterSettings(goalMl: Int, startTimeMinutes: Int, endTimeMinutes: Int, amountMl: Int) {
        val profileId = activeProfileId.value ?: return
        if (goalMl <= 0 || amountMl <= 0) return

        val reminderCount = goalMl / amountMl
        if (reminderCount < 1 || goalMl % amountMl != 0) {
            _message.value = "Daily goal must be divisible by the intake amount"
            return
        }

        val start = startTimeMinutes.coerceIn(0, 1439)
        val end = endTimeMinutes.coerceIn(0, 1439)
        val durationMinutes = end - start
        if (reminderCount > 1 && durationMinutes <= 0) {
            _message.value = "End time must be after start time"
            return
        }

        val frequencyMinutes = if (reminderCount > 1) {
            kotlin.math.round(durationMinutes.toDouble() / (reminderCount - 1)).toInt()
        } else 0
        val legacyIntervalHours = if (frequencyMinutes > 0) {
            kotlin.math.max(1, kotlin.math.round(frequencyMinutes / 60.0).toInt())
        } else 1

        viewModelScope.launch {
            val existing = waterDao.getSettings(profileId)
            val updated = (existing ?: WaterSettingsEntity(profileId = profileId)).copy(
                enabled = true,
                dailyGoalMl = goalMl,
                intervalHours = legacyIntervalHours,
                intakePerReminderMl = amountMl,
                nextReminderAtEpochMillis = null,
                startTimeMinutes = start,
                endTimeMinutes = end,
                reminderCount = reminderCount,
            )
            waterDao.upsertSettings(updated)
            waterScheduler.schedule(updated)
            _message.value = "Water reminder enabled"
        }
    }

    fun toggleWaterReminder(enabled: Boolean) {
        val profileId = activeProfileId.value ?: return
        viewModelScope.launch {
            val existing = waterDao.getSettings(profileId)
            if (existing == null && enabled) {
                _message.value = "SETUP_WATER"
                return@launch
            }
            if (existing != null) {
                val updated = existing.copy(enabled = enabled, nextReminderAtEpochMillis = null)
                waterDao.upsertSettings(updated)
                if (enabled) waterScheduler.schedule(updated) else waterScheduler.cancel(profileId)
            }
        }
    }

    fun addWaterIntake(amountMl: Int) {
        val profileId = activeProfileId.value ?: return
        if (amountMl <= 0) return
        viewModelScope.launch {
            waterDao.insertIntake(WaterIntakeEntity(profileId = profileId, amountMl = amountMl, drankAtEpochMillis = System.currentTimeMillis(), source = "MANUAL"))
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
