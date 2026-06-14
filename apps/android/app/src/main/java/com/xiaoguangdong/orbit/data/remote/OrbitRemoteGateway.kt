package com.xiaoguangdong.orbit.data.remote

/**
 * Backend contracts stay as placeholders for the offline-only release.
 * Hook real implementations here once account, sync, and cloud APIs exist.
 */
interface OrbitRemoteGateway {
    suspend fun register(input: RegisterRequest): RemoteResult<SessionDto>
    suspend fun login(input: LoginRequest): RemoteResult<SessionDto>
    suspend fun logout(): RemoteResult<Unit>
    suspend fun syncHabits(payload: List<HabitDto>): RemoteResult<SyncEnvelope<HabitDto>>
    suspend fun syncTasks(payload: List<TaskDto>): RemoteResult<SyncEnvelope<TaskDto>>
    suspend fun syncMilestones(payload: List<MilestoneDto>): RemoteResult<SyncEnvelope<MilestoneDto>>
    suspend fun syncSettings(payload: SettingsDto): RemoteResult<SettingsDto>
}

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class SessionDto(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String,
)

data class HabitDto(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val frequencyType: String,
    val selectedWeekDays: List<String>,
    val weeklyTargetCount: Int,
    val monthlyDays: List<Int>,
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
    val reminderTimes: List<String>,
    val note: String,
    val customGroup: String,
    val archived: Boolean,
    val updatedAt: String,
)

data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val quadrant: String,
    val dueDate: String?,
    val isCompleted: Boolean,
    val completedAt: String?,
    val updatedAt: String,
)

data class MilestoneDto(
    val id: String,
    val title: String,
    val date: String,
    val type: String,
    val icon: String,
    val colorHex: String,
    val habitId: String?,
    val reminderTime: String?,
    val note: String,
    val updatedAt: String,
)

data class SettingsDto(
    val checkInSound: Boolean,
    val sortIncompleteFirst: Boolean,
    val showInSevenDayStrip: Boolean,
    val includeInBadgeCount: Boolean,
    val hideCompleted: Boolean,
    val compactView: Boolean,
    val weekStartsOnMonday: Boolean,
    val notificationsEnabled: Boolean,
)

data class SyncEnvelope<T>(
    val items: List<T>,
    val serverCursor: String?,
    val syncedAt: String,
)

sealed interface RemoteResult<out T> {
    data class Success<T>(val value: T) : RemoteResult<T>
    data class Failure(val code: String, val message: String) : RemoteResult<Nothing>
}

object NoopOrbitRemoteGateway : OrbitRemoteGateway {
    override suspend fun register(input: RegisterRequest): RemoteResult<SessionDto> = notImplemented()

    override suspend fun login(input: LoginRequest): RemoteResult<SessionDto> = notImplemented()

    override suspend fun logout(): RemoteResult<Unit> = notImplemented()

    override suspend fun syncHabits(payload: List<HabitDto>): RemoteResult<SyncEnvelope<HabitDto>> = notImplemented()

    override suspend fun syncTasks(payload: List<TaskDto>): RemoteResult<SyncEnvelope<TaskDto>> = notImplemented()

    override suspend fun syncMilestones(payload: List<MilestoneDto>): RemoteResult<SyncEnvelope<MilestoneDto>> = notImplemented()

    override suspend fun syncSettings(payload: SettingsDto): RemoteResult<SettingsDto> = notImplemented()

    private fun <T> notImplemented(): RemoteResult<T> =
        RemoteResult.Failure(
            code = "not_implemented",
            message = "Backend integration is intentionally unavailable in the offline MVP.",
        )
}
