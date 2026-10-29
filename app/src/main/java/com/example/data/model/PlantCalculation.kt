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
    val plantGenKwh: Double = 0.0,
    val ssGenKwh: Double = 0.0,
    val plantYield: Double = 0.0,
    val ssYield: Double = 0.0,
    val lossKwh: Double = 0.0,
    val lossPercentage: Double = 0.0,
    val isValid: Boolean = true,
    val validationIssues: List<String> = emptyList()
)

// Formatting Helpers
fun formatMeterValue(value: Double?): String = String.format(Locale.US, "%.2f", value ?: 0.0)
fun formatYield(value: Double?): String = String.format(Locale.US, "%.2f", value ?: 0.0)
fun formatLoss(value: Double?): String = String.format(Locale.US, "%.2f", value ?: 0.0)
fun formatGeneration(value: Double?): String = String.format(Locale.US, "%.0f", value ?: 0.0)

// Report Format Functions
fun buildExactReportText(
    date: String = "",
    todayPlantExp: Double = 0.0,
    prevPlantExp: Double = 0.0,
    todayPlantImp: Double = 0.0,
    prevPlantImp: Double = 0.0,
    todaySSExp: Double = 0.0,
    prevSSExp: Double = 0.0,
    todaySSImp: Double = 0.0,
    prevSSImp: Double = 0.0,
    plantGenKwh: Double = 0.0,
    ssGenKwh: Double = 0.0,
    plantYield: Double = 0.0,
    ssYield: Double = 0.0,
    lossPercentage: Double = 0.0,
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

Total:${formatGeneration(plantGenKwh)}kWh
Unit/kw:${formatYield(plantYield)}kWh/kW

*🧷 S/S End*
Today export Unit :${formatMeterValue(todaySSExp)}
Today import unit :${formatMeterValue(todaySSImp)}
Previous export unit :${formatMeterValue(prevSSExp)}
Previous import unit :${formatMeterValue(prevSSImp)}

Total:${formatGeneration(ssGenKwh)}kWh
Unit/kW:${formatYield(ssYield)}kWh/kW

*Loss:${formatLoss(lossPercentage)}%*
$weather
    """.trimIndent()
}

fun buildExactReportText(
    todayPlantExp: Double = 0.0,
    prevPlantExp: Double = 0.0,
    todayPlantImp: Double = 0.0,
    prevPlantImp: Double = 0.0,
    todaySSExp: Double = 0.0,
    prevSSExp: Double = 0.0,
    todaySSImp: Double = 0.0,
    prevSSImp: Double = 0.0,
    plantGenKwh: Double = 0.0,
    ssGenKwh: Double = 0.0,
    plantYield: Double = 0.0,
    ssYield: Double = 0.0,
    lossPercentage: Double = 0.0,
    date: String = "",
    weather: String = "Cloudy whether 🌤️☀️"
): String = buildExactReportText(
    date = date,
    todayPlantExp = todayPlantExp,
    prevPlantExp = prevPlantExp,
    todayPlantImp = todayPlantImp,
    prevPlantImp = prevPlantImp,
    todaySSExp = todaySSExp,
    prevSSExp = prevSSExp,
    todaySSImp = todaySSImp,
    prevSSImp = prevSSImp,
    plantGenKwh = plantGenKwh,
    ssGenKwh = ssGenKwh,
    plantYield = plantYield,
    ssYield = ssYield,
    lossPercentage = lossPercentage,
    weather = weather
)

fun buildShareReportText(
    date: String = "",
    todayPlantExp: Double = 0.0,
    prevPlantExp: Double = 0.0,
    todayPlantImp: Double = 0.0,
    prevPlantImp: Double = 0.0,
    todaySSExp: Double = 0.0,
    prevSSExp: Double = 0.0,
    todaySSImp: Double = 0.0,
    prevSSImp: Double = 0.0,
    plantGenKwh: Double = 0.0,
    ssGenKwh: Double = 0.0,
    plantYield: Double = 0.0,
    ssYield: Double = 0.0,
    lossPercentage: Double = 0.0,
    weather: String = "Cloudy whether 🌤️☀️"
): String = buildExactReportText(
    date = date,
    todayPlantExp = todayPlantExp,
    prevPlantExp = prevPlantExp,
    todayPlantImp = todayPlantImp,
    prevPlantImp = prevPlantImp,
    todaySSExp = todaySSExp,
    prevSSExp = prevSSExp,
    todaySSImp = todaySSImp,
    prevSSImp = prevSSImp,
    plantGenKwh = plantGenKwh,
    ssGenKwh = ssGenKwh,
    plantYield = plantYield,
    ssYield = ssYield,
    lossPercentage = lossPercentage,
    weather = weather
)

object PlantCalculationEngine {

    fun formatMeterValue(value: Double?): String = com.example.data.model.formatMeterValue(value)
    fun formatYield(value: Double?): String = com.example.data.model.formatYield(value)
    fun formatLoss(value: Double?): String = com.example.data.model.formatLoss(value)
    fun formatGeneration(value: Double?): String = com.example.data.model.formatGeneration(value)

    fun buildExactReportText(
        date: String = "",
        todayPlantExp: Double = 0.0,
        prevPlantExp: Double = 0.0,
        todayPlantImp: Double = 0.0,
        prevPlantImp: Double = 0.0,
        todaySSExp: Double = 0.0,
        prevSSExp: Double = 0.0,
        todaySSImp: Double = 0.0,
        prevSSImp: Double = 0.0,
        plantGenKwh: Double = 0.0,
        ssGenKwh: Double = 0.0,
        plantYield: Double = 0.0,
        ssYield: Double = 0.0,
        lossPercentage: Double = 0.0,
        weather: String = "Cloudy whether 🌤️☀️"
    ): String = com.example.data.model.buildExactReportText(
        date = date,
        todayPlantExp = todayPlantExp,
        prevPlantExp = prevPlantExp,
        todayPlantImp = todayPlantImp,
        prevPlantImp = prevPlantImp,
        todaySSExp = todaySSExp,
        prevSSExp = prevSSExp,
        todaySSImp = todaySSImp,
        prevSSImp = prevSSImp,
        plantGenKwh = plantGenKwh,
        ssGenKwh = ssGenKwh,
        plantYield = plantYield,
        ssYield = ssYield,
        lossPercentage = lossPercentage,
        weather = weather
    )

    fun buildExactReportText(
        todayPlantExp: Double = 0.0,
        prevPlantExp: Double = 0.0,
        todayPlantImp: Double = 0.0,
        prevPlantImp: Double = 0.0,
        todaySSExp: Double = 0.0,
        prevSSExp: Double = 0.0,
        todaySSImp: Double = 0.0,
        prevSSImp: Double = 0.0,
        plantGenKwh: Double = 0.0,
        ssGenKwh: Double = 0.0,
        plantYield: Double = 0.0,
        ssYield: Double = 0.0,
        lossPercentage: Double = 0.0,
        date: String = "",
        weather: String = "Cloudy whether 🌤️☀️"
    ): String = com.example.data.model.buildExactReportText(
        date = date,
        todayPlantExp = todayPlantExp,
        prevPlantExp = prevPlantExp,
        todayPlantImp = todayPlantImp,
        prevPlantImp = prevPlantImp,
        todaySSExp = todaySSExp,
        prevSSExp = prevSSExp,
        todaySSImp = todaySSImp,
        prevSSImp = prevSSImp,
        plantGenKwh = plantGenKwh,
        ssGenKwh = ssGenKwh,
        plantYield = plantYield,
        ssYield = ssYield,
        lossPercentage = lossPercentage,
        weather = weather
    )

    fun buildShareReportText(
        date: String = "",
        todayPlantExp: Double = 0.0,
        prevPlantExp: Double = 0.0,
        todayPlantImp: Double = 0.0,
        prevPlantImp: Double = 0.0,
        todaySSExp: Double = 0.0,
        prevSSExp: Double = 0.0,
        todaySSImp: Double = 0.0,
        prevSSImp: Double = 0.0,
        plantGenKwh: Double = 0.0,
        ssGenKwh: Double = 0.0,
        plantYield: Double = 0.0,
        ssYield: Double = 0.0,
        lossPercentage: Double = 0.0,
        weather: String = "Cloudy whether 🌤️☀️"
    ): String = com.example.data.model.buildExactReportText(
        date = date,
        todayPlantExp = todayPlantExp,
        prevPlantExp = prevPlantExp,
        todayPlantImp = todayPlantImp,
        prevPlantImp = prevPlantImp,
        todaySSExp = todaySSExp,
        prevSSExp = prevSSExp,
        todaySSImp = todaySSImp,
        prevSSImp = prevSSImp,
        plantGenKwh = plantGenKwh,
        ssGenKwh = ssGenKwh,
        plantYield = plantYield,
        ssYield = ssYield,
        lossPercentage = lossPercentage,
        weather = weather
    )

    fun calculate(
        todayPlantImport: Double? = 0.0,
        todayPlantExport: Double?,
        previousPlantImport: Double? = 0.0,
        previousPlantExport: Double?,
        todaySSExport: Double? = 0.0,
        todaySSImport: Double?,
        previousSSExport: Double? = 0.0,
        previousSSImport: Double?,
        isMwh: Boolean = true,
        isMwhUnit: Boolean = true,
        curtailmentPercentage: Double? = 0.0,
        curtailmentTime: Double? = 0.0,
        relaxWeatherUsage: Double? = 0.0,
        relaxWeatherTime: Double? = 0.0
    ): GenerationCalculationResult {
        val issues = mutableListOf<String>()

        if (todayPlantExport == null || previousPlantExport == null) {
            issues.add("Plant End export readings are incomplete.")
        }
        if (todaySSImport == null || previousSSImport == null) {
            issues.add("S/S End import readings are incomplete.")
        }

        val tPlantExp = todayPlantExport ?: 0.0
        val pPlantExp = previousPlantExport ?: 0.0
        val tSSImp = todaySSImport ?: 0.0
        val pSSImp = previousSSImport ?: 0.0

        // Multiplier: MF 5000 Fix
        val multiplier = 5000.0

        // 1. Plant Gen kWh = (Today Export - Prev Export) * 5000
        val plantGenKwh = (tPlantExp - pPlantExp) * multiplier

        // 2. S/S Gen kWh = (Today Import - Prev Import) * 5000
        val ssGenKwh = (tSSImp - pSSImp) * multiplier

        // 3. Plant DC Yield = Plant Gen / 5407.5
        val plantYield = if (PlantConstants.DC_CAPACITY_KW > 0.0 && plantGenKwh > 0.0) {
            plantGenKwh / PlantConstants.DC_CAPACITY_KW
        } else 0.0

        // 4. S/S DC Yield = S/S Gen / 5407.5
        val ssYield = if (PlantConstants.DC_CAPACITY_KW > 0.0 && ssGenKwh > 0.0) {
            ssGenKwh / PlantConstants.DC_CAPACITY_KW
        } else 0.0

        // 5. Line Loss
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
