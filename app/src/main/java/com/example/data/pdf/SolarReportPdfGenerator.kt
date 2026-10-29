package com.example.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.DailyReadingRecord
import com.example.data.model.PlantCalculationEngine
import com.example.data.model.PlantConstants
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object SolarReportPdfGenerator {

    fun generatePdf(context: Context, record: DailyReadingRecord): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Dark Slate
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(217, 119, 6) // Amber
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(2, 132, 199) // Electric Teal
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11f
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }

        var y = 45f

        // Title Header
        canvas.drawText("SUKHPAR RUSHIKESH SOLAR", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Daily Generation & Meter Reading Report", 40f, y, subtitlePaint)
        y += 18f
        canvas.drawText("Plant Capacity: 5407.5 kW DC | 4000 kW AC | 14 Inverters", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("Report Date: ${record.displayDate}", 40f, y, boldBodyPaint)
        
        y += 15f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 25f

        // Key Metrics Summary Box
        canvas.drawRoundRect(40f, y, 555f, y + 100f, 8f, 8f, cardBgPaint)
        val boxY = y + 25f
        
        // Col 1
        canvas.drawText("Plant Generation:", 55f, boxY, bodyPaint)
        canvas.drawText("${PlantCalculationEngine.formatGeneration(record.plantGenerationKwh)} kWh", 55f, boxY + 20f, boldBodyPaint)
        canvas.drawText("Plant Yield: ${PlantCalculationEngine.formatYield(record.plantYield)} kWh/kW", 55f, boxY + 45f, bodyPaint)

        // Col 2
        canvas.drawText("S/S Generation:", 225f, boxY, bodyPaint)
        canvas.drawText("${PlantCalculationEngine.formatGeneration(record.ssGenerationKwh)} kWh", 225f, boxY + 20f, boldBodyPaint)
        canvas.drawText("S/S Yield: ${PlantCalculationEngine.formatYield(record.ssYield)} kWh/kW", 225f, boxY + 45f, bodyPaint)

        // Col 3
        canvas.drawText("Loss Analysis:", 395f, boxY, bodyPaint)
        val lossColor = if (record.lossPercentage > 5.0) Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
        val lossPaint = Paint().apply {
            color = lossColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${PlantCalculationEngine.formatLoss(record.lossPercentage)}% (${PlantCalculationEngine.formatGeneration(record.lossKwh)} kWh)", 395f, boxY + 20f, lossPaint)
        canvas.drawText("Weather: ${record.weather}", 395f, boxY + 45f, bodyPaint)

        y += 125f

        // Section 1: Plant End Readings
        canvas.drawText("1. Plant End Meter Readings", 40f, y, headerPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Metric", "Reading (MWh / Units)", boldBodyPaint, boldBodyPaint)
        y += 16f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Today Import Unit", PlantCalculationEngine.formatMeterValue(record.todayPlantImport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Today Export Unit", PlantCalculationEngine.formatMeterValue(record.todayPlantExport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Previous Import Unit", PlantCalculationEngine.formatMeterValue(record.previousPlantImport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Previous Export Unit", PlantCalculationEngine.formatMeterValue(record.previousPlantExport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Plant Total Generation", "${PlantCalculationEngine.formatGeneration(record.plantGenerationKwh)} kWh", boldBodyPaint, subtitlePaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Plant DC Yield", "${PlantCalculationEngine.formatYield(record.plantYield)} kWh/kW", bodyPaint, boldBodyPaint)

        y += 35f

        // Section 2: S/S End Readings
        canvas.drawText("2. S/S End Meter Readings", 40f, y, headerPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Metric", "Reading (MWh / Units)", boldBodyPaint, boldBodyPaint)
        y += 16f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Today Export Unit", PlantCalculationEngine.formatMeterValue(record.todaySSExport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Today Import Unit", PlantCalculationEngine.formatMeterValue(record.todaySSImport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Previous Export Unit", PlantCalculationEngine.formatMeterValue(record.previousSSExport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Previous Import Unit", PlantCalculationEngine.formatMeterValue(record.previousSSImport), bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "S/S Total Generation", "${PlantCalculationEngine.formatGeneration(record.ssGenerationKwh)} kWh", boldBodyPaint, subtitlePaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "S/S DC Yield", "${PlantCalculationEngine.formatYield(record.ssYield)} kWh/kW", bodyPaint, boldBodyPaint)

        y += 35f

        // Section 3: Curtailment & Operational Release
        canvas.drawText("3. Curtailment & Release Log", 40f, y, headerPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Curtailment", "${record.curtailmentPercentage}% (Time: ${record.curtailmentTime})", bodyPaint, boldBodyPaint)
        y += 18f
        drawTableRow(canvas, 40f, y, "Release", "${record.releasePercentage}% (Time: ${record.releaseTime})", bodyPaint, boldBodyPaint)
        if (record.remarks.isNotBlank()) {
            y += 18f
            drawTableRow(canvas, 40f, y, "Remarks", record.remarks, bodyPaint, boldBodyPaint)
        }

        y += 40f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("Generated by Sukhpar Rushikesh Solar Daily Reporter", 40f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save to cache dir
        val safeDate = record.date.replace("-", "_")
        val file = File(context.cacheDir, "Solar_Report_${safeDate}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawTableRow(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawText(label, x + 10f, y, labelPaint)
        canvas.drawText(value, x + 300f, y, valuePaint)
    }

    fun saveReportAsTxt(context: Context, record: DailyReadingRecord, reportText: String): File {
        val safeDate = record.date.replace("-", "_")
        val file = File(context.cacheDir, "Solar_Report_${safeDate}.txt")
        file.writeText(reportText)
        return file
    }
}
