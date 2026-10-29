package com.example.data.model

import java.util.Locale

object PlantConstants {
    const val PLANT_NAME = "SUKHPAR RUSHIKESH SOLAR"
    const val DC_CAPACITY_KW = 5407.5
    const val AC_CAPACITY_KW = 4000.0
    const val INVERTER_COUNT = 14
    const val DEFAULT_MF = 5000.0
}

data class GenerationCalculationResult(
    val plantGenKwh: Double,
    val ssGenKwh: Double,
    val plantYield: Double,
    val ssYield: Double,
    val lossKwh: Double,
    val lossPercentage: Double,
    val isValid: Boolean,
    val formattedReport: String = "",
    val validationIssues: List<String> = emptyList()
)

object PlantCalculationEngine {

    fun calculate(
        todayPlantExport: Double?,
        previousPlantExport: Double?,
        todayPlantImport: Double? = 0.0,
        previousPlantImport: Double? = 0.0,
        todaySSImport: Double?,
        previousSSImport: Double?,
        todaySSExport: Double? = 0.0,
        previousSSExport: Double? = 0.0,
        selectedDate: String = "29/10/2026",
        weatherCondition: String = "Cloudy whether 🌤️☀️",
        multiplier: Double = PlantConstants.DEFAULT_MF
    ): GenerationCalculationResult {
        val issues = mutableListOf<String>()

        if (todayPlantExport == null || previousPlantExport == null) {
            issues.add("Plant End export readings are missing.")
        }
        if (todaySSImport == null || previousSSImport == null) {
            issues.add("S/S End import readings are missing.")
        }

        if (issues.isNotEmpty()) {
            return GenerationCalculationResult(
                plantGenKwh = 0.0,
                ssGenKwh = 0.0,
                plantYield = 0.0,
                ssYield = 0.0,
                lossKwh = 0.0,
                lossPercentage = 0.0,
                isValid = false,
                validationIssues = issues
            )
        }

        val tPlantExp = todayPlantExport ?: 0.0
        val pPlantExp = previousPlantExport ?: 0.0
        val tPlantImp = todayPlantImport ?: 0.0
        val pPlantImp = previousPlantImport ?: 0.0

        val tSSImp = todaySSImport ?: 0.0
        val pSSImp = previousSSImport ?: 0.0
        val tSSExp = todaySSExport ?: 0.0
        val pSSExp = previousSSExport ?: 0.0

        // 1. Plant End Total Gen = (Today Export - Previous Export) * MF
        val plantGenKwh = (tPlantExp - pPlantExp) * multiplier

        // 2. S/S End Total Gen = (Today Import - Previous Import) * MF
        val ssGenKwh = (tSSImp - pSSImp) * multiplier

        // 3. DC Yield (kWh / kW)
        val plantYield = if (plantGenKwh > 0) plantGenKwh / PlantConstants.DC_CAPACITY_KW else 0.0
        val ssYield = if (ssGenKwh > 0) ssGenKwh / PlantConstants.DC_CAPACITY_KW else 0.0

        // 4. Line Loss
        val lossKwh = plantGenKwh - ssGenKwh
        val lossPercentage = if (plantGenKwh > 0) (lossKwh / plantGenKwh) * 100.0 else 0.0

        // Generate exact WhatsApp / Text Report format
        val report = """
*sukhpar rushikesh solar ${PlantConstants.DC_CAPACITY_KW} KW dc*

Date : $selectedDate

*Plant End*
Today import Unit :${String.format(Locale.US, "%.2f", tPlantImp)}
Today export unit :${String.format(Locale.US, "%.2f", tPlantExp)}
Previous import unit :${String.format(Locale.US, "%.2f", pPlantImp)}
Previous export unit :${String.format(Locale.US, "%.2f", pPlantExp)}

Total:${String.format(Locale.US, "%.0f", plantGenKwh)}kWh
Unit/kw:${String.format(Locale.US, "%.2f", plantYield)}kWh/kW

*🧷 S/S End*
Today export Unit :${String.format(Locale.US, "%.2f", tSSExp)}
Today import unit :${String.format(Locale.US, "%.2f", tSSImp)}
Previous export unit :${String.format(Locale.US, "%.2f", pSSExp)}
Previous import unit :${String.format(Locale.US, "%.2f", pSSImp)}

Total:${String.format(Locale.US, "%.0f", ssGenKwh)}kWh
Unit/kW:${String.format(Locale.US, "%.2f", ssYield)}kWh/kW

*Loss:${String.format(Locale.US, "%.2f", lossPercentage)}%*
$weatherCondition
        """.trimIndent()

        return GenerationCalculationResult(
            plantGenKwh = plantGenKwh,
            ssGenKwh = ssGenKwh,
            plantYield = plantYield,
            ssYield = ssYield,
            lossKwh = lossKwh,
            lossPercentage = lossPercentage,
            isValid = true,
            formattedReport = report
        )
    }
}
