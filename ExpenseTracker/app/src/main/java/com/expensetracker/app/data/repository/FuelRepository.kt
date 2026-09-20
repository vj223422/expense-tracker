package com.expensetracker.app.data.repository

import com.expensetracker.app.data.entity.FuelLogEntity
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.FuelLogDao
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.LocalDate

class FuelRepository(
    private val fuelLogDao: FuelLogDao,
    private val expenseDao: ExpenseDao,
    private val expenseRepository: ExpenseRepository,
) {
    suspend fun addFuel(
        profileId: Long,
        date: LocalDate,
        liters: Double,
        pricePerLiter: Double,
        odometerKm: Double,
        note: String,
    ): AddExpenseResult {
        if (liters <= 0.0 || pricePerLiter <= 0.0 || odometerKm < 0.0) {
            return AddExpenseResult.Error("Enter valid fuel, price and odometer values.")
        }

        val amountMinor = (liters * pricePerLiter * 100.0).toLong()
        if (amountMinor <= 0L) return AddExpenseResult.Error("Fuel amount must be greater than zero.")

        val cleanNote = note.trim()
        val expenseResult = expenseRepository.addExpense(
            profileId = profileId,
            amountMinor = amountMinor,
            category = ExpenseCategory.PETROL,
            note = cleanNote,
            date = date,
        )
        if (expenseResult !is AddExpenseResult.Success) return expenseResult

        val previous = fuelLogDao.getOpenCycle(profileId)
        if (previous != null && previous.epochDay <= date.toEpochDay() && odometerKm >= previous.odometerKm) {
            fuelLogDao.closeCycle(previous.id, date.toEpochDay(), odometerKm)
        }

        fuelLogDao.insert(
            FuelLogEntity(
                profileId = profileId,
                amountMinor = amountMinor,
                liters = liters,
                odometerKm = odometerKm,
                epochDay = date.toEpochDay(),
                note = cleanNote,
                expenseId = expenseResult.expenseId,
            ),
        )
        return expenseResult
    }

    suspend fun deleteFuel(log: FuelLogEntity) {
        // New fuel records have an exact expense ID. Legacy records are matched by their
        // original fuel fields so deletion also removes the corresponding Home transaction.
        val expense = log.expenseId?.let { expenseRepository.getExpenseById(it, log.profileId) }
            ?: expenseDao.findMatchingFuelExpense(
                profileId = log.profileId,
                amountMinor = log.amountMinor,
                category = ExpenseCategory.PETROL,
                note = log.note,
                epochDay = log.epochDay,
            )?.toDomain()

        fuelLogDao.delete(log)
        if (expense != null) {
            expenseRepository.deleteExpense(expense)
        }
    }
}
