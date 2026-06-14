package com.xiaoguangdong.orbit.data

import com.xiaoguangdong.orbit.data.local.CheckInEntity
import com.xiaoguangdong.orbit.data.local.HabitEntity
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.OrbitSettings
import com.xiaoguangdong.orbit.domain.model.TaskCardModel
import com.xiaoguangdong.orbit.domain.model.TaskDueState
import com.xiaoguangdong.orbit.domain.model.TaskQuadrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class OrbitRepositoryLogicTest {
    @Test
    fun `weekly count without pinned weekdays uses first weekdays of week`() {
        val habit = baseHabit(
            frequencyType = "WEEKLY_COUNT",
            weeklyTargetCount = 3,
            selectedWeekDays = "",
        )

        assertTrue(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 8)))
        assertTrue(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 10)))
        assertFalse(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 12)))
    }

    @Test
    fun `interval habit only plans matching cadence`() {
        val habit = baseHabit(
            frequencyType = "INTERVAL",
            intervalDays = 2,
            startDate = "2026-06-01",
        )

        assertTrue(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 1)))
        assertFalse(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 2)))
        assertTrue(isHabitPlannedForDate(habit, LocalDate.of(2026, 6, 3)))
    }

    @Test
    fun `current streak ignores skipped days but breaks on missing planned day`() {
        val habit = baseHabit()
        val checkIns = listOf(
            checkIn(date = "2026-06-12", status = CheckInStatus.COMPLETED),
            checkIn(date = "2026-06-11", status = CheckInStatus.SKIPPED),
            checkIn(date = "2026-06-10", status = CheckInStatus.COMPLETED),
        )

        assertEquals(2, calculateCurrentStreak(habit, checkIns, LocalDate.of(2026, 6, 12)))
        assertEquals(0, calculateCurrentStreak(habit, checkIns, LocalDate.of(2026, 6, 13)))
    }

    @Test
    fun `best streak counts longest completed run across planned days`() {
        val habit = baseHabit()
        val checkIns = listOf(
            checkIn(date = "2026-06-01", status = CheckInStatus.COMPLETED),
            checkIn(date = "2026-06-02", status = CheckInStatus.COMPLETED),
            checkIn(date = "2026-06-03", status = CheckInStatus.FAILED),
            checkIn(date = "2026-06-04", status = CheckInStatus.COMPLETED),
            checkIn(date = "2026-06-05", status = CheckInStatus.COMPLETED),
            checkIn(date = "2026-06-06", status = CheckInStatus.COMPLETED),
        )

        assertEquals(3, calculateBestStreak(habit, checkIns, LocalDate.of(2026, 6, 6)))
    }

    @Test
    fun `task due state reflects overdue today upcoming and someday`() {
        val today = LocalDate.of(2026, 6, 12)

        assertEquals(TaskDueState.OVERDUE, calculateTaskDueState(LocalDate.of(2026, 6, 11), false, today))
        assertEquals(TaskDueState.TODAY, calculateTaskDueState(LocalDate.of(2026, 6, 12), false, today))
        assertEquals(TaskDueState.UPCOMING, calculateTaskDueState(LocalDate.of(2026, 6, 13), false, today))
        assertEquals(TaskDueState.SOMEDAY, calculateTaskDueState(null, false, today))
        assertEquals(TaskDueState.SOMEDAY, calculateTaskDueState(LocalDate.of(2026, 6, 11), true, today))
    }

    @Test
    fun `task board sorting keeps incomplete urgent work before completed tasks`() {
        val tasks = listOf(
            task(title = "Completed", dueState = TaskDueState.SOMEDAY, isCompleted = true),
            task(title = "Upcoming", dueState = TaskDueState.UPCOMING),
            task(title = "Overdue", dueState = TaskDueState.OVERDUE),
            task(title = "Today", dueState = TaskDueState.TODAY),
        )

        val sorted = sortTasksForBoard(tasks, OrbitSettings(sortIncompleteFirst = true))

        assertEquals(listOf("Overdue", "Today", "Upcoming", "Completed"), sorted.map { it.title })
    }

    private fun baseHabit(
        frequencyType: String = "DAILY",
        selectedWeekDays: String = "",
        weeklyTargetCount: Int = 3,
        intervalDays: Int = 1,
        startDate: String = "2026-06-01",
    ) = HabitEntity(
        id = 1,
        name = "Read",
        icon = "read",
        colorHex = "#2F6BFF",
        frequencyType = frequencyType,
        selectedWeekDays = selectedWeekDays,
        weeklyTargetCount = weeklyTargetCount,
        monthlyDays = "",
        intervalDays = intervalDays,
        timeBucket = "ANYTIME",
        targetType = "NONE",
        targetValue = null,
        unitLabel = "",
        startDate = startDate,
        endDate = null,
        goalType = "OPEN_ENDED",
        goalDate = null,
        goalCount = null,
        reminderTimes = "",
        note = "",
        customGroup = "",
        archived = false,
        createdAt = "2026-06-01T08:00:00",
        updatedAt = "2026-06-01T08:00:00",
    )

    private fun checkIn(
        date: String,
        status: CheckInStatus,
    ) = CheckInEntity(
        id = 1,
        habitId = 1,
        date = date,
        status = status.name,
        value = null,
        note = null,
        isBackfilled = false,
        checkedAt = "2026-06-12T08:00:00",
        createdAt = "2026-06-12T08:00:00",
        updatedAt = "2026-06-12T08:00:00",
    )

    private fun task(
        title: String,
        dueState: TaskDueState,
        isCompleted: Boolean = false,
    ) = TaskCardModel(
        id = title.hashCode().toLong(),
        title = title,
        description = "",
        quadrant = TaskQuadrant.IMPORTANT_NOT_URGENT,
        dueDate = when (dueState) {
            TaskDueState.OVERDUE -> LocalDate.of(2026, 6, 11)
            TaskDueState.TODAY -> LocalDate.of(2026, 6, 12)
            TaskDueState.UPCOMING -> LocalDate.of(2026, 6, 13)
            TaskDueState.SOMEDAY -> null
        },
        dueState = dueState,
        isCompleted = isCompleted,
        completedAt = if (isCompleted) LocalDateTime.of(2026, 6, 12, 9, 0) else null,
    )
}
