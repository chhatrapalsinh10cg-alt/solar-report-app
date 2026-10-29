package com.example.data.model

import java.util.Locale

object PlantConstants {
    const val PLANT_NAME = "SUKHPAR RUSHIKESH SOLAR"
    const val DC_CAPACITY_KW = 5407.5
    const val AC_CAPACITY_KW = 4000.0
    const val INVERTER_COUNT = 14
    
    val WEATHER_OPTIONS = listOf(
        "Sunny",
        "Partly Cloudy",
        "Cloudy",
        "Cloudy with Light Rain",
        "Rainy"
    )
}

data class GenerationCalculationResult(
    val plantGenKwh: Double,
    val ssGenKwh: Double,
    val plantYield: Double,
    val ssYield: Double,
    val lossKwh: Double,
    val lossPercentage: Double,
    val isValid: Boolean,
    val validationIssues: List<String>
)

object PlantCalculationEngine {
    
    fun calculate(
        todayPlantImport: Double?,
        todayPlantExport: Double?,
        previousPlantImport: Double?,
        previousPlantExport: Double?,
        todaySSExport: Double?,
        todaySSImport: Double?,
        previousSSExport: Double?,
        previousSSImport: Double?,
        isMWhUnit: Boolean = true
    ): GenerationCalculationResult {
        val issues = mutableListOf<String>()
        
        if (todayPlantImport == null || todayPlantExport == null ||
            previousPlantImport == null || previousPlantExport == null) {
            issues.add("Plant End meter readings are incomplete.")
        }
        
        if (todaySSExport == null || todaySSImport == null ||
            previousSSExport == null || previousSSImport == null) {
            issues.add("S/S End meter readings are incomplete.")
        }
        
        val tPlantImp = todayPlantImport ?: 0.0
        val tPlantExp = todayPlantExport ?: 0.0
        val pPlantImp = previousPlantImport ?: 0.0
        val pPlantExp = previousPlantExport ?: 0.0
        
        val tSSExp = todaySSExport ?: 0.0
        val tSSImp = todaySSImport ?: 0.0
        val pSSExp = previousSSExport ?: 0.0
        val pSSImp = previousSSImport ?: 0.0
        
        // Multiplier
        val multiplier = if (isMWhUnit) 1000.0 else 1.0
        
        // Plant Generation kWh = (Today Plant Export - Previous Plant Export + Previous Plant Import - Today Plant Import) * 1000
        val plantGenKwh = ((tPlantExp - pPlantExp) + (pPlantImp - tPlantImp)) * multiplier
        
        // S/S Generation kWh = (Today S/S Import - Previous S/S Import + Previous S/S Export - Today S/S Export) * 1000
        val ssGenKwh = ((tSSImp - pSSImp) + (pSSExp - tSSExp)) * multiplier
        
        // Plant DC Yield = Plant Generation kWh / 5407.5
        val plantYield = if (PlantConstants.DC_CAPACITY_KW > 0) plantGenKwh / PlantConstants.DC_CAPACITY_KW else 0.0
        
        // S/S DC Yield = S/S Generation kWh / 5407.5
        val ssYield = if (PlantConstants.DC_CAPACITY_KW > 0) ssGenKwh / PlantConstants.DC_CAPACITY_KW else 0.0
        
        // Loss kWh = Plant Generation kWh - S/S Generation kWh
        val lossKwh = plantGenKwh - ssGenKwh
        
        // Loss Percentage = Loss kWh / Plant Generation kWh * 100
        val lossPercentage = if (plantGenKwh > 0.0001) (lossKwh / plantGenKwh) * 100.0 else 0.0
        
        // Validations
        if (todayPlantExport != null && previousPlantExport != null && tPlantExp < pPlantExp) {
            issues.add("Today Plant Export ($tPlantExp) is less than Previous ($pPlantExp). Verify meter rollover or entry.")
        }
        if (todaySSImport != null && previousSSImport != null && tSSImp < pSSImp) {
            issues.add("Today S/S Import ($tSSImp) is less than Previous ($pSSImp). Verify S/S readings.")
        }
        if (plantGenKwh < 0) {
            issues.add("Calculated Plant Generation is negative ($plantGenKwh kWh). Check export/import readings.")
        }
        if (ssGenKwh < 0) {
            issues.add("Calculated S/S Generation is negative ($ssGenKwh kWh). Check S/S export/import readings.")
        }
        if (plantGenKwh > 0 && (lossPercentage > 25.0 || lossPercentage < -5.0)) {
            issues.add(String.format(Locale.US, "Unusual loss percentage (%.2f%%). Check transmission or readings.", lossPercentage))
        }
        
        val isValid = issues.isEmpty()
        
        return GenerationCalculationResult(
            plantGenKwh = plantGenKwh,
            ssGenKwh = ssGenKwh,
            plantYield = plantYield,
            ssYield = ssYield,
            lossKwh = lossKwh,
            lossPercentage = lossPercentage,
            isValid = isValid,
            validationIssues = issues
        )
    }
    
