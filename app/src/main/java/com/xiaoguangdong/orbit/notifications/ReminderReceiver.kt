package com.xiaoguangdong.orbit.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xiaoguangdong.orbit.MainActivity
import com.xiaoguangdong.orbit.OrbitApplication
import com.xiaoguangdong.orbit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext as OrbitApplication
            val repository = app.appContainer.orbitRepository
            val habit = repository.getHabitDraft(habitId)
            if (habit != null && canPostNotifications(context)) {
                ensureChannel(context)
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
                }
                val contentIntent = PendingIntent.getActivity(
                    context,
                    habitId.toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                NotificationManagerCompat.from(context).notify(
                    habitId.toInt(),
                    NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(habit.name)
                        .setContentText("Today's orbit is still waiting for a check-in.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .build(),
                )
            }
            app.appContainer.reminderScheduler.refreshHabitAsync(habitId)
            pendingResult.finish()
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Orbit check-in reminders"
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.xiaoguangdong.orbit.SHOW_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_REMINDER_INDEX = "extra_reminder_index"
        const val CHANNEL_ID = "orbit_habit_reminders"
    }
}
