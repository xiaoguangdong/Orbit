package com.xiaoguangdong.orbit.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.xiaoguangdong.orbit.data.OrbitRepository
import com.xiaoguangdong.orbit.data.local.HabitEntity
import com.xiaoguangdong.orbit.domain.model.asStorage
import com.xiaoguangdong.orbit.domain.model.toLocalDateOrNull
import com.xiaoguangdong.orbit.domain.model.toLocalTimeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderScheduler(
    private val context: Context,
    private val repository: OrbitRepository,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAllAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettingsOnce()
            if (!settings.notificationsEnabled) {
                cancelAllAsync()
                return@launch
            }
            repository.getActiveHabitsOnce().forEach { habit ->
                cancelHabitReminders(habit.id)
                scheduleHabitReminders(habit)
            }
        }
    }

    fun cancelAllAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            repository.getActiveHabitsOnce().forEach { habit ->
                cancelHabitReminders(habit.id)
            }
        }
    }

    fun refreshHabitAsync(habitId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettingsOnce()
            val habit = repository.getActiveHabitsOnce().firstOrNull { it.id == habitId }
            if (habit == null || !settings.notificationsEnabled) {
                cancelHabitReminders(habitId)
                return@launch
            }
            cancelHabitReminders(habitId)
            scheduleHabitReminders(habit)
        }
    }

    fun cancelHabitReminders(habitId: Long) {
        repeat(6) { index ->
            val intent = reminderIntent(habitId, index)
            alarmManager.cancel(intent)
        }
    }

    private suspend fun scheduleHabitReminders(habit: HabitEntity) {
        val today = LocalDate.now()
        val reminderTimes = habit.reminderTimes.split(",").mapNotNull { it.toLocalTimeOrNull() }
        reminderTimes.forEachIndexed { index, time ->
            val nextTrigger = nextTriggerDateTime(habit, time, today) ?: return@forEachIndexed
            val intent = reminderIntent(habit.id, index)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                intent,
            )
        }
    }

    private suspend fun nextTriggerDateTime(
        habit: HabitEntity,
        time: LocalTime,
        fromDate: LocalDate,
    ): LocalDateTime? {
        var cursor = fromDate
        repeat(14) {
            val scheduled = cursor.atTime(time)
            if (scheduled.isAfter(LocalDateTime.now()) && com.xiaoguangdong.orbit.data.isHabitPlannedForDate(habit, cursor)) {
                val status = repository.getCheckInStatus(habit.id, cursor)
                if (status == null) {
                    return scheduled
                }
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    private fun reminderIntent(habitId: Long, reminderIndex: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_INDEX, reminderIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            "${habitId}_$reminderIndex".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
