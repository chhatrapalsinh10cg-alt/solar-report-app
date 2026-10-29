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
    val validationIssues: List<String> = emptyList()
)

fun formatMeterValue(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

fun formatYield(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

fun formatLoss(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

fun buildShareReportText(
    date: String,
    todayPlantExp: Double,
    prevPlantExp: Double,
    todayPlantImp: Double,
    prevPlantImp: Double,
    todaySSExp: Double,
    prevSSExp: Double,
    todaySSImp: Double,
    prevSSImp: Double,
    plantGenKwh: Double,
    ssGenKwh: Double,
    plantYield: Double,
    ssYield: Double,
    lossPercentage: Double,
    weather: String = "Cloudy whether 🌤️☀️"
): String {
    return """
*sukhpar rushikesh solar ${PlantConstants.DC_CAPACITY_KW} KW dc*

Date : $date

*Plant End*
Today import Unit :${formatMeterValue(todayPlantImp)}
Today export unit :${formatMeterValue(todayPlantExp)}
Previous import unit :${formatMeterValue(prevPlantImp)}
Previous export unit :${formatMeterValue(prevPlantExp)}

Total:${String.format(Locale.US, "%.0f", plantGenKwh)}kWh
Unit/kw:${formatYield(plantYield)}kWh/kW

*🧷 S/S End*
Today export Unit :${formatMeterValue(todaySSExp)}
Today import unit :${formatMeterValue(todaySSImp)}
Previous export unit :${formatMeterValue(prevSSExp)}
Previous import unit :${formatMeterValue(prevSSImp)}

Total:${String.format(Locale.US, "%.0f", ssGenKwh)}kWh
Unit/kW:${formatYield(ssYield)}kWh/kW

*Loss:${formatLoss(lossPercentage)}%*
$weather
    """.trimIndent()
}

object PlantCalculationEngine {

    fun formatMeterValue(value: Double): String = com.example.data.model.formatMeterValue(value)
    fun formatYield(value: Double): String = com.example.data.model.formatYield(value)
    fun formatLoss(value: Double): String = com.example.data.model.formatLoss(value)
    fun buildShareReportText(
        date: String,
        todayPlantExp: Double,
        prevPlantExp: Double,
        todayPlantImp: Double,
        prevPlantImp: Double,
        todaySSExp: Double,
        prevSSExp: Double,
        todaySSImp: Double,
        prevSSImp: Double,
        plantGenKwh: Double,
        ssGenKwh: Double,
        plantYield: Double,
        ssYield: Double,
        lossPercentage: Double,
        weather: String = "Cloudy whether 🌤️☀️"
    ): String = com.example.data.model.buildShareReportText(
        date, todayPlantExp, prevPlantExp, todayPlantImp, prevPlantImp,
        todaySSExp, prevSSExp, todaySSImp, prevSSImp, plantGenKwh,
        ssGenKwh, plantYield, ssYield, lossPercentage, weather
    )

    fun calculate(
        todayPlantImport: Double?,
        todayPlantExport: Double?,
        previousPlantImport: Double?,
        previousPlantExport: Double?,
        todaySSExport: Double?,
        todaySSImport: Double?,
        previousSSExport: Double?,
        previousSSImport: Double?,
        isMwhUnit: Boolean = true
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

        // Multiplier: MF 5000 fix
        val multiplier = if (isMwhUnit) 5000.0 else 1.0

        // Plant Gen kWh = (Today Export - Previous Export) * MF
        val plantGenKwh = (tPlantExp - pPlantExp) * multiplier

        // S/S Gen kWh = (Today Import - Previous Import) * MF
        val ssGenKwh = (tSSImp - pSSImp) * multiplier

        // Plant DC Yield = Plant Gen / DC Capacity
        val plantYield = if (PlantConstants.DC_CAPACITY_KW > 0.0 && plantGenKwh > 0.0) {
            plantGenKwh / PlantConstants.DC_CAPACITY_KW
        } else 0.0

        // S/S DC Yield = S/S Gen / DC Capacity
        val ssYield = if (PlantConstants.DC_CAPACITY_KW > 0.0 && ssGenKwh > 0.0) {
            ssGenKwh / PlantConstants.DC_CAPACITY_KW
        } else 0.0

        // Line Loss
        val lossKwh = plantGenKwh - ssGenKwh
        val lossPercentage = if (plantGenKwh > 0.0) {
            (lossKwh / plantGenKwh) * 100.0
        } else 0.0

        return GenerationCalculationResult(
            plantGenKwh = plantGenKwh,
            ssGenKwh = ssGenKwh,
            plantYield = plantYield,
            ssYield = ssYield,
            lossKwh = lossKwh,
            lossPercentage = lossPercentage,
            isValid = issues.isEmpty(),
            validationIssues = issues
        )
    }
}
