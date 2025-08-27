package com.example.bloodsugar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloodsugar.viewmodel.CalculatorViewModel
import com.example.bloodsugar.viewmodel.MealType
import com.example.bloodsugar.viewmodel.SharedViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    calculatorViewModel: CalculatorViewModel = viewModel(),
    sharedViewModel: SharedViewModel
) {
    val uiState by calculatorViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.calculationId) {
        if (uiState.calculationId == 0) return@LaunchedEffect

        when (uiState.lastCalculationSource) {
            1 -> { // Insulin from Carbs
                val carbs = uiState.carbs.replace(',', '.').toFloatOrNull()
                sharedViewModel.setLatestInsulinDose(uiState.insulinDose)
                sharedViewModel.setLatestCarbs(carbs)
            }
            2 -> { // Carbs from Insulin
                val insulin = uiState.insulinDoseForCarbs.replace(',', '.').toFloatOrNull()
                sharedViewModel.setLatestInsulinDose(insulin)
                sharedViewModel.setLatestCarbs(uiState.calculatedCarbs)
            }
            3 -> { // Remaining Carbs
                val insulin = uiState.insulinDoseForRemainingCarbs.replace(',', '.').toFloatOrNull()
                sharedViewModel.setLatestInsulinDose(insulin)
                sharedViewModel.setLatestCarbs(uiState.totalCarbsForDose)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Insulin Calculator", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Insulin from Carbs", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.carbs,
                    onValueChange = { calculatorViewModel.setCarbs(it) },
                    label = { Text("Carbs (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { calculatorViewModel.calculateDose(MealType.BREAKFAST) }) {
                        Text("Breakfast")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateDose(MealType.DINNER) }) {
                        Text("Dinner")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateDose(MealType.SUPPER) }) {
                        Text("Supper")
                    }
                }
                uiState.insulinDose?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Required Insulin Dose", style = MaterialTheme.typography.titleMedium)
                    Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Carbs from Insulin", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.insulinDoseForCarbs,
                    onValueChange = { calculatorViewModel.setInsulinDoseForCarbs(it) },
                    label = { Text("Insulin Dose") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { calculatorViewModel.calculateCarbs(MealType.BREAKFAST) }) {
                        Text("Breakfast")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateCarbs(MealType.DINNER) }) {
                        Text("Dinner")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateCarbs(MealType.SUPPER) }) {
                        Text("Supper")
                    }
                }
                uiState.calculatedCarbs?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Calculated Carbs (grams)", style = MaterialTheme.typography.titleMedium)
                    Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Remaining Carbs", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.insulinDoseForRemainingCarbs,
                    onValueChange = { calculatorViewModel.setInsulinDoseForRemainingCarbs(it) },
                    label = { Text("Insulin Dose") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.plannedCarbs,
                    onValueChange = { calculatorViewModel.setPlannedCarbs(it) },
                    label = { Text("Planned Carbs (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.BREAKFAST) }) {
                        Text("Breakfast")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.DINNER) }) {
                        Text("Dinner")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.SUPPER) }) {
                        Text("Supper")
                    }
                }
                uiState.remainingCarbs?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Remaining Carbs to Eat", style = MaterialTheme.typography.titleMedium)
                    Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
    }
}