    fun formatMeterValue(value: Double?): String {
        if (value == null) return ""
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            // Keep up to 4 decimals, trim trailing zeros
            val str = String.format(Locale.US, "%.4f", value)
            str.trimEnd('0').trimEnd('.')
        }
    }
    
    fun formatGeneration(kwh: Double): String {
        return String.format(Locale.US, "%.2f", kwh)
    }
    
    fun formatYield(yield: Double): String {
        return String.format(Locale.US, "%.2f", yield)
    }
    
    fun formatLoss(lossPercent: Double): String {
        return String.format(Locale.US, "%.2f", lossPercent)
    }
    
    /**
     * Exact WhatsApp format requested:
     * sukhpar rushikesh solar 5407.5 KW dc
     * Date : DD/MM/YYYY
     * Plant End
     * Today import Unit : {todayPlantImport}
     * Today export unit : {todayPlantExport}
     * Previous import unit : {previousPlantImport}
     * Previous export unit : {previousPlantExport}
     * Total:{plantGeneration}kWh
     * Unit/kw:{plantYield}kWh/kW
     * 🧷 S/S End
     * Today export Unit : {todaySSExport}
     * Today import unit : {todaySSImport}
     * Previous export unit : {previousSSExport}
     * Previous import unit : {previousSSImport}
     * Total:{ssGeneration}kWh
     * Unit/kW:{ssYield}kWh/kW
     * Loss:{lossPercentage}%
     * {weather}
     * 📍 cuirtament{curtailmentPercentage}% time {curtailmentTime}
     * 📍Realise{releasePercentage}% time {releaseTime}
     */
    fun buildExactReportText(
        displayDate: String,
        todayPlantImport: String,
        todayPlantExport: String,
        previousPlantImport: String,
        previousPlantExport: String,
        plantGeneration: String,
        plantYield: String,
        todaySSExport: String,
        todaySSImport: String,
        previousSSExport: String,
        previousSSImport: String,
        ssGeneration: String,
        ssYield: String,
        lossPercentage: String,
        weather: String,
        curtailmentPercentage: String,
        curtailmentTime: String,
        releasePercentage: String,
        releaseTime: String
    ): String {
        val cPerc = if (curtailmentPercentage.isBlank()) "0" else curtailmentPercentage.trim()
        val cTime = if (curtailmentTime.isBlank()) "None" else curtailmentTime.trim()
        val rPerc = if (releasePercentage.isBlank()) "100" else releasePercentage.trim()
        val rTime = if (releaseTime.isBlank()) "Full day" else releaseTime.trim()
        
        return buildString {
            appendLine("sukhpar rushikesh solar 5407.5 KW dc")
            appendLine("Date : $displayDate")
            appendLine("Plant End")
            appendLine("Today import Unit : $todayPlantImport")
            appendLine("Today export unit : $todayPlantExport")
            appendLine("Previous import unit : $previousPlantImport")
            appendLine("Previous export unit : $previousPlantExport")
            appendLine("Total:${plantGeneration}kWh")
            appendLine("Unit/kw:${plantYield}kWh/kW")
            appendLine("🧷 S/S End")
            appendLine("Today export Unit : $todaySSExport")
            appendLine("Today import unit : $todaySSImport")
            appendLine("Previous export unit : $previousSSExport")
            appendLine("Previous import unit : $previousSSImport")
            appendLine("Total:${ssGeneration}kWh")
            appendLine("Unit/kW:${ssYield}kWh/kW")
            appendLine("Loss:${lossPercentage}%")
            appendLine(weather)
            appendLine("📍 cuirtament${cPerc}% time $cTime")
            append("📍Realise${rPerc}% time $rTime")
        }
    }
}
