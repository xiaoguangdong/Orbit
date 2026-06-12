package com.xiaoguangdong.orbit.ui.screens.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.components.EmptyState
import com.xiaoguangdong.orbit.ui.components.HeatmapGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: OrbitViewModel,
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val detailFlow = remember(habitId) { viewModel.observeHabitDetail(habitId) }
    val detail by detailFlow.collectAsStateWithLifecycle()
    if (detail == null) {
        EmptyState("Habit not found", "This orbit may have been deleted.")
        return
    }

    val habit = detail!!.habit
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text("Orbit detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                },
            )
        }
        item {
            DetailPanel(
                title = habit.name,
                lines = listOf(
                    "${habit.streak} day current streak",
                    "${habit.bestStreak} day best streak",
                    "${detail!!.totalCompleted} total completions",
                    "${(habit.completionRate30d * 100).toInt()}% completion in 30 days",
                ),
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = habit.statusForDate == CheckInStatus.COMPLETED,
                    onClick = {
                        if (habit.statusForDate == CheckInStatus.COMPLETED) viewModel.undoHabit(habit.id)
                        else viewModel.completeHabit(habit.id)
                    },
                    label = { Text(if (habit.statusForDate == CheckInStatus.COMPLETED) "Undo" else "Check in") },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.skipHabit(habit.id) },
                    label = { Text("Skip") },
                )
                FilterChip(
                    selected = habit.statusForDate == CheckInStatus.FAILED,
                    onClick = { viewModel.failHabit(habit.id) },
                    label = { Text("Fail") },
                    leadingIcon = { Icon(Icons.Outlined.HeartBroken, null) },
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        if (habit.archived) viewModel.restoreHabit(habit.id)
                        else viewModel.archiveHabit(habit.id)
                    },
                    label = { Text(if (habit.archived) "Restore" else "Archive") },
                    leadingIcon = {
                        Icon(
                            if (habit.archived) Icons.Outlined.Restore else Icons.Outlined.Archive,
                            null,
                        )
                    },
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.deleteHabit(habit.id) },
                    label = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                )
            }
        }
        item {
            DetailPanel(title = "Last 30 planned days", content = {
                HeatmapGrid(records = detail!!.monthlyRecords)
            })
        }
        item {
            Text("Recent records", style = MaterialTheme.typography.headlineMedium)
        }
        items(detail!!.recentRecords, key = { it.date.toString() }) { record ->
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(record.date.toString(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            record.status?.displayName ?: "No record",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    record.value?.let {
                        Text(it.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    title: String,
    lines: List<String> = emptyList(),
    content: @Composable () -> Unit = {},
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}
