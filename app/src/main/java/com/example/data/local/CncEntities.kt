package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machine_profiles")
data class MachineProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hostIp: String,
    val port: Int = 8000,
    val architecture: String = "ETHERCAT_DELTA",
    val coordinateSystem: String = "G54",
    val isDefault: Boolean = false,
    val lastConnectedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "mdi_macros")
data class MdiMacroEntity(
    @PrimaryKey
    val id: String,
    val label: String,
    val command: String,
    val description: String,
    val category: String = "SETUP"
)

/**
 * Persisted Work Coordinate System (WCS) origin offsets relative to G53 (Machine Zero).
 * Supports G54 (P1) through G59.3 (P9) according to LinuxCNC RS274NGC standards.
 */
@Entity(tableName = "wcs_offsets")
data class WcsOffsetEntity(
    @PrimaryKey
    val name: String, // e.g. "G54", "G55", "G56", "G57", "G58", "G59", "G59.1", "G59.2", "G59.3"
    val pIndex: Int, // 1 to 9
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val a: Double = 0.0,
    val b: Double = 0.0,
    val c: Double = 0.0,
    val comment: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Persisted MDI Command history and user favorites for rapid workshop execution.
 */
@Entity(tableName = "mdi_history")
data class MdiHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val executionStatus: String = "SUCCESS" // "SUCCESS", "SYNTAX_ERROR", "EXEC_ERROR"
)
