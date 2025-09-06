package com.example.bloodsugar.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bloodsugar.R
import com.example.bloodsugar.database.ActivityRecord
import com.example.bloodsugar.database.BloodSugarRecord
import com.example.bloodsugar.database.EventRecord
import com.example.bloodsugar.features.history.ActivityHistoryItem
import com.example.bloodsugar.features.history.EventHistoryItem
import com.example.bloodsugar.features.history.RecordItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var showStartDateDialog by remember { mutableStateOf(false) }
    var showEndDateDialog by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubberPosition by remember { mutableFloatStateOf(0f) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var historyCardPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.scrollToHistory) {
        if (uiState.scrollToHistory) {
            scope.launch {
                scrollState.animateScrollTo(historyCardPosition.toInt())
            }
            homeViewModel.onScrollToHistoryHandled()
        }
    }

    if (uiState.shownDialog != null) {
        LogInputDialog(
            dialogType = uiState.shownDialog!!,
            uiState = uiState,
            onDismiss = { homeViewModel.onDialogDismiss() },
            onSaveRecord = {
                homeViewModel.saveRecord()
                homeViewModel.onDialogDismiss()
            },
            onSaveEvent = {
                homeViewModel.saveInsulinCarbEvent()
                homeViewModel.onDialogDismiss()
            },
            onSaveActivity = {
                homeViewModel.saveActivity()
                homeViewModel.onDialogDismiss()
            },
            onSugarChange = { homeViewModel.setSugarValue(it) },
            onCommentChange = { homeViewModel.setComment(it) },
            onInsulinChange = { homeViewModel.setInsulinValue(it) },
            onCarbsChange = { homeViewModel.setCarbsValue(it) },
            onActivityTypeChange = { homeViewModel.setActivityType(it) },
            onActivityDurationChange = { homeViewModel.setActivityDuration(it) },
            onActivityIntensityChange = { homeViewModel.setActivityIntensity(it) },
            onTimestampChange = { homeViewModel.setNewRecordTimestamp(it) }
        )
    }

    if (showStartDateDialog) {
        SingleDatePickerDialog(
            onDismiss = { showStartDateDialog = false },
            onConfirm = { date ->
                homeViewModel.setCustomStartDate(date)
                showStartDateDialog = false
            }
        )
    }

    if (showEndDateDialog) {
        SingleDatePickerDialog(
            onDismiss = { showEndDateDialog = false },
            onConfirm = { date ->
                homeViewModel.setCustomEndDate(date)
                showEndDateDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key Metrics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (uiState.chartData != null) {
                    Metric(stringResource(id = R.string.home_metric_min), uiState.chartData!!.min)
                    Metric(stringResource(id = R.string.home_metric_avg), uiState.chartData!!.avg)
                    Metric(stringResource(id = R.string.home_metric_max), uiState.chartData!!.max)
                } else {
                    Text(
                        stringResource(id = R.string.home_no_metrics),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val pagerState = rememberPagerState(pageCount = { 4 })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { page ->
                        val cardModifier = Modifier.fillMaxWidth()
                        when (page) {
                            0 -> Hba1cMetric(uiState.estimatedA1c, modifier = cardModifier)
                            1 -> CarbsProgressMetric(
                                consumed = uiState.todaysCarbs,
                                goal = uiState.dailyCarbsGoal,
                                modifier = cardModifier
                            )
                            2 -> TirBar(uiState = uiState, modifier = cardModifier)
                            3 -> AvgDailyInsulinMetric(value = uiState.avgDailyInsulin, modifier = cardModifier)
                        }
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
            }
        }

        // Chart Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                BloodSugarChart(
                    chartData = uiState.chartData,
                    selectedRecord = uiState.selectedRecord,
                    onRecordClick = { homeViewModel.onChartRecordSelected(it) },
                    onDismissTooltip = { homeViewModel.onChartSelectionDismissed() },
                    isScrubbing = isScrubbing,
                    scrubberPosition = scrubberPosition,
                    onScrub = { position ->
                        isScrubbing = true
                        scrubberPosition = position
                    },
                    onScrubEnd = { isScrubbing = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                var showFilterMenu by remember { mutableStateOf(false) }
                Column(modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)) {
                    Button(onClick = { showFilterMenu = true }) {
                        val filterText = when (uiState.selectedFilter) {
                            FilterType.TODAY -> stringResource(id = R.string.home_filter_today)
                            FilterType.THREE_DAYS -> stringResource(id = R.string.home_filter_3_days)
                            FilterType.SEVEN_DAYS -> stringResource(id = R.string.home_filter_7_days)
                            FilterType.CUSTOM -> stringResource(id = R.string.home_filter_custom)
                        }
                        Text(text = stringResource(id = R.string.home_filter, filterText))
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        val filterTypes = listOf(FilterType.TODAY, FilterType.THREE_DAYS, FilterType.SEVEN_DAYS, FilterType.CUSTOM)
                        filterTypes.forEach { filter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (filter) {
                                            FilterType.TODAY -> stringResource(id = R.string.home_filter_today)
                                            FilterType.THREE_DAYS -> stringResource(id = R.string.home_filter_3_days)
                                            FilterType.SEVEN_DAYS -> stringResource(id = R.string.home_filter_7_days)
                                            FilterType.CUSTOM -> stringResource(id = R.string.home_filter_custom)
                                        }
                                    )
                                },
                                onClick = {
                                    showFilterMenu = false
                                    homeViewModel.setFilter(filter)
                                }
                            )
                        }
                    }
                    if (uiState.selectedFilter == FilterType.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                            OutlinedButton(onClick = { showStartDateDialog = true }) {
                                val text = uiState.customStartDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(id = R.string.home_start_date)
                                Text(text)
                            }
                            OutlinedButton(onClick = { showEndDateDialog = true }) {
                                val text = uiState.customEndDate?.let { dateFormatter.format(Date(it)) } ?: stringResource(id = R.string.home_end_date)
                                Text(text)
                            }
                        }
                    }
                }
            }
        }

        // Recent History Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    historyCardPosition = it.positionInParent().y
                }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.home_recent_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.recentHistoryItems.isEmpty()) {
                    Text(stringResource(id = R.string.home_no_recent_history), modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                } else {
                    uiState.recentHistoryItems.forEach { item ->
                        when (item) {
                            is BloodSugarRecord -> RecordItem(record = item, onDelete = { homeViewModel.deleteRecord(item) })
                            is EventRecord -> EventHistoryItem(event = item, onDelete = { homeViewModel.deleteEvent(item) })
                            is ActivityRecord -> ActivityHistoryItem(activity = item, onDelete = { homeViewModel.deleteActivity(item) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(id = R.string.home_view_all_history))
                    }
                }
            }
        }
    }
}
