package com.example.bloodsugar.features.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bloodsugar.R
import com.example.bloodsugar.Screen
import com.example.bloodsugar.database.FoodItem
import com.example.bloodsugar.features.home.HomeViewModel
import com.example.bloodsugar.domain.MealComponent
import com.example.bloodsugar.domain.MealType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    navController: NavController,
    calculatorViewModel: CalculatorViewModel = viewModel(),
    homeViewModel: HomeViewModel
) {
    val uiState by calculatorViewModel.uiState.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.calculator_tab_insulin_from_carbs),
        stringResource(id = R.string.calculator_tab_carbs_from_insulin),
        stringResource(id = R.string.calculator_tab_remaining_carbs)
    )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InsulinFromCarbs(
    uiState: CalculatorUiState,
    calculatorViewModel: CalculatorViewModel,
    homeViewModel: HomeViewModel,
    navController: NavController,
    scrollState: ScrollState,
    scope: CoroutineScope
) {
    val totalCarbs = uiState.components.sumOf { it.carbs.toDouble() }.toFloat()

    LaunchedEffect(uiState.insulinDose) {
        if (uiState.insulinDose != null) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MealBuilder(
                uiState = uiState,
                calculatorViewModel = calculatorViewModel,
                components = uiState.components,
                totalCarbs = totalCarbs,
                onUpdateComponent = { id, newServing, newUseGrams ->
                    calculatorViewModel.updateComponent(id, newServing, newUseGrams)
                },
                onRemoveComponent = { calculatorViewModel.removeComponent(it) },
                onAddFoodComponent = { useGrams -> calculatorViewModel.addFoodComponent(useGrams) },
                onAddManualComponent = { calculatorViewModel.addManualCarbsComponent() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { calculatorViewModel.calculateDose(MealType.BREAKFAST) }) { Text(stringResource(id = R.string.settings_breakfast)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateDose(MealType.DINNER) }) { Text(stringResource(id = R.string.settings_dinner)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateDose(MealType.SUPPER) }) { Text(stringResource(id = R.string.settings_supper)) }
            }

            // Result
            uiState.insulinDose?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.calculator_required_insulin_dose), style = MaterialTheme.typography.titleMedium)
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
                    }) { Text(stringResource(id = R.string.calculator_log_meal)) }
                    OutlinedButton(onClick = { calculatorViewModel.clearCalculator() }) { Text(stringResource(id = R.string.calculator_clear_all)) }
                }
            }
        }
    }
}

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
            Text(stringResource(id = R.string.calculator_calculate_carbs_from_insulin), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.insulinDoseForCarbs,
                onValueChange = { calculatorViewModel.setInsulinDoseForCarbs(it) },
                label = { Text(stringResource(id = R.string.calculator_insulin_dose)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { calculatorViewModel.calculateCarbs(MealType.BREAKFAST) }) {
                    Text(stringResource(id = R.string.settings_breakfast))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateCarbs(MealType.DINNER) }) {
                    Text(stringResource(id = R.string.settings_dinner))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateCarbs(MealType.SUPPER) }) {
                    Text(stringResource(id = R.string.settings_supper))
                }
            }
            uiState.calculatedCarbs?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.calculator_calculated_carbs), style = MaterialTheme.typography.titleMedium)
                Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RemainingCarbs(uiState: CalculatorUiState, calculatorViewModel: CalculatorViewModel, scrollState: ScrollState, scope: CoroutineScope) {
    val totalCarbs = uiState.remainingCarbsComponents.sumOf { it.carbs.toDouble() }.toFloat()

    LaunchedEffect(uiState.remainingCarbs) {
        if (uiState.remainingCarbs != null) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(id = R.string.calculator_calculate_remaining_carbs), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.insulinDoseForRemainingCarbs,
                onValueChange = { calculatorViewModel.setInsulinDoseForRemainingCarbs(it) },
                label = { Text(stringResource(id = R.string.calculator_insulin_dose)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            MealBuilder(
                uiState = uiState,
                calculatorViewModel = calculatorViewModel,
                components = uiState.remainingCarbsComponents,
                totalCarbs = totalCarbs,
                onUpdateComponent = { id, newServing, newUseGrams ->
                    calculatorViewModel.updateRemainingCarbsComponent(id, newServing, newUseGrams)
                },
                onRemoveComponent = { calculatorViewModel.removeRemainingCarbsComponent(it) },
                onAddFoodComponent = { useGrams -> calculatorViewModel.addRemainingCarbsComponent(useGrams) },
                onAddManualComponent = { calculatorViewModel.addManualRemainingCarbsComponent() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.BREAKFAST) }) {
                    Text(stringResource(id = R.string.settings_breakfast))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.DINNER) }) {
                    Text(stringResource(id = R.string.settings_dinner))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { calculatorViewModel.calculateRemainingCarbs(MealType.SUPPER) }) {
                    Text(stringResource(id = R.string.settings_supper))
                }
            }
            uiState.remainingCarbs?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.calculator_remaining_carbs_to_eat), style = MaterialTheme.typography.titleMedium)
                Text("%.2f".format(it), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MealBuilder(
    uiState: CalculatorUiState,
    calculatorViewModel: CalculatorViewModel,
    components: List<MealComponent>,
    totalCarbs: Float,
    onUpdateComponent: (String, String, Boolean) -> Unit,
    onRemoveComponent: (String) -> Unit,
    onAddFoodComponent: (Boolean) -> Unit,
    onAddManualComponent: () -> Unit
) {
    var useGrams by remember { mutableStateOf(true) }
    var editingComponent by remember { mutableStateOf<MealComponent?>(null) }

    editingComponent?.let {
        EditComponentDialog(
            component = it,
            onDismiss = { editingComponent = null },
            onConfirm = { id, newServing, newUseGrams ->
                onUpdateComponent(id, newServing, newUseGrams)
                editingComponent = null
            }
        )
    }

    Text(stringResource(id = R.string.calculator_meal_builder), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))

    // Total Carbs Display
    Text(stringResource(id = R.string.calculator_total_carbs), style = MaterialTheme.typography.titleMedium)
    Text("%.1f g".format(totalCarbs), style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(4.dp))

    // Added components pager
    if (components.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { components.size })
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            val component = components[page]
            val foodItem = component.foodItemId?.let { id ->
                uiState.foodItems.find { it.id == id }
            }
            MealComponentCard(
                component = component,
                foodItem = foodItem,
                dailyCarbsGoal = uiState.dailyCarbsGoal,
                onEdit = { editingComponent = it },
                onDelete = { onRemoveComponent(it) }
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
            label = { Text(stringResource(id = R.string.screen_food)) },
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
                Button(onClick = { /* Already selected */ }) { Text(stringResource(id = R.string.export_unit_grams)) }
                OutlinedButton(onClick = { useGrams = false; calculatorViewModel.resetFoodInputs() }) { Text(stringResource(id = R.string.food_servings)) }
            } else {
                OutlinedButton(onClick = { useGrams = true; calculatorViewModel.resetFoodInputs() }) { Text(stringResource(id = R.string.export_unit_grams)) }
                Button(onClick = { /* Already selected */ }) { Text(stringResource(id = R.string.food_servings)) }
            }
        }
        OutlinedTextField(
            value = uiState.foodServingValue,
            onValueChange = { calculatorViewModel.calculateCarbsFromFood(it, useGrams) },
            label = { Text(if (useGrams) stringResource(id = R.string.food_weight_g) else stringResource(id = R.string.food_num_servings)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        OutlinedTextField(
            value = uiState.foodCarbs,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.calculator_calculated_carbs)) },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Button(
            onClick = { onAddFoodComponent(useGrams) },
            enabled = uiState.foodCarbs.isNotBlank()
        ) { 
            Text(stringResource(id = R.string.calculator_add_food))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Add Manual Carbs
    OutlinedTextField(
        value = uiState.manualCarbsEntry,
        onValueChange = { calculatorViewModel.setManualCarbsEntry(it) },
        label = { Text(stringResource(id = R.string.food_additional_carbs)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(0.9f)
    )
    Button(
        onClick = { onAddManualComponent() },
        enabled = uiState.manualCarbsEntry.isNotBlank()
    ) {
        Text(stringResource(id = R.string.calculator_add_carbs))
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
        title = { Text("${stringResource(id = R.string.edit)} ${component.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (component.foodItemId != null) { // Is a food item, allow switching units
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (useGrams) {
                            Button(onClick = { /* Already selected */ }) { Text(stringResource(id = R.string.export_unit_grams)) }
                            OutlinedButton(onClick = { useGrams = false }) { Text(stringResource(id = R.string.food_servings)) }
                        } else {
                            OutlinedButton(onClick = { useGrams = true }) { Text(stringResource(id = R.string.export_unit_grams)) }
                            Button(onClick = { /* Already selected */ }) { Text(stringResource(id = R.string.food_servings)) }
                        }
                    }
                    OutlinedTextField(
                        value = servingValue,
                        onValueChange = { servingValue = it },
                        label = { Text(if (useGrams) stringResource(id = R.string.food_weight_g) else stringResource(id = R.string.food_num_servings)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else { // Is a manual entry, only edit carbs
                    OutlinedTextField(
                        value = servingValue,
                        onValueChange = { servingValue = it },
                        label = { Text(stringResource(id = R.string.carbs_per_serving_grams)) },
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
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
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
                    Icon(Icons.Default.Close, stringResource(id = R.string.delete), tint = MaterialTheme.colorScheme.error)
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
                Icon(Icons.Default.Edit, stringResource(id = R.string.edit), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.edit))
            }
        }
    }
}


