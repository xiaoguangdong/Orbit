package com.xiaoguangdong.orbit.ui.screens.task

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiaoguangdong.orbit.domain.model.TaskDraft
import com.xiaoguangdong.orbit.domain.model.TaskQuadrant
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.taskQuadrantLabel
import com.xiaoguangdong.orbit.ui.tr
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: OrbitViewModel,
    taskId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(TaskDraft()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val titleRequiredText = tr("Task title is required.", "请输入任务标题。")

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getTaskDraft(taskId)?.let { draft = it }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text(if (taskId == null) tr("New task", "新建任务") else tr("Edit task", "编辑任务")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back", "返回"))
                    }
                },
            )
        }
        item {
            OutlinedTextField(
                value = draft.title,
                onValueChange = {
                    draft = draft.copy(title = it)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Task title", "任务标题")) },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Description", "描述")) },
                minLines = 3,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Quadrant", "象限"), style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskQuadrant.entries.forEach { quadrant ->
                        val accent = Color(android.graphics.Color.parseColor(quadrant.colorHex))
                        FilterChip(
                            selected = draft.quadrant == quadrant,
                            onClick = { draft = draft.copy(quadrant = quadrant) },
                            label = { Text("${quadrant.shortLabel}. ${taskQuadrantLabel(quadrant)}") },
                            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                borderColor = accent.copy(alpha = 0.4f),
                                selectedBorderColor = accent,
                                enabled = true,
                                selected = draft.quadrant == quadrant,
                            ),
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.dueDate?.toString().orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text(tr("Due date", "截止日期")) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val sourceDate = draft.dueDate ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    draft = draft.copy(dueDate = LocalDate.of(year, month + 1, day))
                                },
                                sourceDate.year,
                                sourceDate.monthValue - 1,
                                sourceDate.dayOfMonth,
                            ).show()
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = tr("Due date", "截止日期"))
                        }
                    },
                    supportingText = { Text(tr("Leave empty to keep it unscheduled.", "留空表示暂不安排。")) },
                )
                if (draft.dueDate != null) {
                    FilterChip(
                        selected = false,
                        onClick = { draft = draft.copy(dueDate = null) },
                        label = { Text(tr("Clear due date", "清除截止日期")) },
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (draft.title.isBlank()) {
                        error = titleRequiredText
                    } else {
                        viewModel.saveTask(draft) { onBack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(tr("Save task", "保存任务"))
            }
        }
    }
}
