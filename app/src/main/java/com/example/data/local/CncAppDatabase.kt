package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineProfileDao {
    @Query("SELECT * FROM machine_profiles ORDER BY isDefault DESC, lastConnectedTime DESC")
    fun getAllProfiles(): Flow<List<MachineProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: MachineProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: MachineProfileEntity)

    @Query("DELETE FROM machine_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)
}

@Dao
interface MdiMacroDao {
    @Query("SELECT * FROM mdi_macros")
    fun getAllMacros(): Flow<List<MdiMacroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacros(macros: List<MdiMacroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MdiMacroEntity)

    @Query("DELETE FROM mdi_macros WHERE id = :id")
    suspend fun deleteMacro(id: String)
}

@Dao
interface WcsOffsetDao {
    @Query("SELECT * FROM wcs_offsets ORDER BY pIndex ASC")
    fun getAllOffsets(): Flow<List<WcsOffsetEntity>>

    @Query("SELECT * FROM wcs_offsets WHERE name = :name LIMIT 1")
    suspend fun getOffsetByName(name: String): WcsOffsetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(offset: WcsOffsetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(offsets: List<WcsOffsetEntity>)

    @Query("UPDATE wcs_offsets SET x = :x, updatedAt = :time WHERE name = :name")
    suspend fun updateX(name: String, x: Double, time: Long = System.currentTimeMillis())

    @Query("UPDATE wcs_offsets SET y = :y, updatedAt = :time WHERE name = :name")
    suspend fun updateY(name: String, y: Double, time: Long = System.currentTimeMillis())

    @Query("UPDATE wcs_offsets SET z = :z, updatedAt = :time WHERE name = :name")
    suspend fun updateZ(name: String, z: Double, time: Long = System.currentTimeMillis())

    @Query("UPDATE wcs_offsets SET a = :a, updatedAt = :time WHERE name = :name")
    suspend fun updateA(name: String, a: Double, time: Long = System.currentTimeMillis())
}

@Dao
interface MdiHistoryDao {
    @Query("SELECT * FROM mdi_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 30): Flow<List<MdiHistoryEntity>>

    @Query("SELECT * FROM mdi_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<MdiHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: MdiHistoryEntity): Long

    @Query("UPDATE mdi_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM mdi_history WHERE isFavorite = 0")
    suspend fun clearNonFavorites()
}

@Database(
    entities = [
        MachineProfileEntity::class,
        MdiMacroEntity::class,
        WcsOffsetEntity::class,
        MdiHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CncAppDatabase : RoomDatabase() {
    abstract fun profileDao(): MachineProfileDao
    abstract fun macroDao(): MdiMacroDao
    abstract fun wcsOffsetDao(): WcsOffsetDao
    abstract fun mdiHistoryDao(): MdiHistoryDao
}
