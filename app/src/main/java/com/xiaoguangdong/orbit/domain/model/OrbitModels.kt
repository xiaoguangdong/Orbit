package com.xiaoguangdong.orbit.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class HabitTimeBucket(val displayName: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    ANYTIME("Anytime"),
}

enum class FrequencyType(val displayName: String) {
    DAILY("Every day"),
    WEEKLY_DAYS("Specific weekdays"),
    WEEKLY_COUNT("Weekly count"),
    MONTHLY_DATES("Monthly dates"),
    INTERVAL("Every N days"),
}

enum class TargetType(val displayName: String) {
    NONE("One tap"),
    COUNT("Count"),
    MINUTES("Minutes"),
    AMOUNT("Amount"),
    DISTANCE("Distance"),
}

enum class GoalType(val displayName: String) {
    OPEN_ENDED("Keep going"),
    TARGET_DATE("Target date"),
    TARGET_COMPLETIONS("Target completions"),
}

enum class CheckInStatus(val displayName: String) {
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    FAILED("Failed"),
}

enum class MilestoneType(val displayName: String) {
    COUNTDOWN("Countdown"),
    SINCE("Since"),
    HABIT_GOAL("Habit goal"),
}

enum class TaskQuadrant(
    val displayName: String,
    val shortLabel: String,
    val colorHex: String,
) {
    IMPORTANT_URGENT("Important and urgent", "I", "#FF6B6B"),
    IMPORTANT_NOT_URGENT("Important not urgent", "II", "#F6B938"),
    NOT_IMPORTANT_URGENT("Not important but urgent", "III", "#4F7CFF"),
    NOT_IMPORTANT_NOT_URGENT("Not important not urgent", "IV", "#24C99A"),
}

enum class AppScreen {
    TODAY,
    ORBIT,
    INSIGHTS,
    MILESTONES,
    ME,
}

data class HabitDraft(
    val id: Long? = null,
    val name: String = "",
    val icon: String = "sparkle",
    val colorHex: String = "#2F6BFF",
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val selectedWeekDays: Set<DayOfWeek> = emptySet(),
    val weeklyTargetCount: Int = 3,
    val monthlyDays: Set<Int> = emptySet(),
    val intervalDays: Int = 2,
    val timeBucket: HabitTimeBucket = HabitTimeBucket.ANYTIME,
    val targetType: TargetType = TargetType.NONE,
    val targetValue: Double? = null,
    val unitLabel: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val goalType: GoalType = GoalType.OPEN_ENDED,
    val goalDate: LocalDate? = null,
    val goalCount: Int? = null,
    val reminderTimes: List<LocalTime> = emptyList(),
    val note: String = "",
    val customGroup: String = "",
)

data class MilestoneDraft(
    val id: Long? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val type: MilestoneType = MilestoneType.COUNTDOWN,
    val icon: String = "target",
    val colorHex: String = "#F6B938",
    val habitId: Long? = null,
    val reminderTime: LocalTime? = null,
    val note: String = "",
)

data class TaskDraft(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val quadrant: TaskQuadrant = TaskQuadrant.IMPORTANT_NOT_URGENT,
    val dueDate: LocalDate? = null,
    val isCompleted: Boolean = false,
)

data class HabitCardModel(
    val id: Long,
    val name: String,
    val icon: String,
    val colorHex: String,
    val bucket: HabitTimeBucket,
    val customGroup: String,
    val streak: Int,
    val bestStreak: Int,
    val completionRate30d: Float,
    val targetLabel: String?,
    val reminderLabel: String?,
    val statusForDate: CheckInStatus?,
    val isPlannedForDate: Boolean,
    val note: String,
    val archived: Boolean,
)

data class HabitDetailModel(
    val habit: HabitCardModel,
    val totalCompleted: Int,
    val monthlyRecords: List<DailyRecord>,
    val recentRecords: List<DailyRecord>,
)

data class DailyRecord(
    val date: LocalDate,
    val status: CheckInStatus?,
    val value: Double?,
    val note: String?,
    val isBackfilled: Boolean = false,
)

data class TodaySectionModel(
    val title: String,
    val habits: List<HabitCardModel>,
    val isCompletedSection: Boolean = false,
)

data class TodayOverview(
    val date: LocalDate,
    val plannedCount: Int,
    val completedCount: Int,
    val skippedCount: Int,
    val progress: Float,
    val activeStreaks: Int,
)

data class InsightsModel(
    val totalHabits: Int,
    val activeHabits: Int,
    val completedToday: Int,
    val plannedToday: Int,
    val weeklyCompletionRate: Float,
    val currentLeaders: List<HabitCardModel>,
    val longestLeaders: List<HabitCardModel>,
    val heatmap: List<DailyRecord>,
    val starCredits: Int,
)

data class MilestoneCardModel(
    val id: Long,
    val title: String,
    val icon: String,
    val colorHex: String,
    val type: MilestoneType,
    val date: LocalDate,
    val daysDelta: Long,
    val habitId: Long?,
    val note: String,
)

data class TaskCardModel(
    val id: Long,
    val title: String,
    val description: String,
    val quadrant: TaskQuadrant,
    val dueDate: LocalDate?,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?,
)

data class QuadrantSectionModel(
    val quadrant: TaskQuadrant,
    val tasks: List<TaskCardModel>,
)

data class TaskBoardModel(
    val sections: List<QuadrantSectionModel>,
    val activeCount: Int,
    val completedCount: Int,
)

data class OrbitSettings(
    val checkInSound: Boolean = true,
    val sortIncompleteFirst: Boolean = true,
    val showInSevenDayStrip: Boolean = true,
    val includeInBadgeCount: Boolean = true,
    val hideCompleted: Boolean = false,
    val compactView: Boolean = false,
    val weekStartsOnMonday: Boolean = true,
    val notificationsEnabled: Boolean = true,
)

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.asStorage(): String = format(dateFormatter)
fun LocalDateTime.asStorage(): String = format(dateTimeFormatter)
fun LocalTime.asStorage(): String = format(timeFormatter)

fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this, dateFormatter) }.getOrNull()
fun String.toLocalDateTimeOrNull(): LocalDateTime? = runCatching { LocalDateTime.parse(this, dateTimeFormatter) }.getOrNull()
fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this, timeFormatter) }.getOrNull()
