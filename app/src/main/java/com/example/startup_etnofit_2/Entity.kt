package com.example.startup_etnofit_2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checks_data")
data class ChecksData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val revenue: Double,
    val numberOfChecks: Int,
    val averageCheck: Double,
    val realRevenue: Double
)

@Entity(tableName = "reckoning_data")
data class ReckoningData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val region: String,
    val electricityCurr: Double,
    val gasCurr: Double,
    val hotWaterCurr: Double,
    val coldWaterCurr: Double,
    val S: Double,
    val M: Double
)

@Entity(tableName = "previous_reckoning_data")
data class PreviousReckoningData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val region: String,
    val electricityPrev: Double,
    val gasPrev: Double,
    val hotWaterPrev: Double,
    val coldWaterPrev: Double
)