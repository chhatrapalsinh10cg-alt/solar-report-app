package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DailyReadingRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(entities = [DailyReadingRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyReadingDao(): DailyReadingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "solar_plant_reports.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with realistic baseline historical records
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateBaselineData(database.dailyReadingDao())
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateBaselineData(dao: DailyReadingDao) {
            val count = dao.getCount()
            if (count == 0) {
                val calendar = Calendar.getInstance()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                
                // Baseline: 2 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -2)
                val date2DaysAgoIso = isoFormat.format(calendar.time)
                val date2DaysAgoDisplay = displayFormat.format(calendar.time)
                
                // Yesterday baseline
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val yesterdayIso = isoFormat.format(calendar.time)
                val yesterdayDisplay = displayFormat.format(calendar.time)
                
                // Day before yesterday record
                val recordTwoDaysAgo = DailyReadingRecord(
                    date = date2DaysAgoIso,
                    displayDate = date2DaysAgoDisplay,
                    todayPlantImport = 12.450,
                    todayPlantExport = 1530.820,
                    previousPlantImport = 12.210,
                    previousPlantExport = 1506.120,
                    todaySSExport = 15.680,
                    todaySSImport = 1502.340,
                    previousSSExport = 15.420,
                    previousSSImport = 1478.100,
                    plantGenerationKwh = 24460.0,
                    ssGenerationKwh = 23980.0,
                    plantYield = 4.52,
                    ssYield = 4.43,
                    lossKwh = 480.0,
                    lossPercentage = 1.96,
                    weather = "Sunny",
                    curtailmentPercentage = "0",
                    curtailmentTime = "None",
                    releasePercentage = "100",
                    releaseTime = "Full day",
                    remarks = "Normal generation"
                )
                dao.insertRecord(recordTwoDaysAgo)

                // Yesterday record (which acts as the baseline for Today!)
                val recordYesterday = DailyReadingRecord(
                    date = yesterdayIso,
                    displayDate = yesterdayDisplay,
                    todayPlantImport = 12.680,
                    todayPlantExport = 1555.750,
                    previousPlantImport = 12.450,
                    previousPlantExport = 1530.820,
                    todaySSExport = 15.940,
                    todaySSImport = 1526.850,
                    previousSSExport = 15.680,
                    previousSSImport = 1502.340,
                    plantGenerationKwh = 24700.0,
                    ssGenerationKwh = 24250.0,
                    plantYield = 4.57,
                    ssYield = 4.48,
                    lossKwh = 450.0,
                    lossPercentage = 1.82,
                    weather = "Sunny",
                    curtailmentPercentage = "0",
                    curtailmentTime = "None",
                    releasePercentage = "100",
                    releaseTime = "Full day",
                    remarks = "Optimal operation"
                )
                dao.insertRecord(recordYesterday)
            }
        }
    }
}
