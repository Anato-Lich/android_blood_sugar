package com.example.bloodsugar.features.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloodsugar.R
import com.example.bloodsugar.database.FoodItem

@Composable
fun FoodScreen(foodViewModel: FoodViewModel = viewModel()) {
    val foodItems by foodViewModel.foodItems.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingFoodItem by remember { mutableStateOf<FoodItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingFoodItem = null
                showDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_food))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(stringResource(id = R.string.food_library), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(foodItems) { item ->
                FoodItemRow(
                    foodItem = item,
                    onEdit = {
                        editingFoodItem = it
                        showDialog = true
                    },
                    onDelete = { foodViewModel.deleteFoodItem(it) }
                )
            }
        }
    }

    if (showDialog) {
        FoodDialog(
            foodItem = editingFoodItem,
            onDismiss = { showDialog = false },
            onConfirm = { id, name, serving, carbs ->
                foodViewModel.addOrUpdateFoodItem(id, name, serving, carbs)
                showDialog = false
            }
        )
    }
}

@Composable
fun FoodItemRow(foodItem: FoodItem, onEdit: (FoodItem) -> Unit, onDelete: (FoodItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(foodItem.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "%.1fg serving has %.1fg carbs (%.1f / 100g)".format(
                        foodItem.servingSizeGrams,
                        foodItem.carbsPerServing,
                        foodItem.carbsPer100g
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row {
                IconButton(onClick = { onEdit(foodItem) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit_food))
                }
                IconButton(onClick = { onDelete(foodItem) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_food))
                }
            }
        }
    }
}

@Composable
fun FoodDialog(
    foodItem: FoodItem?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(foodItem?.name ?: "") }
    var servingSize by remember { mutableStateOf(foodItem?.servingSizeGrams?.toString() ?: "") }
    var carbsPerServing by remember { mutableStateOf(foodItem?.carbsPerServing?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (foodItem == null) stringResource(id = R.string.add_food) else stringResource(id = R.string.edit_food)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.food_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = { Text(stringResource(id = R.string.serving_size_grams)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = carbsPerServing,
                    onValueChange = { carbsPerServing = it },
                    label = { Text(stringResource(id = R.string.carbs_per_serving_grams)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(foodItem?.id ?: 0, name, servingSize, carbsPerServing) },
                enabled = name.isNotBlank() && servingSize.isNotBlank() && carbsPerServing.isNotBlank()
            ) {
                Text(if (foodItem == null) stringResource(id = R.string.add) else stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
