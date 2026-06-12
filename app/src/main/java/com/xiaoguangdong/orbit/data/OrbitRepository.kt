package com.xiaoguangdong.orbit.data

import com.xiaoguangdong.orbit.data.local.CheckInDao
import com.xiaoguangdong.orbit.data.local.CheckInEntity
import com.xiaoguangdong.orbit.data.local.HabitDao
import com.xiaoguangdong.orbit.data.local.HabitEntity
import com.xiaoguangdong.orbit.data.local.MilestoneDao
import com.xiaoguangdong.orbit.data.local.MilestoneEntity
import com.xiaoguangdong.orbit.data.local.TaskDao
import com.xiaoguangdong.orbit.data.local.TaskEntity
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.DailyRecord
import com.xiaoguangdong.orbit.domain.model.FrequencyType
import com.xiaoguangdong.orbit.domain.model.GoalType
import com.xiaoguangdong.orbit.domain.model.HabitCardModel
import com.xiaoguangdong.orbit.domain.model.HabitDetailModel
import com.xiaoguangdong.orbit.domain.model.HabitDraft
import com.xiaoguangdong.orbit.domain.model.HabitTimeBucket
import com.xiaoguangdong.orbit.domain.model.InsightsModel
import com.xiaoguangdong.orbit.domain.model.MilestoneCardModel
import com.xiaoguangdong.orbit.domain.model.MilestoneDraft
import com.xiaoguangdong.orbit.domain.model.MilestoneType
import com.xiaoguangdong.orbit.domain.model.OrbitSettings
import com.xiaoguangdong.orbit.domain.model.QuadrantSectionModel
import com.xiaoguangdong.orbit.domain.model.TargetType
import com.xiaoguangdong.orbit.domain.model.TaskBoardModel
import com.xiaoguangdong.orbit.domain.model.TaskCardModel
import com.xiaoguangdong.orbit.domain.model.TaskDraft
import com.xiaoguangdong.orbit.domain.model.TaskQuadrant
import com.xiaoguangdong.orbit.domain.model.TodayOverview
import com.xiaoguangdong.orbit.domain.model.TodaySectionModel
import com.xiaoguangdong.orbit.domain.model.asStorage
import com.xiaoguangdong.orbit.domain.model.toLocalDateOrNull
import com.xiaoguangdong.orbit.domain.model.toLocalDateTimeOrNull
import com.xiaoguangdong.orbit.domain.model.toLocalTimeOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

