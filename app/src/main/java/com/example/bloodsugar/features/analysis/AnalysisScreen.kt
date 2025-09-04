package com.example.bloodsugar.features.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import com.example.bloodsugar.R

@Composable
fun AnalysisScreen(analysisViewModel: AnalysisViewModel = viewModel()) {
    val uiState by analysisViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { analysisViewModel.updateAnalysisPeriod(7) }, enabled = uiState.selectedPeriod != 7) {
                Text(stringResource(id = R.string.analysis_7_days))
            }
            Button(onClick = { analysisViewModel.updateAnalysisPeriod(30) }, enabled = uiState.selectedPeriod != 30) {
                Text(stringResource(id = R.string.analysis_30_days))
            }
            Button(onClick = { analysisViewModel.updateAnalysisPeriod(90) }, enabled = uiState.selectedPeriod != 90) {
                Text(stringResource(id = R.string.analysis_90_days))
            }
        }

        TirCard(uiState = uiState)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(id = R.string.analysis_daily_insulin_intake), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                InsulinBarChart(dailyDoses = uiState.dailyInsulin)
            }
        }
    }
}

@Composable
fun TirCard(uiState: AnalysisUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(id = R.string.analysis_distribution), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(4.dp))

            val totalPercentage = uiState.veryHigh + uiState.high + uiState.timeInRange + uiState.low + uiState.veryLow

            if (totalPercentage > 0) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(MaterialTheme.shapes.medium)
                ) {
                    if (uiState.veryLow > 0) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .weight(uiState.veryLow)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                        )
                    }
                    if (uiState.low > 0) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .weight(uiState.low)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        )
                    }
                    if (uiState.timeInRange > 0) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .weight(uiState.timeInRange)
                            .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (uiState.high > 0) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .weight(uiState.high)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        )
                    }
                    if (uiState.veryHigh > 0) {
                        Box(modifier = Modifier
                            .fillMaxHeight()
                            .weight(uiState.veryHigh)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        )
                    }
                }
            } else {
                Text(stringResource(id = R.string.analysis_no_data), modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display percentages below the bar
            TirPercentageRow(stringResource(id = R.string.analysis_very_high), uiState.veryHigh, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            TirPercentageRow(stringResource(id = R.string.analysis_high), uiState.high, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            TirPercentageRow(stringResource(id = R.string.analysis_in_range), uiState.timeInRange, MaterialTheme.colorScheme.primary)
            TirPercentageRow(stringResource(id = R.string.analysis_low), uiState.low, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            TirPercentageRow(stringResource(id = R.string.analysis_very_low), uiState.veryLow, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun TirPercentageRow(label: String, percentage: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
        Text("%.1f%%".format(percentage), style = MaterialTheme.typography.bodyLarge, color = color)
    }
}