package com.expensetracker.app.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.entity.FuelLogEntity
import com.expensetracker.app.data.local.dao.FuelLogDao
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FuelTrackerUiState(
    val logs: List<FuelLogEntity> = emptyList(),
    val totalSpentMinor: Long = 0L,
    val totalLiters: Double = 0.0,
    val averagePricePerLiter: Double = 0.0,
    val averageMileage: Double? = null,
    val activeProfileId: Long? = null,
)

class FuelTrackerViewModel(
    private val fuelLogDao: FuelLogDao,
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val profileId = profileRepository.observeActiveProfileId()

    val uiState: StateFlow<FuelTrackerUiState> = profileId.flatMapLatest { id ->
        if (id == null) {
            flowOf(FuelTrackerUiState())
        } else {
            fuelLogDao.observeAll(id).map { logs ->
                val totalSpent = logs.sumOf { it.amountMinor }
                val liters = logs.sumOf { it.liters }
                val averagePrice = if (liters > 0) totalSpent / 100.0 / liters else 0.0
                val ordered = logs.sortedBy { it.odometerKm }
                val distance = if (ordered.size >= 2) ordered.last().odometerKm - ordered.first().odometerKm else 0.0
                val fuelForMileage = if (ordered.size >= 2) ordered.drop(1).sumOf { it.liters } else 0.0
                val mileage = if (distance > 0 && fuelForMileage > 0) distance / fuelForMileage else null
                FuelTrackerUiState(logs, totalSpent, liters, averagePrice, mileage, id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelTrackerUiState())

    fun addFuel(amount: Double, liters: Double, odometerKm: Double, note: String) {
        val id = uiState.value.activeProfileId ?: return
        if (amount <= 0 || liters <= 0 || odometerKm < 0) return
        viewModelScope.launch {
            fuelLogDao.insert(FuelLogEntity(profileId = id, amountMinor = (amount * 100).toLong(), liters = liters, odometerKm = odometerKm, epochDay = LocalDate.now().toEpochDay(), note = note.trim()))
        }
    }

    fun deleteFuel(log: FuelLogEntity) = viewModelScope.launch { fuelLogDao.delete(log) }
}
