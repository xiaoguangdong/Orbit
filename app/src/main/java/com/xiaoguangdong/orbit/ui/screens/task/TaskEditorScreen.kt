package com.xiaoguangdong.orbit.ui.screens.task

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: OrbitViewModel,
    taskId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(TaskDraft()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

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
                title = { Text(if (taskId == null) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                label = { Text("Task title") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
            )
        }
        item {
            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 3,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quadrant", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskQuadrant.entries.forEach { quadrant ->
                        val accent = Color(android.graphics.Color.parseColor(quadrant.colorHex))
                        FilterChip(
                            selected = draft.quadrant == quadrant,
                            onClick = { draft = draft.copy(quadrant = quadrant) },
                            label = { Text("${quadrant.shortLabel}. ${quadrant.displayName}") },
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
            OutlinedTextField(
                value = draft.dueDate?.toString().orEmpty(),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text("Due date") },
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
                        Icon(Icons.Outlined.Edit, contentDescription = "Due date")
                    }
                },
                supportingText = { Text("Leave empty to keep it unscheduled.") },
            )
        }
        item {
            Button(
                onClick = {
                    if (draft.title.isBlank()) {
                        error = "Task title is required."
                    } else {
                        viewModel.saveTask(draft) { onBack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save task")
            }
        }
    }
}
