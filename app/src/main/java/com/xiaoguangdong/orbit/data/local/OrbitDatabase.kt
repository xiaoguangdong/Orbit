package com.xiaoguangdong.orbit.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val frequencyType: String,
    val selectedWeekDays: String,
    val weeklyTargetCount: Int,
    val monthlyDays: String,
    val intervalDays: Int,
    val timeBucket: String,
    val targetType: String,
    val targetValue: Double?,
    val unitLabel: String,
    val startDate: String,
    val endDate: String?,
    val goalType: String,
    val goalDate: String?,
    val goalCount: Int?,
    val reminderTimes: String,
    val note: String,
    val customGroup: String,
    val archived: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String,
    val status: String,
    val value: Double?,
    val note: String?,
    val isBackfilled: Boolean,
    val checkedAt: String,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val type: String,
    val icon: String,
    val colorHex: String,
    val habitId: Long?,
    val reminderTime: String?,
    val note: String,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val quadrant: String,
    val dueDate: String?,
    val isCompleted: Boolean,
    val completedAt: String?,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY updatedAt DESC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun observeHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE archived = 0")
    suspend fun getActiveHabitsOnce(): List<HabitEntity>

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = :archived, updatedAt = :updatedAt WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, archived: Boolean, updatedAt: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long)
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins")
    fun observeAllCheckIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY date DESC")
    fun observeHabitCheckIns(habitId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCheckInForDate(habitId: Long, date: String): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCheckInsInRange(startDate: String, endDate: String): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getHabitCheckIns(habitId: Long): List<CheckInEntity>

    @Upsert
    suspend fun upsertCheckIn(checkIn: CheckInEntity): Long

    @Query("DELETE FROM check_ins WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCheckInForDate(habitId: Long, date: String)

    @Query("DELETE FROM check_ins WHERE habitId = :habitId")
    suspend fun deleteCheckInsForHabit(habitId: Long)
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones ORDER BY date ASC")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE id = :id LIMIT 1")
    suspend fun getMilestoneById(id: Long): MilestoneEntity?

    @Upsert
    suspend fun upsertMilestone(milestone: MilestoneEntity): Long

    @Query("DELETE FROM milestones WHERE id = :milestoneId")
    suspend fun deleteMilestone(milestoneId: Long)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, sortOrder ASC, updatedAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun getTasksOnce(): List<TaskEntity>

    @Upsert
    suspend fun upsertTask(task: TaskEntity): Long

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean, completedAt: String?, updatedAt: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)
}

@Database(
    entities = [HabitEntity::class, CheckInEntity::class, MilestoneEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun checkInDao(): CheckInDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun taskDao(): TaskDao
}
