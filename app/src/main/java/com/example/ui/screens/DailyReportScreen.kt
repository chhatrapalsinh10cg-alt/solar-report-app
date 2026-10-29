package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlantConstants
import com.example.ui.components.CalculationMetricsCard
import com.example.ui.components.MeterPhotoUploadCard
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.GridEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.WarningCoral
import com.example.ui.viewmodel.MeterSection
import com.example.ui.viewmodel.SolarReportUiState
import com.example.ui.viewmodel.SolarReportViewModel
import java.util.Calendar

@Composable
fun DailyReportScreen(
    viewModel: SolarReportViewModel,
    uiState: SolarReportUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // DatePicker setup
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                viewModel.onDateSelected(pickedCal)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Duplicate overwrite confirmation dialog
    if (uiState.showDuplicateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SolarAmber
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Existing Record Found")
                }
            },
            text = {
                Text("A confirmed daily report already exists for ${uiState.displayDate}. Would you like to overwrite it with these updated readings?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmOverwriteAndSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                    modifier = Modifier.testTag("confirm_overwrite_button")
                ) {
                    Text("Overwrite & Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDuplicateDialog() },
                    modifier = Modifier.testTag("cancel_overwrite_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Plant Identity Header Card
            PlantHeaderCard(uiState = uiState, onPickDate = { datePickerDialog.show() })
        }

        // Section 1: Previous Readings Card (Auto-populated with warning if missing)
        item {
            PreviousReadingsSection(
                uiState = uiState,
                onPreviousReadingChanged = viewModel::onPreviousReadingChanged
            )
        }

        // Section 2: Plant End Uploads
        item {
            SectionHeader(
                title = "Plant End Meter Photos & Readings",
                subtitle = "Upload Today Import & Today Export meter photos"
            )
        }

        item {
            MeterPhotoUploadCard(
                title = "Plant End - Today Import",
                subtitle = "Take photo of Plant Import meter screen",
                icon = Icons.Default.ElectricMeter,
                slotState = uiState.plantTodayImport,
                testTagPrefix = "plant_today_import",
                onImageSelected = { uri -> viewModel.onImagePicked(MeterSection.PLANT_TODAY_IMPORT, uri, context) },
                onValueChanged = { text -> viewModel.onReadingManualChanged(MeterSection.PLANT_TODAY_IMPORT, text) },
                onCandidateSelected = { cand -> viewModel.onCandidateSelected(MeterSection.PLANT_TODAY_IMPORT, cand) }
            )
        }

        item {
            MeterPhotoUploadCard(
                title = "Plant End - Today Export",
                subtitle = "Take photo of Plant Export meter screen",
                icon = Icons.Default.SolarPower,
                slotState = uiState.plantTodayExport,
                testTagPrefix = "plant_today_export",
                onImageSelected = { uri -> viewModel.onImagePicked(MeterSection.PLANT_TODAY_EXPORT, uri, context) },
                onValueChanged = { text -> viewModel.onReadingManualChanged(MeterSection.PLANT_TODAY_EXPORT, text) },
                onCandidateSelected = { cand -> viewModel.onCandidateSelected(MeterSection.PLANT_TODAY_EXPORT, cand) }
            )
        }

        // Section 3: S/S End Uploads
        item {
            SectionHeader(
                title = "🧷 S/S End Meter Photos & Readings",
                subtitle = "Upload Today Export & Today Import meter photos"
            )
        }

        item {
            MeterPhotoUploadCard(
                title = "S/S End - Today Export",
                subtitle = "Take photo of Sub-Station Export meter screen",
                icon = Icons.Default.Power,
                slotState = uiState.ssTodayExport,
                testTagPrefix = "ss_today_export",
                onImageSelected = { uri -> viewModel.onImagePicked(MeterSection.SS_TODAY_EXPORT, uri, context) },
                onValueChanged = { text -> viewModel.onReadingManualChanged(MeterSection.SS_TODAY_EXPORT, text) },
                onCandidateSelected = { cand -> viewModel.onCandidateSelected(MeterSection.SS_TODAY_EXPORT, cand) }
            )
        }

        item {
            MeterPhotoUploadCard(
                title = "S/S End - Today Import",
                subtitle = "Take photo of Sub-Station Import meter screen",
                icon = Icons.Default.Speed,
                slotState = uiState.ssTodayImport,
                testTagPrefix = "ss_today_import",
                onImageSelected = { uri -> viewModel.onImagePicked(MeterSection.SS_TODAY_IMPORT, uri, context) },
                onValueChanged = { text -> viewModel.onReadingManualChanged(MeterSection.SS_TODAY_IMPORT, text) },
                onCandidateSelected = { cand -> viewModel.onCandidateSelected(MeterSection.SS_TODAY_IMPORT, cand) }
            )
        }

        // Section 4: Operational & Environmental Inputs
        item {
            EnvironmentalInputsCard(
                uiState = uiState,
                onWeatherChanged = viewModel::onWeatherChanged,
                onCurtailmentChanged = viewModel::onCurtailmentChanged,
                onReleaseChanged = viewModel::onReleaseChanged,
                onRemarksChanged = viewModel::onRemarksChanged
            )
        }

        // Section 5: Real-time Calculation & Yield
        item {
            CalculationMetricsCard(calculation = uiState.calculation)
        }

        // Section 6: Action Buttons
        item {
            ReportActionsCard(
                uiState = uiState,
                onPreview = { viewModel.showPreview(true) },
                onCopy = { viewModel.copyReportToClipboard(context) },
                onSave = { viewModel.checkAndSaveReport() },
                onShare = { viewModel.shareReportText(context) },
                onDownloadTxt = { viewModel.downloadAsTxt(context) },
                onDownloadPdf = { viewModel.downloadAsPdf(context) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlantHeaderCard(
    uiState: SolarReportUiState,
    onPickDate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plant_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SUKHPAR RUSHIKESH SOLAR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "DC Capacity: 5407.5 kW  •  AC Capacity: 4000 kW  •  14 Inverters",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SolarAmber)
                        .clickable { onPickDate() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .clickable { onPickDate() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reporting Date: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.displayDate,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Tap to Change",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SolarAmber
                )
            }
        }
    }
}

@Composable
private fun PreviousReadingsSection(
    uiState: SolarReportUiState,
    onPreviousReadingChanged: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("previous_readings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ElectricTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Historical Previous Readings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (uiState.previousReadingsSourceDate != null)
                            "Automatically populated from ${uiState.previousReadingsSourceDate}"
                        else "Editable historical baseline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Warning if historical data is unavailable
            AnimatedVisibility(visible = uiState.hasPreviousReadingsWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SolarAmber.copy(alpha = 0.15f))
                        .border(1.dp, SolarAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = SolarAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No historical record found for previous day. Please enter previous readings manually.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Previous Readings Fields (2x2 Grid)
            Text(
                text = "Plant End Previous",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SolarAmber
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.previousPlantImport,
                    onValueChange = { onPreviousReadingChanged("plantImport", it) },
                    label = { Text("Prev Plant Import") },
                    placeholder = { Text("12.450") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_plant_import_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.previousPlantExport,
                    onValueChange = { onPreviousReadingChanged("plantExport", it) },
                    label = { Text("Prev Plant Export") },
                    placeholder = { Text("1530.820") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_plant_export_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🧷 S/S End Previous",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ElectricTeal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.previousSSExport,
                    onValueChange = { onPreviousReadingChanged("ssExport", it) },
                    label = { Text("Prev S/S Export") },
                    placeholder = { Text("15.680") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_ss_export_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.previousSSImport,
                    onValueChange = { onPreviousReadingChanged("ssImport", it) },
                    label = { Text("Prev S/S Import") },
                    placeholder = { Text("1502.340") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_ss_import_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun EnvironmentalInputsCard(
    uiState: SolarReportUiState,
    onWeatherChanged: (String) -> Unit,
    onCurtailmentChanged: (String, String) -> Unit,
    onReleaseChanged: (String, String) -> Unit,
    onRemarksChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("environmental_inputs_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weather & Curtailment Log",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Operational constraints and sky condition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Weather chips
            Text(
                text = "Weather Condition",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlantConstants.WEATHER_OPTIONS.forEach { option ->
                    val isSelected = option == uiState.weather
                    FilterChip(
                        selected = isSelected,
                        onClick = { onWeatherChanged(option) },
                        label = { Text(option, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarAmber,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Curtailment Inputs
            Text(
                text = "📍 Curtailment (cuirtament)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.curtailmentPercentage,
                    onValueChange = { onCurtailmentChanged(it, uiState.curtailmentTime) },
                    label = { Text("Percentage %") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.curtailmentTime,
                    onValueChange = { onCurtailmentChanged(uiState.curtailmentPercentage, it) },
                    label = { Text("Time") },
                    placeholder = { Text("None or 11:30 to 13:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Release Inputs
            Text(
                text = "📍 Realise / Release",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.releasePercentage,
                    onValueChange = { onReleaseChanged(it, uiState.releaseTime) },
                    label = { Text("Percentage %") },
                    placeholder = { Text("100") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = uiState.releaseTime,
                    onValueChange = { onReleaseChanged(uiState.releasePercentage, it) },
                    label = { Text("Time") },
                    placeholder = { Text("Full day or 13:00") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remarks field
            OutlinedTextField(
                value = uiState.remarks,
                onValueChange = onRemarksChanged,
                label = { Text("Remarks (Optional)") },
                placeholder = { Text("e.g. Inverter 4 under inspection") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun ReportActionsCard(
    uiState: SolarReportUiState,
    onPreview: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDownloadTxt: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_actions_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Report Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Preview, copy directly to WhatsApp, or save to database",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1: Copy Report & Preview Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("copy_report_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GridEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy Report",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onPreview,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("preview_report_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preview Report",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Save Daily Readings
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_readings_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Daily Readings",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Secondary actions (Share, Download TXT, Download PDF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("share_report_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDownloadPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("download_pdf_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDownloadTxt,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("download_txt_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TXT", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
