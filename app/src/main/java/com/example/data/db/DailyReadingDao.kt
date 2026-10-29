package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyReadingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReadingDao {
    @Query("SELECT * FROM daily_readings ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyReadingRecord>>

    @Query("SELECT * FROM daily_readings WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): DailyReadingRecord?

    // Retrieves the most recent valid reading recorded before the specified date
    @Query("SELECT * FROM daily_readings WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecordBeforeDate(date: String): DailyReadingRecord?

    @Query("SELECT * FROM daily_readings ORDER BY date DESC LIMIT 1")
    suspend fun getLatestRecord(): DailyReadingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyReadingRecord): Long

    @Update
    suspend fun updateRecord(record: DailyReadingRecord)

    @Query("DELETE FROM daily_readings WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM daily_readings WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)

    @Query("SELECT COUNT(*) FROM daily_readings")
    suspend fun getCount(): Int
}
