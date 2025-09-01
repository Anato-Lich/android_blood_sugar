package com.example.bloodsugar.features.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bloodsugar.Screen
import com.example.bloodsugar.database.FoodItem
import com.example.bloodsugar.features.home.HomeViewModel
import com.example.bloodsugar.domain.MealComponent
import com.example.bloodsugar.domain.MealType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    navController: NavController,
    calculatorViewModel: CalculatorViewModel = viewModel(),
    homeViewModel: HomeViewModel
) {
    val uiState by calculatorViewModel.uiState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Insulin from Carbs", "Carbs from Insulin", "Remaining Carbs")
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (selectedTabIndex) {
                0 -> InsulinFromCarbs(uiState, calculatorViewModel, homeViewModel, navController, scrollState, scope)
                1 -> CarbsFromInsulin(uiState, calculatorViewModel, scrollState, scope)
                2 -> RemainingCarbs(uiState, calculatorViewModel, scrollState, scope)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InsulinFromCarbs(
    uiState: CalculatorUiState,
    calculatorViewModel: CalculatorViewModel,
    homeViewModel: HomeViewModel,
    navController: NavController,
    scrollState: ScrollState,
    scope: CoroutineScope
) {
    var useGrams by remember { mutableStateOf(true) }
    val totalCarbs = uiState.components.sumOf { it.carbs.toDouble() }.toFloat()
    var editingComponent by remember { mutableStateOf<MealComponent?>(null) }

    LaunchedEffect(uiState.insulinDose) {
        if (uiState.insulinDose != null) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    editingComponent?.let {
        EditComponentDialog(
            component = it,
            onDismiss = { editingComponent = null },
            onConfirm = { id, newServing, newUseGrams ->
                calculatorViewModel.updateComponent(id, newServing, newUseGrams)
                editingComponent = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meal Builder", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))

            // Total Carbs Display
            Text("Total Carbs", style = MaterialTheme.typography.titleMedium)
            Text("%.1f g".format(totalCarbs), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))

            // Added components pager
            if (uiState.components.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { uiState.components.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) { page ->
                    val component = uiState.components[page]
                    val foodItem = component.foodItemId?.let { id ->
                        uiState.foodItems.find { it.id == id }
                    }
                    MealComponentCard(
                        component = component,
                        foodItem = foodItem,
                        dailyCarbsGoal = uiState.dailyCarbsGoal,
                        onEdit = { editingComponent = it },
                        onDelete = { calculatorViewModel.removeComponent(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier.height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add from Food Library
            var foodExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = foodExpanded, onExpandedChange = { foodExpanded = !foodExpanded }) {
                OutlinedTextField(
                    value = uiState.selectedFood?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Food") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(0.9f)
                )
                ExposedDropdownMenu(expanded = foodExpanded, onDismissRequest = { foodExpanded = false }) {
                    uiState.foodItems.forEach { food ->
                        DropdownMenuItem(
                            text = { Text(food.name) },
                            onClick = {
                                calculatorViewModel.onFoodSelected(food)
                                foodExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.selectedFood != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (useGrams) {
                        Button(onClick = { /* Already selected */ }) { Text("Grams") }
                        OutlinedButton(onClick = { useGrams = false; calculatorViewModel.resetFoodInputs() }) { Text("Servings") }
                    } else {
                        OutlinedButton(onClick = { useGrams = true; calculatorViewModel.resetFoodInputs() }) { Text("Grams") }
                        Button(onClick = { /* Already selected */ }) { Text("Servings") }
                    }
                }
                OutlinedTextField(
                    value = uiState.foodServingValue,
                    onValueChange = { calculatorViewModel.calculateCarbsFromFood(it, useGrams) },
                    label = { Text(if (useGrams) "Weight (g)" else "Number of Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                OutlinedTextField(
                    value = uiState.foodCarbs,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Calculated Carbs") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                Button(
                    onClick = { calculatorViewModel.addFoodComponent(useGrams) },
                    enabled = uiState.foodCarbs.isNotBlank()
                ) {
                    Text("Add Food")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Manual Carbs
            OutlinedTextField(
                value = uiState.manualCarbsEntry,
                onValueChange = { calculatorViewModel.setManualCarbsEntry(it) },
                label = { Text("Additional Carbs (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Button(
                onClick = { calculatorViewModel.addManualCarbsComponent() },
                enabled = uiState.manualCarbsEntry.isNotBlank()
            ) {
                Text("Add Carbs")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { calculatorViewModel.calculateDose(MealType.BREAKFAST) }) { Text("Breakfast") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateDose(MealType.DINNER) }) { Text("Dinner") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateDose(MealType.SUPPER) }) { Text("Supper") }
            }

            // Result
            uiState.insulinDose?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Required Insulin Dose", style = MaterialTheme.typography.titleMedium)
                Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val details = uiState.components.joinToString(separator = "\n") { 
                            "- ${it.name} (${it.servingValue}${if(it.useGrams) "g" else " servings"}) -> ${"%.1f".format(it.carbs)}g"
                        }
                        val roundedTotalCarbs = totalCarbs.toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP).toFloat()
                        homeViewModel.logMealFromCalculator(roundedTotalCarbs, uiState.insulinDose, details)
                        calculatorViewModel.clearCalculator()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Calculator.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Text("Log Meal")
                    }
                    OutlinedButton(onClick = { calculatorViewModel.clearCalculator() }) {
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

@Composable
fun EditComponentDialog(
    component: MealComponent,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var servingValue by remember { mutableStateOf(component.servingValue) }
    var useGrams by remember { mutableStateOf(component.useGrams) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${component.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (component.foodItemId != null) { // Is a food item, allow switching units
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (useGrams) {
                            Button(onClick = { /* Already selected */ }) { Text("Grams") }
                            OutlinedButton(onClick = { useGrams = false }) { Text("Servings") }
                        } else {
                            OutlinedButton(onClick = { useGrams = true }) { Text("Grams") }
                            Button(onClick = { /* Already selected */ }) { Text("Servings") }
                        }
                    }
                    OutlinedTextField(
                        value = servingValue,
                        onValueChange = { servingValue = it },
                        label = { Text(if (useGrams) "Weight (g)" else "Number of Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else { // Is a manual entry, only edit carbs
                    OutlinedTextField(
                        value = servingValue,
                        onValueChange = { servingValue = it },
                        label = { Text("Carbs (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(component.id, servingValue, useGrams) },
                enabled = servingValue.isNotBlank() && servingValue.replace(',', '.').toFloatOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarbsFromInsulin(uiState: CalculatorUiState, calculatorViewModel: CalculatorViewModel, scrollState: ScrollState, scope: CoroutineScope) {
    LaunchedEffect(uiState.calculatedCarbs) {
        if (uiState.calculatedCarbs != null) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Calculate Carbs from Insulin Dose", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.insulinDoseForCarbs,
                onValueChange = { calculatorViewModel.setInsulinDoseForCarbs(it) },
                label = { Text("Insulin Dose") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RemainingCarbs(uiState: CalculatorUiState, calculatorViewModel: CalculatorViewModel, scrollState: ScrollState, scope: CoroutineScope) {
    var useGrams by remember { mutableStateOf(true) }
    val totalCarbs = uiState.remainingCarbsComponents.sumOf { it.carbs.toDouble() }.toFloat()
    var editingComponent by remember { mutableStateOf<MealComponent?>(null) }

    LaunchedEffect(uiState.remainingCarbs) {
        if (uiState.remainingCarbs != null) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    editingComponent?.let {
        EditComponentDialog(
            component = it,
            onDismiss = { editingComponent = null },
            onConfirm = { id, newServing, newUseGrams ->
                calculatorViewModel.updateRemainingCarbsComponent(id, newServing, newUseGrams)
                editingComponent = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Calculate Remaining Carbs", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.insulinDoseForRemainingCarbs,
                onValueChange = { calculatorViewModel.setInsulinDoseForRemainingCarbs(it) },
                label = { Text("Insulin Dose") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Planned Meal", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))

            // Total Carbs Display
            Text("Total Carbs", style = MaterialTheme.typography.titleMedium)
            Text("%.1f g".format(totalCarbs), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))

            // Added components pager
            if (uiState.remainingCarbsComponents.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { uiState.remainingCarbsComponents.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) { page ->
                    val component = uiState.remainingCarbsComponents[page]
                    val foodItem = component.foodItemId?.let { id ->
                        uiState.foodItems.find { it.id == id }
                    }
                    MealComponentCard(
                        component = component,
                        foodItem = foodItem,
                        dailyCarbsGoal = uiState.dailyCarbsGoal,
                        onEdit = { editingComponent = it },
                        onDelete = { calculatorViewModel.removeRemainingCarbsComponent(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier.height(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add from Food Library
            var foodExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = foodExpanded, onExpandedChange = { foodExpanded = !foodExpanded }) {
                OutlinedTextField(
                    value = uiState.selectedFood?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Food") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = foodExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(0.9f)
                )
                ExposedDropdownMenu(expanded = foodExpanded, onDismissRequest = { foodExpanded = false }) {
                    uiState.foodItems.forEach { food ->
                        DropdownMenuItem(
                            text = { Text(food.name) },
                            onClick = {
                                calculatorViewModel.onFoodSelected(food)
                                foodExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.selectedFood != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (useGrams) {
                        Button(onClick = { /* Already selected */ }) { Text("Grams") }
                        OutlinedButton(onClick = { useGrams = false; calculatorViewModel.resetFoodInputs() }) { Text("Servings") }
                    } else {
                        OutlinedButton(onClick = { useGrams = true; calculatorViewModel.resetFoodInputs() }) { Text("Grams") }
                        Button(onClick = { /* Already selected */ }) { Text("Servings") }
                    }
                }
                OutlinedTextField(
                    value = uiState.foodServingValue,
                    onValueChange = { calculatorViewModel.calculateCarbsFromFood(it, useGrams) },
                    label = { Text(if (useGrams) "Weight (g)" else "Number of Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                OutlinedTextField(
                    value = uiState.foodCarbs,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Calculated Carbs") },
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                Button(
                    onClick = { calculatorViewModel.addRemainingCarbsComponent(useGrams) },
                    enabled = uiState.foodCarbs.isNotBlank()
                ) {
                    Text("Add Food")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Manual Carbs
            OutlinedTextField(
                value = uiState.manualCarbsEntry,
                onValueChange = { calculatorViewModel.setManualCarbsEntry(it) },
                label = { Text("Additional Carbs (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Button(
                onClick = { calculatorViewModel.addManualRemainingCarbsComponent() },
                enabled = uiState.manualCarbsEntry.isNotBlank()
            ) {
                Text("Add Carbs")
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MealComponentCard(
    component: MealComponent,
    foodItem: FoodItem?,
    dailyCarbsGoal: Float,
    onEdit: (MealComponent) -> Unit,
    onDelete: (String) -> Unit
) {
    val weight: Float?
    val servings: Float?
    if (foodItem != null) {
        if (component.useGrams) {
            weight = component.servingValue.toFloatOrNull()
            servings = weight?.let {
                if (foodItem.servingSizeGrams > 0) it / foodItem.servingSizeGrams else 0f
            }
        } else {
            servings = component.servingValue.toFloatOrNull()
            weight = servings?.let { it * foodItem.servingSizeGrams }
        }
    } else {
        weight = null
        servings = null
    }

    val carbPercentOfGoal = if (dailyCarbsGoal > 0) {
        (component.carbs / dailyCarbsGoal) * 100
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = { onDelete(component.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Carbs: %.1f g (%d%% of goal)".format(component.carbs, carbPercentOfGoal.toInt()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (weight != null) {
                Text(
                    text = "Weight: %.1f g".format(weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (component.useGrams) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (servings != null) {
                Text(
                    text = "Servings: %.1f".format(servings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!component.useGrams) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onEdit(component) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit")
            }
        }
    }
}