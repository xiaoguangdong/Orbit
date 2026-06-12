package com.xiaoguangdong.orbit

import android.app.Application
import androidx.room.Room
import com.xiaoguangdong.orbit.data.OrbitRepository
import com.xiaoguangdong.orbit.data.SettingsRepository
import com.xiaoguangdong.orbit.data.local.OrbitDatabase
import com.xiaoguangdong.orbit.notifications.ReminderScheduler

class OrbitApplication : Application() {
    lateinit var appContainer: OrbitAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            OrbitDatabase::class.java,
            "orbit.db",
        ).fallbackToDestructiveMigration().build()

        val settingsRepository = SettingsRepository(applicationContext)
        val orbitRepository = OrbitRepository(
            habitDao = database.habitDao(),
            checkInDao = database.checkInDao(),
            milestoneDao = database.milestoneDao(),
            taskDao = database.taskDao(),
            settingsRepository = settingsRepository,
        )
        val reminderScheduler = ReminderScheduler(applicationContext, orbitRepository)

        appContainer = OrbitAppContainer(
            database = database,
            settingsRepository = settingsRepository,
            orbitRepository = orbitRepository,
            reminderScheduler = reminderScheduler,
        )
    }
}

data class OrbitAppContainer(
    val database: OrbitDatabase,
    val settingsRepository: SettingsRepository,
    val orbitRepository: OrbitRepository,
    val reminderScheduler: ReminderScheduler,
)