class OrbitRepository(
    private val habitDao: HabitDao,
    private val checkInDao: CheckInDao,
    private val milestoneDao: MilestoneDao,
    private val taskDao: TaskDao,
    private val settingsRepository: SettingsRepository,
) {
    val settings: Flow<OrbitSettings> = settingsRepository.settings

    fun observeActiveHabits(date: LocalDate): Flow<List<HabitCardModel>> =
        combine(habitDao.observeActiveHabits(), checkInDao.observeAllCheckIns()) { habits, checkIns ->
            habits.map { it.toCardModel(checkIns, date) }
        }

    fun observeArchivedHabits(date: LocalDate): Flow<List<HabitCardModel>> =
        combine(habitDao.observeArchivedHabits(), checkInDao.observeAllCheckIns()) { habits, checkIns ->
            habits.map { it.toCardModel(checkIns, date) }
        }

    fun observeTodaySections(date: LocalDate): Flow<List<TodaySectionModel>> =
        combine(observeActiveHabits(date), settings) { habits, settings ->
            val planned = habits.filter { it.isPlannedForDate }
            val completed = planned.filter { it.statusForDate == CheckInStatus.COMPLETED }
            val skipped = planned.filter { it.statusForDate == CheckInStatus.SKIPPED }
            val pending = planned.filter { it.statusForDate == null || it.statusForDate == CheckInStatus.FAILED }
            val sections = buildList {
                HabitTimeBucket.entries.forEach { bucket ->
                    val bucketHabits = pending.filter { it.bucket == bucket }
                    if (bucketHabits.isNotEmpty()) {
                        add(TodaySectionModel(title = bucket.displayName, habits = bucketHabits))
                    }
                }
                if (!settings.hideCompleted && (completed.isNotEmpty() || skipped.isNotEmpty())) {
                    add(
                        TodaySectionModel(
                            title = "Completed",
                            habits = completed + skipped,
                            isCompletedSection = true,
                        ),
                    )
                }
            }
            sections
        }

    fun observeTodayOverview(date: LocalDate): Flow<TodayOverview> =
        combine(observeActiveHabits(date), settings) { habits, _ ->
            val planned = habits.count { it.isPlannedForDate }
            val completed = habits.count { it.isPlannedForDate && it.statusForDate == CheckInStatus.COMPLETED }
            val skipped = habits.count { it.isPlannedForDate && it.statusForDate == CheckInStatus.SKIPPED }
            TodayOverview(
                date = date,
                plannedCount = planned,
                completedCount = completed,
                skippedCount = skipped,
                progress = if (planned == 0) 0f else completed.toFloat() / planned.toFloat(),
                activeStreaks = habits.count { it.streak > 0 },
            )
        }

    fun observeHabitDetail(habitId: Long, date: LocalDate): Flow<HabitDetailModel?> =
        combine(
            habitDao.observeHabitById(habitId),
            checkInDao.observeHabitCheckIns(habitId),
        ) { habit, checkIns ->
            habit?.let {
                val card = it.toCardModel(checkIns, date)
                HabitDetailModel(
                    habit = card,
                    totalCompleted = checkIns.count { entry -> entry.status == CheckInStatus.COMPLETED.name },
                    monthlyRecords = buildDateWindow(date.minusDays(29), date).map { entryDate ->
                        val record = checkIns.firstOrNull { item -> item.date == entryDate.asStorage() }
                        record.toDailyRecord(entryDate)
                    },
                    recentRecords = checkIns
                        .sortedByDescending { entry -> entry.date }
                        .take(20)
                        .mapNotNull { entry -> entry.date.toLocalDateOrNull()?.let { entryDate -> entry.toDailyRecord(entryDate) } },
                )
            }
        }

    fun observeInsights(date: LocalDate): Flow<InsightsModel> =
        combine(habitDao.observeActiveHabits(), checkInDao.observeAllCheckIns()) { habits, checkIns ->
            val cards = habits.map { it.toCardModel(checkIns, date) }
            val plannedToday = cards.count { it.isPlannedForDate }
            val completedToday = cards.count { it.statusForDate == CheckInStatus.COMPLETED && it.isPlannedForDate }
            val weekStart = date.minusDays(6)
            val weekRecords = buildDateWindow(weekStart, date)
            val totalWeeklyPlanned = cards.sumOf { card ->
                weekRecords.count { day -> isHabitPlannedForDate(habits.first { it.id == card.id }, day) }
            }
            val completedWeekly = checkIns.count { entry ->
                val entryDate = entry.date.toLocalDateOrNull()
                entry.status == CheckInStatus.COMPLETED.name && entryDate != null && !entryDate.isBefore(weekStart) && !entryDate.isAfter(date)
            }
            val heatmapDates = buildDateWindow(date.minusDays(59), date)
            val starCredits = checkIns.sumOf { entry ->
                when (entry.status) {
                    CheckInStatus.COMPLETED.name -> if (entry.isBackfilled) 3 else 10
                    else -> 0
                }.toInt()
            }
            InsightsModel(
                totalHabits = cards.size,
                activeHabits = cards.count { !it.archived },
                completedToday = completedToday,
                plannedToday = plannedToday,
                weeklyCompletionRate = if (totalWeeklyPlanned == 0) 0f else completedWeekly.toFloat() / totalWeeklyPlanned.toFloat(),
                currentLeaders = cards.sortedByDescending { it.streak }.take(5),
                longestLeaders = cards.sortedByDescending { it.bestStreak }.take(5),
                heatmap = heatmapDates.map { entryDate ->
                    val record = checkIns.firstOrNull { item -> item.date == entryDate.asStorage() }
                    record.toDailyRecord(entryDate)
                },
                starCredits = starCredits,
            )
        }

    fun observeMilestones(date: LocalDate): Flow<List<MilestoneCardModel>> =
        milestoneDao.observeMilestones().combine(habitDao.observeActiveHabits()) { milestones, habits ->
            milestones.map { milestone ->
                val milestoneDate = milestone.date.toLocalDateOrNull() ?: date
                val daysDelta = when (MilestoneType.valueOf(milestone.type)) {
                    MilestoneType.COUNTDOWN,
                    MilestoneType.HABIT_GOAL -> ChronoUnit.DAYS.between(date, milestoneDate)
                    MilestoneType.SINCE -> ChronoUnit.DAYS.between(milestoneDate, date)
                }
                MilestoneCardModel(
                    id = milestone.id,
                    title = milestone.title,
                    icon = milestone.icon,
                    colorHex = milestone.colorHex,
                    type = MilestoneType.valueOf(milestone.type),
                    date = milestoneDate,
                    daysDelta = daysDelta,
                    habitId = milestone.habitId?.takeIf { id -> habits.any { it.id == id } },
                    note = milestone.note,
                )
            }
        }

    fun observeTaskBoard(): Flow<TaskBoardModel> =
        taskDao.observeTasks().combine(settings) { tasks, settings ->
            val cards = tasks.map { it.toCardModel() }
            TaskBoardModel(
                sections = TaskQuadrant.entries.map { quadrant ->
                    QuadrantSectionModel(
                        quadrant = quadrant,
                        tasks = cards.filter { task ->
                            task.quadrant == quadrant && (!task.isCompleted || !settings.hideCompleted)
                        },
                    )
                },
                activeCount = cards.count { !it.isCompleted },
                completedCount = cards.count { it.isCompleted },
            )
        }

    suspend fun upsertHabit(draft: HabitDraft): Long {
        val now = LocalDateTime.now().asStorage()
        val existing = draft.id?.let { habitDao.getHabitById(it) }
        val entity = HabitEntity(
            id = draft.id ?: 0,
            name = draft.name.trim(),
            icon = draft.icon,
            colorHex = draft.colorHex,
            frequencyType = draft.frequencyType.name,
            selectedWeekDays = draft.selectedWeekDays.joinToString(",") { it.name },
            weeklyTargetCount = draft.weeklyTargetCount,
            monthlyDays = draft.monthlyDays.sorted().joinToString(","),
            intervalDays = max(1, draft.intervalDays),
            timeBucket = draft.timeBucket.name,
            targetType = draft.targetType.name,
            targetValue = draft.targetValue,
            unitLabel = draft.unitLabel.trim(),
            startDate = draft.startDate.asStorage(),
            endDate = draft.endDate?.asStorage(),
            goalType = draft.goalType.name,
            goalDate = draft.goalDate?.asStorage(),
            goalCount = draft.goalCount,
            reminderTimes = draft.reminderTimes.sorted().joinToString(",") { it.asStorage() },
            note = draft.note.trim(),
            customGroup = draft.customGroup.trim(),
            archived = existing?.archived ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return habitDao.upsertHabit(entity)
    }

    suspend fun archiveHabit(habitId: Long) {
        habitDao.setArchived(habitId, archived = true, updatedAt = LocalDateTime.now().asStorage())
    }

    suspend fun restoreHabit(habitId: Long) {
        habitDao.setArchived(habitId, archived = false, updatedAt = LocalDateTime.now().asStorage())
    }

    suspend fun deleteHabit(habitId: Long) {
        checkInDao.deleteCheckInsForHabit(habitId)
        habitDao.deleteHabit(habitId)
    }

    suspend fun upsertMilestone(draft: MilestoneDraft): Long {
        val now = LocalDateTime.now().asStorage()
        val existing = draft.id?.let { milestoneDao.getMilestoneById(it) }
        val entity = MilestoneEntity(
            id = draft.id ?: 0,
            title = draft.title.trim(),
            date = draft.date.asStorage(),
            type = draft.type.name,
            icon = draft.icon,
            colorHex = draft.colorHex,
            habitId = draft.habitId,
            reminderTime = draft.reminderTime?.asStorage(),
            note = draft.note.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return milestoneDao.upsertMilestone(entity)
    }

    suspend fun deleteMilestone(milestoneId: Long) {
        milestoneDao.deleteMilestone(milestoneId)
    }

    suspend fun upsertTask(draft: TaskDraft): Long {
        val now = LocalDateTime.now().asStorage()
        val existing = draft.id?.let { taskDao.getTaskById(it) }
        val entity = TaskEntity(
            id = draft.id ?: 0,
            title = draft.title.trim(),
            description = draft.description.trim(),
            quadrant = draft.quadrant.name,
            dueDate = draft.dueDate?.asStorage(),
            isCompleted = existing?.isCompleted ?: draft.isCompleted,
            completedAt = existing?.completedAt,
            sortOrder = existing?.sortOrder ?: ((taskDao.getTasksOnce().maxOfOrNull { it.sortOrder } ?: 0) + 1),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return taskDao.upsertTask(entity)
    }

    suspend fun getTaskDraft(taskId: Long): TaskDraft? = taskDao.getTaskById(taskId)?.toDraft()

    suspend fun toggleTaskCompleted(taskId: Long) {
        val existing = taskDao.getTaskById(taskId) ?: return
        val isCompleted = !existing.isCompleted
        taskDao.setTaskCompleted(
            taskId = taskId,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) LocalDateTime.now().asStorage() else null,
            updatedAt = LocalDateTime.now().asStorage(),
        )
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTask(taskId)
    }

    suspend fun markCompleted(habitId: Long, date: LocalDate, value: Double? = null, note: String? = null, isBackfilled: Boolean = false) {
        upsertCheckIn(habitId, date, CheckInStatus.COMPLETED, value, note, isBackfilled)
    }

    suspend fun markSkipped(habitId: Long, date: LocalDate, note: String? = null) {
        upsertCheckIn(habitId, date, CheckInStatus.SKIPPED, null, note, false)
    }

    suspend fun markFailed(habitId: Long, date: LocalDate, note: String? = null) {
        upsertCheckIn(habitId, date, CheckInStatus.FAILED, null, note, false)
    }

    suspend fun undoCheckIn(habitId: Long, date: LocalDate) {
        checkInDao.deleteCheckInForDate(habitId, date.asStorage())
    }

    suspend fun getHabitDraft(habitId: Long): HabitDraft? =
        habitDao.getHabitById(habitId)?.toDraft()

    suspend fun getMilestoneDraft(milestoneId: Long): MilestoneDraft? =
        milestoneDao.getMilestoneById(milestoneId)?.toDraft()

    suspend fun getActiveHabitsOnce(): List<HabitEntity> = habitDao.getActiveHabitsOnce()

    suspend fun getSettingsOnce(): OrbitSettings {
        return settings.first()
    }

    suspend fun updateSettings(transform: OrbitSettings.() -> OrbitSettings) {
        settingsRepository.updateSettings(transform)
    }

    suspend fun getCheckInStatus(habitId: Long, date: LocalDate): CheckInStatus? =
        checkInDao.getCheckInForDate(habitId, date.asStorage())?.status?.let(CheckInStatus::valueOf)

    private suspend fun upsertCheckIn(
        habitId: Long,
        date: LocalDate,
        status: CheckInStatus,
        value: Double?,
        note: String?,
        isBackfilled: Boolean,
    ) {
        val now = LocalDateTime.now().asStorage()
        val existing = checkInDao.getCheckInForDate(habitId, date.asStorage())
        val entity = CheckInEntity(
            id = existing?.id ?: 0,
            habitId = habitId,
            date = date.asStorage(),
            status = status.name,
            value = value,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            isBackfilled = isBackfilled,
            checkedAt = now,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        checkInDao.upsertCheckIn(entity)
    }
}

private fun HabitEntity.toCardModel(checkIns: List<CheckInEntity>, date: LocalDate): HabitCardModel {
    val habitCheckIns = checkIns.filter { it.habitId == id }.sortedByDescending { it.date }
    val streak = calculateCurrentStreak(this, habitCheckIns, date)
    val bestStreak = calculateBestStreak(this, habitCheckIns, date)
    val completionRate = calculateCompletionRate30d(this, habitCheckIns, date)
    val statusForDate = habitCheckIns.firstOrNull { it.date == date.asStorage() }?.status?.let(CheckInStatus::valueOf)
    return HabitCardModel(
        id = id,
        name = name,
        icon = icon,
        colorHex = colorHex,
        bucket = HabitTimeBucket.valueOf(timeBucket),
        customGroup = customGroup,
        streak = streak,
        bestStreak = bestStreak,
        completionRate30d = completionRate,
        targetLabel = buildTargetLabel(),
        reminderLabel = reminderTimes.split(",").filter { it.isNotBlank() }.firstOrNull(),
        statusForDate = statusForDate,
        isPlannedForDate = isHabitPlannedForDate(this, date),
        note = note,
        archived = archived,
    )
}

private fun HabitEntity.buildTargetLabel(): String? {
    val type = TargetType.valueOf(targetType)
    if (type == TargetType.NONE || targetValue == null) return null
    return "${targetValue.toInt()} ${unitLabel.ifBlank { type.displayName.lowercase() }}"
}

private fun CheckInEntity?.toDailyRecord(date: LocalDate): DailyRecord =
    if (this == null) {
        DailyRecord(date = date, status = null, value = null, note = null)
    } else {
        DailyRecord(
            date = date,
            status = CheckInStatus.valueOf(status),
            value = value,
            note = note,
            isBackfilled = isBackfilled,
        )
    }

private fun HabitEntity.toDraft(): HabitDraft = HabitDraft(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    frequencyType = FrequencyType.valueOf(frequencyType),
    selectedWeekDays = selectedWeekDays.split(",").mapNotNull {
        if (it.isBlank()) null else runCatching { DayOfWeek.valueOf(it) }.getOrNull()
    }.toSet(),
    weeklyTargetCount = weeklyTargetCount,
    monthlyDays = monthlyDays.split(",").mapNotNull { it.toIntOrNull() }.toSet(),
    intervalDays = intervalDays,
    timeBucket = HabitTimeBucket.valueOf(timeBucket),
    targetType = TargetType.valueOf(targetType),
    targetValue = targetValue,
    unitLabel = unitLabel,
    startDate = startDate.toLocalDateOrNull() ?: LocalDate.now(),
    endDate = endDate?.toLocalDateOrNull(),
    goalType = GoalType.valueOf(goalType),
    goalDate = goalDate?.toLocalDateOrNull(),
    goalCount = goalCount,
    reminderTimes = reminderTimes.split(",").mapNotNull { it.toLocalTimeOrNull() },
    note = note,
    customGroup = customGroup,
)

private fun MilestoneEntity.toDraft(): MilestoneDraft = MilestoneDraft(
    id = id,
    title = title,
    date = date.toLocalDateOrNull() ?: LocalDate.now(),
    type = MilestoneType.valueOf(type),
    icon = icon,
    colorHex = colorHex,
    habitId = habitId,
    reminderTime = reminderTime?.toLocalTimeOrNull(),
    note = note,
)

private fun TaskEntity.toCardModel(): TaskCardModel = TaskCardModel(
    id = id,
    title = title,
    description = description,
    quadrant = TaskQuadrant.valueOf(quadrant),
    dueDate = dueDate?.toLocalDateOrNull(),
    isCompleted = isCompleted,
    completedAt = completedAt?.toLocalDateTimeOrNull(),
)

private fun TaskEntity.toDraft(): TaskDraft = TaskDraft(
    id = id,
    title = title,
    description = description,
    quadrant = TaskQuadrant.valueOf(quadrant),
    dueDate = dueDate?.toLocalDateOrNull(),
    isCompleted = isCompleted,
)

private fun calculateCompletionRate30d(habit: HabitEntity, checkIns: List<CheckInEntity>, today: LocalDate): Float {
    val start = today.minusDays(29)
    val plannedDates = buildDateWindow(start, today).filter { date -> isHabitPlannedForDate(habit, date) }
    if (plannedDates.isEmpty()) return 0f
    val completedDates = checkIns.filter { it.status == CheckInStatus.COMPLETED.name }.mapNotNull { it.date.toLocalDateOrNull() }.toSet()
    return plannedDates.count { it in completedDates }.toFloat() / plannedDates.size.toFloat()
}

private fun calculateCurrentStreak(habit: HabitEntity, checkIns: List<CheckInEntity>, today: LocalDate): Int {
    var streak = 0
    var cursor = today
    while (!cursor.isBefore(habit.startDate.toLocalDateOrNull() ?: today.minusYears(5))) {
        if (!isHabitPlannedForDate(habit, cursor)) {
            cursor = cursor.minusDays(1)
            continue
        }
        val record = checkIns.firstOrNull { it.date == cursor.asStorage() }
        when (record?.status?.let(CheckInStatus::valueOf)) {
            CheckInStatus.COMPLETED -> streak += 1
            CheckInStatus.SKIPPED -> {}
            else -> break
        }
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun calculateBestStreak(habit: HabitEntity, checkIns: List<CheckInEntity>, today: LocalDate): Int {
    val dates = buildDateWindow(habit.startDate.toLocalDateOrNull() ?: today.minusDays(120), today)
    var best = 0
    var current = 0
    dates.forEach { date ->
        if (!isHabitPlannedForDate(habit, date)) {
            return@forEach
        }
        val record = checkIns.firstOrNull { it.date == date.asStorage() }
        when (record?.status?.let(CheckInStatus::valueOf)) {
            CheckInStatus.COMPLETED -> {
                current += 1
                best = max(best, current)
            }
            CheckInStatus.SKIPPED -> {}
            else -> current = 0
        }
    }
    return best
}

fun isHabitPlannedForDate(habit: HabitEntity, date: LocalDate): Boolean {
    val startDate = habit.startDate.toLocalDateOrNull() ?: LocalDate.now()
    val endDate = habit.endDate?.toLocalDateOrNull()
    if (date.isBefore(startDate)) return false
    if (endDate != null && date.isAfter(endDate)) return false

    return when (FrequencyType.valueOf(habit.frequencyType)) {
        FrequencyType.DAILY -> true
        FrequencyType.WEEKLY_DAYS -> {
            val set = habit.selectedWeekDays.split(",").mapNotNull {
                runCatching { DayOfWeek.valueOf(it) }.getOrNull()
            }.toSet()
            date.dayOfWeek in set
        }
        FrequencyType.WEEKLY_COUNT -> {
            val days = habit.selectedWeekDays.split(",").mapNotNull {
                runCatching { DayOfWeek.valueOf(it) }.getOrNull()
            }.toSet()
            if (days.isNotEmpty()) date.dayOfWeek in days else date.dayOfWeek.value <= habit.weeklyTargetCount
        }
        FrequencyType.MONTHLY_DATES -> {
            val dates = habit.monthlyDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            date.dayOfMonth in dates
        }
        FrequencyType.INTERVAL -> {
            val diff = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
            diff % max(1, habit.intervalDays).toLong() == 0L
        }
    }
}

private fun buildDateWindow(start: LocalDate, end: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var cursor = start
    while (!cursor.isAfter(end)) {
        dates += cursor
        cursor = cursor.plusDays(1)
    }
    return dates
}
