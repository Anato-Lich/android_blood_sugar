package com.example.bloodsugar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getValueColor(value: Float, lowColor: Color, inRangeColor: Color, highColor: Color): Color {
    return when {
        value < 4f -> lowColor
        value <= 10f -> inRangeColor
        else -> highColor
    }
}

private fun getActivityIcon(activityType: String): ImageVector {
    return when (activityType) {
        "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
        "Gym" -> Icons.Default.FitnessCenter
        else -> Icons.Default.FitnessCenter
    }
}

@Composable
fun RecordItem(
    record: BloodSugarRecord,
    onDelete: () -> Unit
) {
    val valueColor = getValueColor(
        value = record.value,
        lowColor = MaterialTheme.colorScheme.secondary,
        inRangeColor = MaterialTheme.colorScheme.primary,
        highColor = MaterialTheme.colorScheme.error
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp) // Adjust height to be dynamic if possible
                    .background(valueColor)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = "Blood Sugar Record",
                        tint = valueColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "%.1f mmol/L".format(record.value),
                            style = MaterialTheme.typography.titleLarge,
                            color = valueColor
                        )
                        if (record.comment.isNotBlank()) {
                            Text(
                                text = record.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Record")
                }
            }
        }
    }
}

@Composable
fun EventHistoryItem(
    event: EventRecord,
    onDelete: () -> Unit
) {
    val (color, label, icon) = when (event.type) {
        "INSULIN" -> Triple(MaterialTheme.colorScheme.secondary, "Insulin", Icons.Default.MedicalServices)
        "CARBS" -> Triple(MaterialTheme.colorScheme.tertiary, "Carbs", Icons.Default.Restaurant)
        else -> Triple(Color.Gray, "Unknown Event", Icons.Default.Info)
    }
    val valueText = when(event.type) {
        "INSULIN" -> "%.1fu".format(event.value)
        "CARBS" -> "%.1fg".format(event.value)
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge,
                            color = color
                        )
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (event.foodServing != null) {
                            Text(
                                text = event.foodServing!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (event.foodName != null) {
                            Text(
                                text = "(${event.foodName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryItem(activity: ActivityRecord, onDelete: () -> Unit) {
    val color = MaterialTheme.colorScheme.tertiary
    val icon = getActivityIcon(activity.type)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(6.dp)
                    .height(80.dp)
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = "Activity",
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = activity.type,
                            style = MaterialTheme.typography.titleLarge,
                            color = color
                        )
                        Text(
                            text = "${activity.durationMinutes} min (${activity.intensity})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(activity.timestamp)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Activity")
                }
            }
        }
    }
}
