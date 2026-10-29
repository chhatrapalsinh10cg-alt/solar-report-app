package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlantCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Solar Report", appName)
    }

    @Test
    fun testSolarPlantCalculationFormulas() {
        // Sample readings in MWh
        val todayPlantImport = 12.680
        val todayPlantExport = 1555.750
        val prevPlantImport = 12.450
        val prevPlantExport = 1530.820

        val todaySSExport = 15.940
        val todaySSImport = 1526.850
        val prevSSExport = 15.680
        val prevSSImport = 1502.340

        val result = PlantCalculationEngine.calculate(
            todayPlantImport = todayPlantImport,
            todayPlantExport = todayPlantExport,
            previousPlantImport = prevPlantImport,
            previousPlantExport = prevPlantExport,
            todaySSExport = todaySSExport,
            todaySSImport = todaySSImport,
            previousSSExport = prevSSExport,
            previousSSImport = prevSSImport,
            isMWhUnit = true
        )

        // Plant Generation kWh = (1555.750 - 1530.820 + 12.450 - 12.680) * 1000
        // = (24.930 - 0.230) * 1000 = 24.700 * 1000 = 24700.0 kWh
        assertEquals(24700.0, result.plantGenKwh, 0.01)

        // S/S Generation kWh = (1526.850 - 1502.340 + 15.680 - 15.940) * 1000
        // = (24.510 - 0.260) * 1000 = 24.250 * 1000 = 24250.0 kWh
        assertEquals(24250.0, result.ssGenKwh, 0.01)

        // Plant DC Yield = 24700.0 / 5407.5 = 4.5677...
        assertEquals(4.5677, result.plantYield, 0.01)

        // Loss kWh = 24700.0 - 24250.0 = 450.0 kWh
        assertEquals(450.0, result.lossKwh, 0.01)

        // Loss Percentage = (450.0 / 24700.0) * 100 = 1.8218...%
        assertEquals(1.82, result.lossPercentage, 0.02)
        assertTrue(result.isValid)
    }

    @Test
    fun testExactWhatsAppReportFormat() {
        val report = PlantCalculationEngine.buildExactReportText(
            displayDate = "29/10/2026",
            todayPlantImport = "12.680",
            todayPlantExport = "1555.750",
            previousPlantImport = "12.450",
            previousPlantExport = "1530.820",
            plantGeneration = "24700.00",
            plantYield = "4.57",
            todaySSExport = "15.940",
            todaySSImport = "1526.850",
            previousSSExport = "15.680",
            previousSSImport = "1502.340",
            ssGeneration = "24250.00",
            ssYield = "4.48",
            lossPercentage = "1.82",
            weather = "Sunny",
            curtailmentPercentage = "0",
            curtailmentTime = "None",
            releasePercentage = "100",
            releaseTime = "Full day"
        )

        assertTrue(report.startsWith("sukhpar rushikesh solar 5407.5 KW dc"))
        assertTrue(report.contains("Date : 29/10/2026"))
        assertTrue(report.contains("Plant End"))
        assertTrue(report.contains("Today import Unit : 12.680"))
        assertTrue(report.contains("Today export unit : 1555.750"))
        assertTrue(report.contains("Previous import unit : 12.450"))
        assertTrue(report.contains("Previous export unit : 1530.820"))
        assertTrue(report.contains("Total:24700.00kWh"))
        assertTrue(report.contains("Unit/kw:4.57kWh/kW"))
        assertTrue(report.contains("🧷 S/S End"))
        assertTrue(report.contains("Today export Unit : 15.940"))
        assertTrue(report.contains("Today import unit : 1526.850"))
        assertTrue(report.contains("Previous export unit : 15.680"))
        assertTrue(report.contains("Previous import unit : 1502.340"))
        assertTrue(report.contains("Total:24250.00kWh"))
        assertTrue(report.contains("Unit/kW:4.48kWh/kW"))
        assertTrue(report.contains("Loss:1.82%"))
        assertTrue(report.contains("Sunny"))
        assertTrue(report.contains("📍 cuirtament0% time None"))
        assertTrue(report.contains("📍Realise100% time Full day"))
    }
}
