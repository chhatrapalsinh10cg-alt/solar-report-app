package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DailyReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReportPreviewDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SolarAmber
import com.example.ui.viewmodel.SolarReportViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SolarReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SolarAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SolarAppContent(viewModel: SolarReportViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Collect one-off events (e.g. snackbars)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle Android system Back button: if on sub-tab, return to tab 0
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.SolarPower, contentDescription = "Daily Report") },
                    label = { Text("Daily Report") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SolarAmber,
                        selectedTextColor = SolarAmber,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_daily_report")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SolarAmber,
                        selectedTextColor = SolarAmber,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Plant & Specs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SolarAmber,
                        selectedTextColor = SolarAmber,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DailyReportScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                1 -> HistoryScreen(
                    records = historyRecords,
                    onSelectRecord = { record ->
                        viewModel.populateFromRecord(record)
                        selectedTab = 0
                    },
                    onDeleteRecord = { id -> viewModel.deleteHistoricalRecord(id) },
                    onCopyRecord = { record ->
                        viewModel.populateFromRecord(record)
                        viewModel.copyReportToClipboard(context)
                    }
                )
                2 -> SettingsScreen(
                    isMWhUnit = uiState.isMWhUnit,
                    onToggleUnit = viewModel::onCalculationModeToggled,
                    onRestoreSampleData = viewModel::resetSampleData,
                    recordCount = historyRecords.size
                )
            }

            // Report Preview Dialog (exact WhatsApp format)
            if (uiState.showPreviewDialog) {
                ReportPreviewDialog(
                    reportText = viewModel.getFormattedReportText(),
                    onDismiss = { viewModel.showPreview(false) },
                    onCopy = { viewModel.copyReportToClipboard(context) },
                    onShare = { viewModel.shareReportText(context) },
                    onDownloadTxt = { viewModel.downloadAsTxt(context) },
                    onDownloadPdf = { viewModel.downloadAsPdf(context) }
                )
            }
        }
    }
}
