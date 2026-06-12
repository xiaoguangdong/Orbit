package com.xiaoguangdong.orbit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xiaoguangdong.orbit.OrbitApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as OrbitApplication
            app.appContainer.reminderScheduler.scheduleAllAsync()
        }
    }
}
