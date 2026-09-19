package com.expensetracker.app.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.entity.FuelLogEntity
import com.expensetracker.app.data.local.dao.FuelLogDao
import com.expensetracker.app.data.repository.FuelRepository
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
    private val fuelRepository: FuelRepository,
) : ViewModel() {
    private val profileId = profileRepository.observeActiveProfileId()

    val uiState: StateFlow<FuelTrackerUiState> = profileId.flatMapLatest { id ->
        if (id == null) flowOf(FuelTrackerUiState()) else {
            fuelLogDao.observeAll(id).map { logs ->
                val totalSpent = logs.sumOf { it.amountMinor }
                val liters = logs.sumOf { it.liters }
                val averagePrice = if (liters > 0) totalSpent / 100.0 / liters else 0.0
                val completed = logs.mapNotNull { log ->
                    val end = log.endOdometerKm ?: return@mapNotNull null
                    val distance = end - log.odometerKm
                    if (distance > 0 && log.liters > 0) distance to log.liters else null
                }
                val mileage = if (completed.isNotEmpty()) completed.sumOf { it.first } / completed.sumOf { it.second } else null
                FuelTrackerUiState(logs, totalSpent, liters, averagePrice, mileage, id)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelTrackerUiState())

    fun addFuel(date: LocalDate, liters: Double, pricePerLiter: Double, odometerKm: Double, note: String) {
        val id = uiState.value.activeProfileId ?: return
        viewModelScope.launch { fuelRepository.addFuel(id, date, liters, pricePerLiter, odometerKm, note) }
    }

    fun deleteFuel(log: FuelLogEntity) = viewModelScope.launch { fuelLogDao.delete(log) }
}
