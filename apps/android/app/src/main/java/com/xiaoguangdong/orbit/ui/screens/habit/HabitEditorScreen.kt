package com.xiaoguangdong.orbit.ui.screens.habit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import com.xiaoguangdong.orbit.domain.model.FrequencyType
import com.xiaoguangdong.orbit.domain.model.GoalType
import com.xiaoguangdong.orbit.domain.model.HabitDraft
import com.xiaoguangdong.orbit.domain.model.HabitTimeBucket
import com.xiaoguangdong.orbit.domain.model.TargetType
import com.xiaoguangdong.orbit.ui.OrbitViewModel
import com.xiaoguangdong.orbit.ui.bucketLabel
import com.xiaoguangdong.orbit.ui.frequencyLabel
import com.xiaoguangdong.orbit.ui.goalTypeLabel
import com.xiaoguangdong.orbit.ui.targetTypeLabel
import com.xiaoguangdong.orbit.ui.tr
import com.xiaoguangdong.orbit.ui.weekdayLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    viewModel: OrbitViewModel,
    habitId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(HabitDraft()) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    val habitNameRequiredText = tr("Habit name is required.", "请输入习惯名称。")

    LaunchedEffect(habitId) {
        if (habitId != null) {
            viewModel.getHabitDraft(habitId)?.let { draft = it }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text(if (habitId == null) tr("New orbit", "新建习惯") else tr("Edit orbit", "编辑习惯")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back", "返回"))
                    }
                },
            )
        }
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = {
                    draft = draft.copy(name = it)
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Habit name", "习惯名称")) },
                isError = errorText != null,
                supportingText = { errorText?.let { Text(it) } },
            )
        }
        item {
            Text(tr("Icon", "图标"), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("sparkle", "run", "read", "journal", "sleep", "call", "water", "mind", "meditate").forEach { icon ->
                    FilterChip(
                        selected = draft.icon == icon,
                        onClick = { draft = draft.copy(icon = icon) },
                        label = { Text(icon.replaceFirstChar(Char::titlecase)) },
                    )
                }
            }
        }
        item {
            Text(tr("Color", "颜色"), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("#2F6BFF", "#24C99A", "#F6B938", "#FF6B6B", "#8B5CF6", "#0EA5E9").forEach { color ->
                    FilterChip(
                        selected = draft.colorHex == color,
                        onClick = { draft = draft.copy(colorHex = color) },
                        label = { Text(color.removePrefix("#")) },
                    )
                }
            }
        }
        item {
            EnumChooser(
                label = tr("Frequency", "频率"),
                options = FrequencyType.entries,
                selected = draft.frequencyType,
                labelFor = { frequencyLabel(it) },
            ) {
                draft = draft.copy(frequencyType = it)
            }
        }
        when (draft.frequencyType) {
            FrequencyType.WEEKLY_DAYS -> item {
                WeekdayPicker(draft.selectedWeekDays) { selected ->
                    draft = draft.copy(selectedWeekDays = selected)
                }
            }

            FrequencyType.WEEKLY_COUNT -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.weeklyTargetCount.toString(),
                        onValueChange = { value ->
                            draft = draft.copy(weeklyTargetCount = value.toIntOrNull()?.coerceIn(1, 7) ?: 1)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Times per week", "每周次数")) },
                        supportingText = { Text(tr("Optionally pin the preferred weekdays below.", "可选指定偏好的星期几。")) },
                    )
                    WeekdayPicker(draft.selectedWeekDays) { selected ->
                        draft = draft.copy(selectedWeekDays = selected)
                    }
                }
            }

            FrequencyType.MONTHLY_DATES -> item {
                OutlinedTextField(
                    value = draft.monthlyDays.sorted().joinToString(","),
                    onValueChange = {
                        draft = draft.copy(
                            monthlyDays = it.split(",").mapNotNull(String::toIntOrNull).filter { day -> day in 1..31 }.toSet(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Monthly dates", "每月日期")) },
                    supportingText = { Text(tr("Example: 1,15,30", "例如：1,15,30")) },
                )
            }

            FrequencyType.INTERVAL -> item {
                OutlinedTextField(
                    value = draft.intervalDays.toString(),
                    onValueChange = { value ->
                        draft = draft.copy(intervalDays = value.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Every N days", "每隔 N 天")) },
                )
            }

            FrequencyType.DAILY -> Unit
        }
        item {
            EnumChooser(
                label = tr("Time bucket", "时间段"),
                options = HabitTimeBucket.entries,
                selected = draft.timeBucket,
                labelFor = { bucketLabel(it) },
            ) {
                draft = draft.copy(timeBucket = it)
            }
        }
        item {
            EnumChooser(
                label = tr("Target type", "目标类型"),
                options = TargetType.entries,
                selected = draft.targetType,
                labelFor = { targetTypeLabel(it) },
            ) {
                draft = draft.copy(targetType = it)
            }
        }
        if (draft.targetType != TargetType.NONE) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft.targetValue?.toInt()?.toString().orEmpty(),
                        onValueChange = { value ->
                            draft = draft.copy(targetValue = value.toDoubleOrNull())
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(tr("Target value", "目标值")) },
                    )
                    OutlinedTextField(
                        value = draft.unitLabel,
                        onValueChange = { draft = draft.copy(unitLabel = it) },
                        modifier = Modifier.weight(1f),
                        label = { Text(tr("Unit label", "单位")) },
                    )
                }
            }
        }
        item {
            EnumChooser(
                label = tr("Goal", "目标"),
                options = GoalType.entries,
                selected = draft.goalType,
                labelFor = { goalTypeLabel(it) },
            ) {
                draft = draft.copy(goalType = it)
            }
        }
        if (draft.goalType == GoalType.TARGET_DATE) {
            item {
                DateField(tr("Goal date", "目标日期"), draft.goalDate, onClick = {
                    val sourceDate = draft.goalDate ?: LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            draft = draft.copy(goalDate = LocalDate.of(year, month + 1, day))
                        },
                        sourceDate.year,
                        sourceDate.monthValue - 1,
                        sourceDate.dayOfMonth,
                    ).show()
                })
            }
        }
        if (draft.goalType == GoalType.TARGET_COMPLETIONS) {
            item {
                OutlinedTextField(
                    value = draft.goalCount?.toString().orEmpty(),
                    onValueChange = { draft = draft.copy(goalCount = it.toIntOrNull()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Target completions", "目标次数")) },
                )
            }
        }
        item {
            DateField(tr("Start date", "开始日期"), draft.startDate, onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        draft = draft.copy(startDate = LocalDate.of(year, month + 1, day))
                    },
                    draft.startDate.year,
                    draft.startDate.monthValue - 1,
                    draft.startDate.dayOfMonth,
                ).show()
            })
        }
        item {
            Text(tr("Reminders", "提醒"), style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.reminderTimes.forEach { reminderTime ->
                    FilterChip(
                        selected = true,
                        onClick = { draft = draft.copy(reminderTimes = draft.reminderTimes - reminderTime) },
                        label = { Text(reminderTime.toString()) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        val now = LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                draft = draft.copy(reminderTimes = (draft.reminderTimes + LocalTime.of(hour, minute)).distinct().sorted())
                            },
                            now.hour,
                            now.minute,
                            true,
                        ).show()
                    },
                    label = { Text(tr("Add", "添加")) },
                    leadingIcon = { Icon(Icons.Outlined.Add, null) },
                )
            }
        }
        item {
            OutlinedTextField(
                value = draft.customGroup,
                onValueChange = { draft = draft.copy(customGroup = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Custom group", "自定义分组")) },
            )
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
                    if (draft.name.isBlank()) {
                        errorText = habitNameRequiredText
                    } else {
                        viewModel.saveHabit(draft) { onBack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(tr("Save orbit", "保存习惯"))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayPicker(
    selectedDays: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Weekdays", "星期"), style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        val next = if (day in selectedDays) selectedDays - day else selectedDays + day
                        onChange(next)
                    },
                    label = { Text(weekdayLabel(day)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EnumChooser(
    label: String,
    options: Iterable<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(labelFor(option)) },
                )
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
