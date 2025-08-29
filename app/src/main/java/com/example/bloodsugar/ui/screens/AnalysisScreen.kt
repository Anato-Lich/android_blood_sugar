package com.example.bloodsugar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloodsugar.viewmodel.AnalysisViewModel

@Composable
fun AnalysisScreen(analysisViewModel: AnalysisViewModel = viewModel()) {
    val uiState by analysisViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Time In Range Analysis", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { analysisViewModel.calculateTir(7) }, enabled = uiState.selectedPeriod != 7) {
                Text("7 Days")
            }
            Button(onClick = { analysisViewModel.calculateTir(30) }, enabled = uiState.selectedPeriod != 30) {
                Text("30 Days")
            }
            Button(onClick = { analysisViewModel.calculateTir(90) }, enabled = uiState.selectedPeriod != 90) {
                Text("90 Days")
            }
        }

        TirCard(uiState = uiState)
    }
}

@Composable
fun TirCard(uiState: com.example.bloodsugar.viewmodel.AnalysisUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TirRow("Very High", uiState.veryHigh, Color.Red.copy(alpha = 0.7f))
            TirRow("High", uiState.high, Color.Red.copy(alpha = 0.4f))
            TirRow("In Range", uiState.timeInRange, Color(0xFF006400))
            TirRow("Low", uiState.low, Color.Blue.copy(alpha = 0.4f))
            TirRow("Very Low", uiState.veryLow, Color.Blue.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun TirRow(label: String, percentage: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("%.1f%%".format(percentage), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
