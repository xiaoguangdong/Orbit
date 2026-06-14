package com.xiaoguangdong.orbit.ui.screens.habit

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.checkInStatusLabel
import com.xiaoguangdong.orbit.ui.tr
import com.xiaoguangdong.orbit.ui.components.EmptyState
import com.xiaoguangdong.orbit.ui.components.HeatmapGrid
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: OrbitViewModel,
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val detailFlow = remember(habitId) { viewModel.observeHabitDetail(habitId) }
    val detail by detailFlow.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (detail == null) {
        EmptyState(
            tr("Habit not found", "未找到习惯"),
            tr("This orbit may have been deleted.", "这个习惯可能已经被删除。"),
        )
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
                title = { Text(tr("Orbit detail", "习惯详情")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back", "返回"))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = tr("Edit", "编辑"))
                    }
                },
            )
        }
        item {
            DetailPanel(
                title = habit.name,
                lines = listOf(
                    tr("${habit.streak} day current streak", "当前连续 ${habit.streak} 天"),
                    tr("${habit.bestStreak} day best streak", "最长连续 ${habit.bestStreak} 天"),
                    tr("${detail!!.totalCompleted} total completions", "累计完成 ${detail!!.totalCompleted} 次"),
                    tr("${(habit.completionRate30d * 100).toInt()}% completion in 30 days", "近 30 天完成率 ${(habit.completionRate30d * 100).toInt()}%"),
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
                    label = { Text(if (habit.statusForDate == CheckInStatus.COMPLETED) tr("Undo", "撤销") else tr("Check in", "打卡")) },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.skipHabit(habit.id) },
                    label = { Text(tr("Skip", "跳过")) },
                )
                FilterChip(
                    selected = habit.statusForDate == CheckInStatus.FAILED,
                    onClick = { viewModel.failHabit(habit.id) },
                    label = { Text(tr("Fail", "失败")) },
                    leadingIcon = { Icon(Icons.Outlined.HeartBroken, null) },
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val sourceDate = detail!!.recentRecords.firstOrNull()?.date ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.completeHabitOn(
                                    habitId = habit.id,
                                    date = java.time.LocalDate.of(year, month + 1, day),
                                    isBackfilled = true,
                                )
                            },
                            sourceDate.year,
                            sourceDate.monthValue - 1,
                            sourceDate.dayOfMonth,
                        ).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    },
                    label = { Text(tr("Backfill", "补卡")) },
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        if (habit.archived) viewModel.restoreHabit(habit.id)
                        else viewModel.archiveHabit(habit.id)
                    },
                    label = { Text(if (habit.archived) tr("Restore", "恢复") else tr("Archive", "归档")) },
                    leadingIcon = {
                        Icon(
                            if (habit.archived) Icons.Outlined.Restore else Icons.Outlined.Archive,
                            null,
                        )
                    },
                )
                FilterChip(
                    selected = false,
                    onClick = { confirmDelete = true },
                    label = { Text(tr("Delete", "删除")) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                )
            }
        }
        item {
            DetailPanel(title = tr("Last 30 planned days", "最近 30 天计划"), content = {
                HeatmapGrid(records = detail!!.monthlyRecords)
            })
        }
        item {
            Text(tr("Recent records", "最近记录"), style = MaterialTheme.typography.headlineMedium)
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
                            checkInStatusLabel(record.status),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (record.isBackfilled) {
                            Text(
                                tr("Backfilled", "已补卡"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF6B938),
                            )
                        }
                    }
                    record.value?.let {
                        Text(it.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Delete habit?", "删除习惯？")) },
            text = { Text(tr("\"${habit.name}\" and its history will be removed.", "“${habit.name}”及其历史记录将被删除。")) },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text(tr("Cancel", "取消"))
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteHabit(habit.id)
                    confirmDelete = false
                    onBack()
                }) {
                    Text(tr("Delete", "删除"))
                }
            },
        )
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
