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
                Text("7 Days")
            }
            Button(onClick = { analysisViewModel.updateAnalysisPeriod(30) }, enabled = uiState.selectedPeriod != 30) {
                Text("30 Days")
            }
            Button(onClick = { analysisViewModel.updateAnalysisPeriod(90) }, enabled = uiState.selectedPeriod != 90) {
                Text("90 Days")
            }
        }

        TirCard(uiState = uiState)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Daily Insulin Intake", style = MaterialTheme.typography.titleMedium)
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
            Text("Distribution", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
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
                Text("Log at least two blood sugar readings to see your Time in Range analysis.", modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display percentages below the bar
            TirPercentageRow("Very High", uiState.veryHigh, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            TirPercentageRow("High", uiState.high, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            TirPercentageRow("In Range", uiState.timeInRange, MaterialTheme.colorScheme.primary)
            TirPercentageRow("Low", uiState.low, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            TirPercentageRow("Very Low", uiState.veryLow, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
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