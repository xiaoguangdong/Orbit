package com.xiaoguangdong.orbit.ui.screens.habit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.outlined.AddAlarm
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiaoguangdong.orbit.domain.model.MilestoneDraft
import com.xiaoguangdong.orbit.domain.model.MilestoneType
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.milestoneTypeLabel
import com.xiaoguangdong.orbit.ui.tr
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MilestoneEditorScreen(
    viewModel: OrbitViewModel,
    milestoneId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(MilestoneDraft()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val titleRequiredText = tr("Milestone title is required.", "请输入里程碑标题。")
    val reminderLabel = draft.reminderTime?.let { tr("Reminder $it", "提醒 $it") } ?: tr("Add reminder", "添加提醒")

    LaunchedEffect(milestoneId) {
        if (milestoneId != null) {
            viewModel.getMilestoneDraft(milestoneId)?.let { draft = it }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text(if (milestoneId == null) tr("New milestone", "新建里程碑") else tr("Edit milestone", "编辑里程碑")) },
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
                label = { Text(tr("Title", "标题")) },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
            )
        }
        item {
            Text(tr("Type", "类型"), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MilestoneType.entries.forEach { type ->
                    FilterChip(
                        selected = draft.type == type,
                        onClick = { draft = draft.copy(type = type) },
                        label = { Text(milestoneTypeLabel(type)) },
                    )
                }
            }
        }
        item {
            DateField(tr("Date", "日期"), draft.date, onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        draft = draft.copy(date = LocalDate.of(year, month + 1, day))
                    },
                    draft.date.year,
                    draft.date.monthValue - 1,
                    draft.date.dayOfMonth,
                ).show()
            })
        }
        item {
            OutlinedTextField(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text(tr("Notes", "备注")) },
            )
        }
        item {
            Button(
                onClick = {
                    val now = LocalTime.now()
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            draft = draft.copy(reminderTime = LocalTime.of(hour, minute))
                        },
                        draft.reminderTime?.hour ?: now.hour,
                        draft.reminderTime?.minute ?: now.minute,
                        true,
                    ).show()
                },
            ) {
                Icon(Icons.Outlined.AddAlarm, contentDescription = null)
                Text(
                    text = reminderLabel,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item {
            Button(
                onClick = {
                    if (draft.title.isBlank()) error = titleRequiredText
                    else viewModel.saveMilestone(draft) { onBack() }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(tr("Save milestone", "保存里程碑"))
            }
        }
    }
}

@Composable
private fun DateField(label: String, value: LocalDate?, onClick: () -> Unit) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = onClick) {
                Icon(Icons.Outlined.Edit, contentDescription = label)
            }
        },
    )
}
