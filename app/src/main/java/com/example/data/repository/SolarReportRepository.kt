package com.example.data.repository

import com.example.data.db.DailyReadingDao
import com.example.data.model.DailyReadingRecord
import kotlinx.coroutines.flow.Flow

class SolarReportRepository(private val dao: DailyReadingDao) {

    val allRecords: Flow<List<DailyReadingRecord>> = dao.getAllRecords()

    suspend fun getRecordForDate(date: String): DailyReadingRecord? {
        return dao.getRecordByDate(date)
    }

    /**
     * Retrieves the most recent valid readings recorded prior to the given date.
     * Note: Does not use today's reading as yesterday's reading.
     */
    suspend fun getPreviousReadingRecord(date: String): DailyReadingRecord? {
        return dao.getLatestRecordBeforeDate(date)
    }

    suspend fun getLatestRecord(): DailyReadingRecord? {
        return dao.getLatestRecord()
    }

    suspend fun saveRecord(record: DailyReadingRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun deleteRecordByDate(date: String) {
        dao.deleteRecordByDate(date)
    }
}
