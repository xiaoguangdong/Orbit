package com.xiaoguangdong.orbit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaoguangdong.orbit.OrbitAppContainer
import com.xiaoguangdong.orbit.data.OrbitRepository
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.HabitDetailModel
import com.xiaoguangdong.orbit.domain.model.HabitDraft
import com.xiaoguangdong.orbit.domain.model.InsightsModel
import com.xiaoguangdong.orbit.domain.model.MilestoneDraft
import com.xiaoguangdong.orbit.domain.model.OrbitSettings
import com.xiaoguangdong.orbit.domain.model.TaskBoardModel
import com.xiaoguangdong.orbit.domain.model.TaskDraft
import com.xiaoguangdong.orbit.domain.model.TodayOverview
import com.xiaoguangdong.orbit.domain.model.TodaySectionModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class OrbitViewModel(
    private val appContainer: OrbitAppContainer,
) : ViewModel() {
    private val repository: OrbitRepository = appContainer.orbitRepository
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val currentDate: StateFlow<LocalDate> = selectedDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    val todayOverview: StateFlow<TodayOverview> = selectedDate
        .flatMapLatest { repository.observeTodayOverview(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayOverview(LocalDate.now(), 0, 0, 0, 0f, 0),
        )

    val todaySections: StateFlow<List<TodaySectionModel>> = selectedDate
        .flatMapLatest { repository.observeTodaySections(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeHabits = selectedDate
        .flatMapLatest { repository.observeActiveHabits(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedHabits = selectedDate
        .flatMapLatest { repository.observeArchivedHabits(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val insights: StateFlow<InsightsModel> = selectedDate
        .flatMapLatest { repository.observeInsights(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InsightsModel(0, 0, 0, 0, 0f, emptyList(), emptyList(), emptyList(), 0),
        )

    val milestones = selectedDate
        .flatMapLatest { repository.observeMilestones(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taskBoard = repository.observeTaskBoard()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TaskBoardModel(emptyList(), 0, 0),
        )

    val settings = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrbitSettings())

    init {
        appContainer.reminderScheduler.scheduleAllAsync()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun completeHabit(habitId: Long, value: Double? = null, note: String? = null, isBackfilled: Boolean = false) {
        viewModelScope.launch {
            repository.markCompleted(habitId, selectedDate.value, value, note, isBackfilled)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun completeHabitOn(habitId: Long, date: LocalDate, value: Double? = null, note: String? = null, isBackfilled: Boolean = false) {
        viewModelScope.launch {
            repository.markCompleted(habitId, date, value, note, isBackfilled)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun skipHabit(habitId: Long) {
        viewModelScope.launch {
            repository.markSkipped(habitId, selectedDate.value)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun failHabit(habitId: Long) {
        viewModelScope.launch {
            repository.markFailed(habitId, selectedDate.value)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun undoHabit(habitId: Long, date: LocalDate = selectedDate.value) {
        viewModelScope.launch {
            repository.undoCheckIn(habitId, date)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
            appContainer.reminderScheduler.cancelHabitReminders(habitId)
        }
    }

    fun restoreHabit(habitId: Long) {
        viewModelScope.launch {
            repository.restoreHabit(habitId)
            appContainer.reminderScheduler.refreshHabitAsync(habitId)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            appContainer.reminderScheduler.cancelHabitReminders(habitId)
        }
    }

    fun saveHabit(draft: HabitDraft, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val habitId = repository.upsertHabit(draft)
            appContainer.reminderScheduler.refreshHabitAsync(draft.id ?: habitId)
            onDone(draft.id ?: habitId)
        }
    }

    fun saveMilestone(draft: MilestoneDraft, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val milestoneId = repository.upsertMilestone(draft)
            onDone(draft.id ?: milestoneId)
        }
    }

    fun deleteMilestone(milestoneId: Long) {
        viewModelScope.launch {
            repository.deleteMilestone(milestoneId)
        }
    }

    fun saveTask(draft: TaskDraft, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val taskId = repository.upsertTask(draft)
            onDone(draft.id ?: taskId)
        }
    }

    fun toggleTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    suspend fun getHabitDraft(habitId: Long): HabitDraft? = repository.getHabitDraft(habitId)

    suspend fun getMilestoneDraft(milestoneId: Long): MilestoneDraft? = repository.getMilestoneDraft(milestoneId)

    suspend fun getTaskDraft(taskId: Long): TaskDraft? = repository.getTaskDraft(taskId)

    fun observeHabitDetail(habitId: Long): StateFlow<HabitDetailModel?> =
        repository.observeHabitDetail(habitId, selectedDate.value)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSettings(transform: OrbitSettings.() -> OrbitSettings) {
        viewModelScope.launch {
            repository.updateSettings(transform)
            val enabled = repository.getSettingsOnce().notificationsEnabled
            if (enabled) appContainer.reminderScheduler.scheduleAllAsync()
            else appContainer.reminderScheduler.cancelAllAsync()
        }
    }
}

class OrbitViewModelFactory(
    private val appContainer: OrbitAppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrbitViewModel(appContainer) as T
    }
}
