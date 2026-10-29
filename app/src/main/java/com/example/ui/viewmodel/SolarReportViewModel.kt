package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DailyReadingRecord
import com.example.data.model.GenerationCalculationResult
import com.example.data.model.PlantCalculationEngine
import com.example.data.ocr.MeterOcrAnalyzer
import com.example.data.ocr.MeterOcrResult
import com.example.data.pdf.SolarReportPdfGenerator
import com.example.data.repository.SolarReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class MeterSection {
    PLANT_TODAY_IMPORT,
    PLANT_TODAY_EXPORT,
    SS_TODAY_EXPORT,
    SS_TODAY_IMPORT
}

data class MeterSlotState(
    val photoUri: Uri? = null,
    val textValue: String = "",
    val isScanning: Boolean = false,
    val ocrResult: MeterOcrResult? = null,
    val error: String? = null
)

data class SolarReportUiState(
    val selectedDateIso: String = "",
    val displayDate: String = "",
    
    // Meter Slots
    val plantTodayImport: MeterSlotState = MeterSlotState(),
    val plantTodayExport: MeterSlotState = MeterSlotState(),
    val ssTodayExport: MeterSlotState = MeterSlotState(),
    val ssTodayImport: MeterSlotState = MeterSlotState(),
    
    // Previous readings (editable, auto-populated)
    val previousPlantImport: String = "",
    val previousPlantExport: String = "",
    val previousSSExport: String = "",
    val previousSSImport: String = "",
    val hasPreviousReadingsWarning: Boolean = false,
    val previousReadingsSourceDate: String? = null,
    
    // Environmental & Operational
    val weather: String = "Sunny",
    val curtailmentPercentage: String = "0",
    val curtailmentTime: String = "None",
    val releasePercentage: String = "100",
    val releaseTime: String = "Full day",
    val remarks: String = "",
    
    // Settings
    val isMWhUnit: Boolean = true,
    
    // Calculations
    val calculation: GenerationCalculationResult = GenerationCalculationResult(
        plantGenKwh = 0.0,
        ssGenKwh = 0.0,
        plantYield = 0.0,
        ssYield = 0.0,
        lossKwh = 0.0,
        lossPercentage = 0.0,
        isValid = false,
        validationIssues = listOf("Awaiting meter readings.")
    ),
    
    // Dialog / UI state
    val showPreviewDialog: Boolean = false,
    val showDuplicateConfirmDialog: Boolean = false,
    val isSaving: Boolean = false,
    val userNotification: String? = null
)

class SolarReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SolarReportRepository
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    private val _uiState = MutableStateFlow(SolarReportUiState())
    val uiState: StateFlow<SolarReportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SolarReportRepository(db.dailyReadingDao())
        
        // Initialize with today's date and look up previous readings
        val today = Calendar.getInstance().time
        val iso = isoDateFormat.format(today)
        val display = displayDateFormat.format(today)
        
        _uiState.value = _uiState.value.copy(
            selectedDateIso = iso,
            displayDate = display
        )
        
        viewModelScope.launch {
            // Ensure baseline data is available in case of first launch
            AppDatabase.populateBaselineData(db.dailyReadingDao())
            loadPreviousReadingsForDate(iso)
        }
    }

    val historyRecords: StateFlow<List<DailyReadingRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(calendar: Calendar) {
        val iso = isoDateFormat.format(calendar.time)
        val display = displayDateFormat.format(calendar.time)
        _uiState.value = _uiState.value.copy(
            selectedDateIso = iso,
            displayDate = display
        )
        viewModelScope.launch {
            // Check if existing record exists for this date to prefill
            val existing = repository.getRecordForDate(iso)
            if (existing != null) {
                populateFromRecord(existing)
                _events.emit("Loaded existing saved report for $display.")
            } else {
                loadPreviousReadingsForDate(iso)
            }
            recalculate()
        }
    }

    private suspend fun loadPreviousReadingsForDate(targetDateIso: String) {
        val prevRecord = repository.getPreviousReadingRecord(targetDateIso)
        if (prevRecord != null) {
            _uiState.value = _uiState.value.copy(
                previousPlantImport = PlantCalculationEngine.formatMeterValue(prevRecord.todayPlantImport),
                previousPlantExport = PlantCalculationEngine.formatMeterValue(prevRecord.todayPlantExport),
                previousSSExport = PlantCalculationEngine.formatMeterValue(prevRecord.todaySSExport),
                previousSSImport = PlantCalculationEngine.formatMeterValue(prevRecord.todaySSImport),
                hasPreviousReadingsWarning = false,
                previousReadingsSourceDate = prevRecord.displayDate
            )
        } else {
            _uiState.value = _uiState.value.copy(
                hasPreviousReadingsWarning = true,
                previousReadingsSourceDate = null
            )
        }
        recalculate()
    }

    fun onImagePicked(section: MeterSection, uri: Uri, context: Context) {
        updateSlotState(section) { it.copy(photoUri = uri, isScanning = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val ocrResult = MeterOcrAnalyzer.analyzeImage(context, uri)
            withContext(Dispatchers.Main) {
                updateSlotState(section) { current ->
                    val newText = if (ocrResult.isClear && ocrResult.primaryReading != null) {
                        ocrResult.primaryReading
                    } else {
                        current.textValue // preserve existing if OCR was unclear
                    }
                    current.copy(
                        isScanning = false,
                        ocrResult = ocrResult,
                        textValue = newText,
                        error = if (!ocrResult.isClear) ocrResult.statusMessage else null
                    )
                }
                recalculate()
            }
        }
    }

    fun onReadingManualChanged(section: MeterSection, text: String) {
        updateSlotState(section) { it.copy(textValue = text, error = null) }
        recalculate()
    }

    fun onCandidateSelected(section: MeterSection, candidate: String) {
        updateSlotState(section) { it.copy(textValue = candidate, error = null) }
        recalculate()
    }

    fun onPreviousReadingChanged(field: String, text: String) {
        when (field) {
            "plantImport" -> _uiState.value = _uiState.value.copy(previousPlantImport = text)
            "plantExport" -> _uiState.value = _uiState.value.copy(previousPlantExport = text)
            "ssExport" -> _uiState.value = _uiState.value.copy(previousSSExport = text)
            "ssImport" -> _uiState.value = _uiState.value.copy(previousSSImport = text)
        }
        recalculate()
    }

    fun onWeatherChanged(weather: String) {
        _uiState.value = _uiState.value.copy(weather = weather)
    }

    fun onCurtailmentChanged(percentage: String, time: String) {
        _uiState.value = _uiState.value.copy(
            curtailmentPercentage = percentage,
            curtailmentTime = time
        )
    }

    fun onReleaseChanged(percentage: String, time: String) {
        _uiState.value = _uiState.value.copy(
            releasePercentage = percentage,
            releaseTime = time
        )
    }

    fun onRemarksChanged(remarks: String) {
        _uiState.value = _uiState.value.copy(remarks = remarks)
    }

    fun onCalculationModeToggled(isMWh: Boolean) {
        _uiState.value = _uiState.value.copy(isMWhUnit = isMWh)
        recalculate()
    }

    private fun updateSlotState(section: MeterSection, transform: (MeterSlotState) -> MeterSlotState) {
        val s = _uiState.value
        _uiState.value = when (section) {
            MeterSection.PLANT_TODAY_IMPORT -> s.copy(plantTodayImport = transform(s.plantTodayImport))
            MeterSection.PLANT_TODAY_EXPORT -> s.copy(plantTodayExport = transform(s.plantTodayExport))
            MeterSection.SS_TODAY_EXPORT -> s.copy(ssTodayExport = transform(s.ssTodayExport))
            MeterSection.SS_TODAY_IMPORT -> s.copy(ssTodayImport = transform(s.ssTodayImport))
        }
    }

    private fun recalculate() {
        val s = _uiState.value
        val result = PlantCalculationEngine.calculate(
            todayPlantImport = s.plantTodayImport.textValue.toDoubleOrNull(),
            todayPlantExport = s.plantTodayExport.textValue.toDoubleOrNull(),
            previousPlantImport = s.previousPlantImport.toDoubleOrNull(),
            previousPlantExport = s.previousPlantExport.toDoubleOrNull(),
            todaySSExport = s.ssTodayExport.textValue.toDoubleOrNull(),
            todaySSImport = s.ssTodayImport.textValue.toDoubleOrNull(),
            previousSSExport = s.previousSSExport.toDoubleOrNull(),
            previousSSImport = s.previousSSImport.toDoubleOrNull(),
            isMWhUnit = s.isMWhUnit
        )
        _uiState.value = _uiState.value.copy(calculation = result)
    }

    fun getFormattedReportText(): String {
        val s = _uiState.value
        val calc = s.calculation
        return PlantCalculationEngine.buildExactReportText(
            displayDate = s.displayDate,
            todayPlantImport = s.plantTodayImport.textValue,
            todayPlantExport = s.plantTodayExport.textValue,
            previousPlantImport = s.previousPlantImport,
            previousPlantExport = s.previousPlantExport,
            plantGeneration = PlantCalculationEngine.formatGeneration(calc.plantGenKwh),
            plantYield = PlantCalculationEngine.formatYield(calc.plantYield),
            todaySSExport = s.ssTodayExport.textValue,
            todaySSImport = s.ssTodayImport.textValue,
            previousSSExport = s.previousSSExport,
            previousSSImport = s.previousSSImport,
            ssGeneration = PlantCalculationEngine.formatGeneration(calc.ssGenKwh),
            ssYield = PlantCalculationEngine.formatYield(calc.ssYield),
            lossPercentage = PlantCalculationEngine.formatLoss(calc.lossPercentage),
            weather = s.weather,
            curtailmentPercentage = s.curtailmentPercentage,
            curtailmentTime = s.curtailmentTime,
            releasePercentage = s.releasePercentage,
            releaseTime = s.releaseTime
        )
    }

    fun showPreview(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPreviewDialog = show)
    }

    fun checkAndSaveReport() {
        val s = _uiState.value
        if (!s.calculation.isValid) {
            viewModelScope.launch {
                val issue = s.calculation.validationIssues.firstOrNull() ?: "Please complete all meter readings."
                _events.emit("Validation failed: $issue")
            }
            return
        }

        viewModelScope.launch {
            val existing = repository.getRecordForDate(s.selectedDateIso)
            if (existing != null) {
                // Duplicate check - prompt user
                _uiState.value = _uiState.value.copy(showDuplicateConfirmDialog = true)
            } else {
                performSave()
            }
        }
    }

    fun confirmOverwriteAndSave() {
        _uiState.value = _uiState.value.copy(showDuplicateConfirmDialog = false)
        performSave()
    }

    fun dismissDuplicateDialog() {
        _uiState.value = _uiState.value.copy(showDuplicateConfirmDialog = false)
    }

    private fun performSave() {
        val s = _uiState.value
        val calc = s.calculation
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val record = DailyReadingRecord(
                    date = s.selectedDateIso,
                    displayDate = s.displayDate,
                    todayPlantImport = s.plantTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    todayPlantExport = s.plantTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    previousPlantImport = s.previousPlantImport.toDoubleOrNull() ?: 0.0,
                    previousPlantExport = s.previousPlantExport.toDoubleOrNull() ?: 0.0,
                    todaySSExport = s.ssTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    todaySSImport = s.ssTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    previousSSExport = s.previousSSExport.toDoubleOrNull() ?: 0.0,
                    previousSSImport = s.previousSSImport.toDoubleOrNull() ?: 0.0,
                    plantGenerationKwh = calc.plantGenKwh,
                    ssGenerationKwh = calc.ssGenKwh,
                    plantYield = calc.plantYield,
                    ssYield = calc.ssYield,
                    lossKwh = calc.lossKwh,
                    lossPercentage = calc.lossPercentage,
                    weather = s.weather,
                    curtailmentPercentage = s.curtailmentPercentage,
                    curtailmentTime = s.curtailmentTime,
                    releasePercentage = s.releasePercentage,
                    releaseTime = s.releaseTime,
                    remarks = s.remarks,
                    isMWhUnit = s.isMWhUnit,
                    plantImportPhotoUri = s.plantTodayImport.photoUri?.toString(),
                    plantExportPhotoUri = s.plantTodayExport.photoUri?.toString(),
                    ssExportPhotoUri = s.ssTodayExport.photoUri?.toString(),
                    ssImportPhotoUri = s.ssTodayImport.photoUri?.toString()
                )
                repository.saveRecord(record)
                _events.emit("Daily readings saved successfully for ${s.displayDate}!")
            } catch (e: Exception) {
                _events.emit("Failed to save report: ${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun populateFromRecord(record: DailyReadingRecord) {
        _uiState.value = _uiState.value.copy(
            selectedDateIso = record.date,
            displayDate = record.displayDate,
            plantTodayImport = MeterSlotState(
                textValue = PlantCalculationEngine.formatMeterValue(record.todayPlantImport),
                photoUri = record.plantImportPhotoUri?.let { Uri.parse(it) }
            ),
            plantTodayExport = MeterSlotState(
                textValue = PlantCalculationEngine.formatMeterValue(record.todayPlantExport),
                photoUri = record.plantExportPhotoUri?.let { Uri.parse(it) }
            ),
            ssTodayExport = MeterSlotState(
                textValue = PlantCalculationEngine.formatMeterValue(record.todaySSExport),
                photoUri = record.ssExportPhotoUri?.let { Uri.parse(it) }
            ),
            ssTodayImport = MeterSlotState(
                textValue = PlantCalculationEngine.formatMeterValue(record.todaySSImport),
                photoUri = record.ssImportPhotoUri?.let { Uri.parse(it) }
            ),
            previousPlantImport = PlantCalculationEngine.formatMeterValue(record.previousPlantImport),
            previousPlantExport = PlantCalculationEngine.formatMeterValue(record.previousPlantExport),
            previousSSExport = PlantCalculationEngine.formatMeterValue(record.previousSSExport),
            previousSSImport = PlantCalculationEngine.formatMeterValue(record.previousSSImport),
            weather = record.weather,
            curtailmentPercentage = record.curtailmentPercentage,
            curtailmentTime = record.curtailmentTime,
            releasePercentage = record.releasePercentage,
            releaseTime = record.releaseTime,
            remarks = record.remarks,
            isMWhUnit = record.isMWhUnit,
            hasPreviousReadingsWarning = false
        )
        recalculate()
    }

    fun deleteHistoricalRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            _events.emit("Record removed from history.")
        }
    }

    fun copyReportToClipboard(context: Context) {
        val reportText = getFormattedReportText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Solar Daily Report", reportText)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _events.emit("Report copied! Ready to paste into WhatsApp.")
        }
    }

    fun shareReportText(context: Context) {
        val reportText = getFormattedReportText()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Solar Report via WhatsApp / Apps")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun downloadAsTxt(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = _uiState.value
                val calc = s.calculation
                val record = DailyReadingRecord(
                    date = s.selectedDateIso,
                    displayDate = s.displayDate,
                    todayPlantImport = s.plantTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    todayPlantExport = s.plantTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    previousPlantImport = s.previousPlantImport.toDoubleOrNull() ?: 0.0,
                    previousPlantExport = s.previousPlantExport.toDoubleOrNull() ?: 0.0,
                    todaySSExport = s.ssTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    todaySSImport = s.ssTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    previousSSExport = s.previousSSExport.toDoubleOrNull() ?: 0.0,
                    previousSSImport = s.previousSSImport.toDoubleOrNull() ?: 0.0,
                    plantGenerationKwh = calc.plantGenKwh,
                    ssGenerationKwh = calc.ssGenKwh,
                    plantYield = calc.plantYield,
                    ssYield = calc.ssYield,
                    lossKwh = calc.lossKwh,
                    lossPercentage = calc.lossPercentage,
                    weather = s.weather,
                    curtailmentPercentage = s.curtailmentPercentage,
                    curtailmentTime = s.curtailmentTime,
                    releasePercentage = s.releasePercentage,
                    releaseTime = s.releaseTime,
                    remarks = s.remarks,
                    isMWhUnit = s.isMWhUnit
                )
                val file = SolarReportPdfGenerator.saveReportAsTxt(context, record, getFormattedReportText())
                shareFile(context, file, "text/plain")
                _events.emit("TXT report created at ${file.name}")
            } catch (e: Exception) {
                _events.emit("Error generating TXT: ${e.localizedMessage}")
            }
        }
    }

    fun downloadAsPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = _uiState.value
                val calc = s.calculation
                val record = DailyReadingRecord(
                    date = s.selectedDateIso,
                    displayDate = s.displayDate,
                    todayPlantImport = s.plantTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    todayPlantExport = s.plantTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    previousPlantImport = s.previousPlantImport.toDoubleOrNull() ?: 0.0,
                    previousPlantExport = s.previousPlantExport.toDoubleOrNull() ?: 0.0,
                    todaySSExport = s.ssTodayExport.textValue.toDoubleOrNull() ?: 0.0,
                    todaySSImport = s.ssTodayImport.textValue.toDoubleOrNull() ?: 0.0,
                    previousSSExport = s.previousSSExport.toDoubleOrNull() ?: 0.0,
                    previousSSImport = s.previousSSImport.toDoubleOrNull() ?: 0.0,
                    plantGenerationKwh = calc.plantGenKwh,
                    ssGenerationKwh = calc.ssGenKwh,
                    plantYield = calc.plantYield,
                    ssYield = calc.ssYield,
                    lossKwh = calc.lossKwh,
                    lossPercentage = calc.lossPercentage,
                    weather = s.weather,
                    curtailmentPercentage = s.curtailmentPercentage,
                    curtailmentTime = s.curtailmentTime,
                    releasePercentage = s.releasePercentage,
                    releaseTime = s.releaseTime,
                    remarks = s.remarks,
                    isMWhUnit = s.isMWhUnit
                )
                val pdfFile = SolarReportPdfGenerator.generatePdf(context, record)
                shareFile(context, pdfFile, "application/pdf")
                _events.emit("PDF report generated at ${pdfFile.name}")
            } catch (e: Exception) {
                _events.emit("Error generating PDF: ${e.localizedMessage}")
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Solar Plant Daily Report: ${_uiState.value.displayDate}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Open or Share Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun resetSampleData() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            AppDatabase.populateBaselineData(db.dailyReadingDao())
            loadPreviousReadingsForDate(_uiState.value.selectedDateIso)
            _events.emit("Baseline sample data restored.")
        }
    }
}
