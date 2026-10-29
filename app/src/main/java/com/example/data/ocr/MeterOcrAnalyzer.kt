package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class MeterOcrResult(
    val isClear: Boolean,
    val primaryReading: String?,
    val candidateReadings: List<String>,
    val rawExtractedText: String,
    val statusMessage: String
)

object MeterOcrAnalyzer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Matches numbers with decimals (e.g. 1555.750, 12.68, 0450.12)
    private val DECIMAL_NUMBER_PATTERN = Pattern.compile("""\b\d{1,7}[.,]\d{1,4}\b""")
    
    // Matches pure numbers of typical meter reading length (4 to 8 digits)
    private val INTEGER_METER_PATTERN = Pattern.compile("""\b\d{4,8}\b""")

    // Excluded patterns (Dates, Times, Standard technical ratings)
    private val DATE_PATTERN = Pattern.compile("""\b\d{1,4}[/\-.]\d{1,2}[/\-.]\d{2,4}\b""")
    private val TIME_PATTERN = Pattern.compile("""\b\d{1,2}:\d{2}(:\d{2})?\b""")
    private val RATING_PATTERN = Pattern.compile("""(?i)\b(11kv|33kv|66kv|415v|50hz|class|type|s\/n|sr|no)\b""")

    suspend fun analyzeImage(context: Context, imageUri: Uri): MeterOcrResult {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            processInputImage(image)
        } catch (e: Exception) {
            MeterOcrResult(
                isClear = false,
                primaryReading = null,
                candidateReadings = emptyList(),
                rawExtractedText = "",
                statusMessage = "Error loading image: ${e.localizedMessage ?: "Unknown error"}. Please enter reading manually."
            )
        }
    }

    suspend fun analyzeBitmap(bitmap: Bitmap): MeterOcrResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            processInputImage(image)
        } catch (e: Exception) {
            MeterOcrResult(
                isClear = false,
                primaryReading = null,
                candidateReadings = emptyList(),
                rawExtractedText = "",
                statusMessage = "Error processing photo: ${e.localizedMessage ?: "Unknown error"}."
            )
        }
    }

    private suspend fun processInputImage(image: InputImage): MeterOcrResult =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val candidates = mutableSetOf<String>()
                    val decimalCandidates = mutableListOf<String>()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            
                            // Skip date or time lines
                            if (DATE_PATTERN.matcher(lineText).find() || TIME_PATTERN.matcher(lineText).find()) {
                                continue
                            }
                            if (RATING_PATTERN.matcher(lineText).find()) {
                                continue
                            }

                            // Clean common OCR anomalies for numbers: 'O' or 'o' to '0', 'l'/'I' to '1'
                            val cleaned = lineText
                                .replace('O', '0')
                                .replace('o', '0')
                                .replace(',', '.')

                            // Check decimal patterns first (most common for MWh/kWh energy meters)
                            val decMatcher = DECIMAL_NUMBER_PATTERN.matcher(cleaned)
                            while (decMatcher.find()) {
                                val match = decMatcher.group().replace(',', '.')
                                decimalCandidates.add(match)
                                candidates.add(match)
                            }

                            // Also look for integer meter numbers if no decimals
                            val intMatcher = INTEGER_METER_PATTERN.matcher(cleaned)
                            while (intMatcher.find()) {
                                val match = intMatcher.group()
                                candidates.add(match)
                            }
                        }
                    }

                    // Score candidates: prioritize decimal numbers, especially those with 2 or 3 decimals
                    val sortedCandidates = candidates.toList().sortedWith(
                        compareByDescending<String> { it.contains(".") }
                            .thenByDescending { it.length }
                    )

                    if (sortedCandidates.isNotEmpty()) {
                        val best = sortedCandidates.first()
                        continuation.resume(
                            MeterOcrResult(
                                isClear = true,
                                primaryReading = best,
                                candidateReadings = sortedCandidates,
                                rawExtractedText = fullText,
                                statusMessage = "Reading identified: $best. Please verify or adjust if needed."
                            )
                        )
                    } else if (fullText.isNotBlank()) {
                        continuation.resume(
                            MeterOcrResult(
                                isClear = false,
                                primaryReading = null,
                                candidateReadings = emptyList(),
                                rawExtractedText = fullText,
                                statusMessage = "Meter reading could not be clearly identified. Please retake photo or enter manually."
                            )
                        )
                    } else {
                        continuation.resume(
                            MeterOcrResult(
                                isClear = false,
                                primaryReading = null,
                                candidateReadings = emptyList(),
                                rawExtractedText = "",
                                statusMessage = "No text or meter digits detected. Please retake clear photo of the meter display."
                            )
                        )
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resume(
                        MeterOcrResult(
                            isClear = false,
                            primaryReading = null,
                            candidateReadings = emptyList(),
                            rawExtractedText = "",
                            statusMessage = "OCR scan failed: ${error.localizedMessage}. Please enter manually."
                        )
                    )
                }
        }
}
