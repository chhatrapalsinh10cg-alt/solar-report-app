package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_readings",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyReadingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO format: yyyy-MM-dd
    val displayDate: String, // Formatted: dd/MM/yyyy
    
    // Plant End readings
    val todayPlantImport: Double,
    val todayPlantExport: Double,
    val previousPlantImport: Double,
    val previousPlantExport: Double,
    
    // S/S End readings
    val todaySSExport: Double,
    val todaySSImport: Double,
    val previousSSExport: Double,
    val previousSSImport: Double,
    
    // Calculated metrics
    val plantGenerationKwh: Double,
    val ssGenerationKwh: Double,
    val plantYield: Double,
    val ssYield: Double,
    val lossKwh: Double,
    val lossPercentage: Double,
    
    // Operational and environmental
    val weather: String, // Sunny, Partly Cloudy, Cloudy, Cloudy with Light Rain, Rainy
    val curtailmentPercentage: String,
    val curtailmentTime: String,
    val releasePercentage: String,
    val releaseTime: String,
    val remarks: String = "",
    
    // Configuration
    val isMWhUnit: Boolean = true,
    
    // Image attachments (optional local URI strings)
    val plantImportPhotoUri: String? = null,
    val plantExportPhotoUri: String? = null,
    val ssExportPhotoUri: String? = null,
    val ssImportPhotoUri: String? = null,
    
    val createdAt: Long = System.currentTimeMillis()
)
