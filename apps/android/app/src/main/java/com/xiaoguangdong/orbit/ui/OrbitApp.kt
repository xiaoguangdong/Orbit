package com.xiaoguangdong.orbit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xiaoguangdong.orbit.domain.model.AppScreen
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.MilestoneCardModel
import com.xiaoguangdong.orbit.domain.model.QuadrantSectionModel
import com.xiaoguangdong.orbit.domain.model.TaskBoardModel
import com.xiaoguangdong.orbit.domain.model.TaskCardModel
import com.xiaoguangdong.orbit.domain.model.TaskDueState
import com.xiaoguangdong.orbit.domain.model.TaskQuadrant
import com.xiaoguangdong.orbit.ui.components.DateStrip
import com.xiaoguangdong.orbit.ui.components.EmptyState
import com.xiaoguangdong.orbit.ui.components.HabitCard
import com.xiaoguangdong.orbit.ui.components.HeatmapGrid
import com.xiaoguangdong.orbit.ui.components.OrbitGradientBackdrop
import com.xiaoguangdong.orbit.ui.components.SectionHeader
import com.xiaoguangdong.orbit.ui.components.TodayOverviewCard
import com.xiaoguangdong.orbit.ui.screens.habit.HabitDetailScreen
import com.xiaoguangdong.orbit.ui.screens.habit.HabitEditorScreen
import com.xiaoguangdong.orbit.ui.screens.habit.MilestoneEditorScreen
import com.xiaoguangdong.orbit.ui.screens.task.TaskEditorScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val screen: AppScreen,
)

private val topLevelDestinations = listOf(
    TopLevelDestination("today", Icons.Outlined.Today, AppScreen.TODAY),
    TopLevelDestination("orbit", Icons.Outlined.Public, AppScreen.ORBIT),
    TopLevelDestination("insights", Icons.Outlined.Insights, AppScreen.INSIGHTS),
    TopLevelDestination("milestones", Icons.Outlined.Flag, AppScreen.MILESTONES),
    TopLevelDestination("me", Icons.Outlined.AccountCircle, AppScreen.ME),
)

private enum class TodayPane {
    HABITS,
    QUADRANTS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitApp(
    viewModel: OrbitViewModel,
    initialHabitId: Long? = null,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "today"
    val showShell = currentRoute in topLevelDestinations.map { it.route }
    var todayPane by rememberSaveable { mutableStateOf(TodayPane.HABITS.name) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LaunchedEffect(initialHabitId) {
        initialHabitId?.let { habitId ->
            navController.navigate("habitDetail/$habitId")
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides settings.appLanguage) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrbitGradientBackdrop(modifier = Modifier.fillMaxSize())
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (showShell) {
                        NavigationBar(containerColor = Color.White.copy(alpha = 0.94f)) {
                            topLevelDestinations.forEach { destination ->
                                val selected = navBackStackEntry?.destination?.hierarchy?.any { node ->
                                    node.route == destination.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(destination.icon, topLevelLabel(destination.route)) },
                                    label = { Text(topLevelLabel(destination.route)) },
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    when {
                        !showShell -> Unit
                        currentRoute.startsWith("milestones") -> FloatingActionButton(
                            onClick = { navController.navigate("milestoneEditor") },
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = tr("Add milestone", "添加里程碑"))
                        }

                        currentRoute in listOf("today", "orbit") -> FloatingActionButton(
                            onClick = {
                                if (currentRoute == "today" && todayPane == TodayPane.QUADRANTS.name) {
                                    navController.navigate("taskEditor")
                                } else {
                                    navController.navigate("habitEditor")
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = if (currentRoute == "today" && todayPane == TodayPane.QUADRANTS.name) {
                                    tr("Add task", "添加任务")
                                } else {
                                    tr("Add habit", "添加习惯")
                                },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "today",
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable("today") {
                        TodayScreen(
                            viewModel = viewModel,
                            currentPane = TodayPane.valueOf(todayPane),
                            onPaneChange = { todayPane = it.name },
                            onOpenHabit = { habitId -> navController.navigate("habitDetail/$habitId") },
                            onOpenTask = { taskId -> navController.navigate("taskEditor?taskId=$taskId") },
                        )
                    }
                    composable("orbit") {
                        OrbitScreen(
                            viewModel = viewModel,
                            onOpenHabit = { habitId -> navController.navigate("habitDetail/$habitId") },
                        )
                    }
                    composable("insights") {
                        InsightsScreen(viewModel = viewModel)
                    }
                    composable("milestones") {
                        MilestonesScreen(
                            viewModel = viewModel,
                            onEditMilestone = { milestoneId -> navController.navigate("milestoneEditor?milestoneId=$milestoneId") },
                        )
                    }
                    composable("me") {
                        MeScreen(viewModel = viewModel)
                    }
                    composable(
                        route = "habitDetail/{habitId}",
                        arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
                    ) { backStack ->
                        val habitId = backStack.arguments?.getLong("habitId") ?: return@composable
                        HabitDetailScreen(
                            viewModel = viewModel,
                            habitId = habitId,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("habitEditor?habitId=$habitId") },
                        )
                    }
                    composable(
                        route = "habitEditor?habitId={habitId}",
                        arguments = listOf(navArgument("habitId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }),
                    ) { backStack ->
                        val rawId = backStack.arguments?.getString("habitId")
                        HabitEditorScreen(
                            viewModel = viewModel,
                            habitId = rawId?.toLongOrNull(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "milestoneEditor?milestoneId={milestoneId}",
                        arguments = listOf(navArgument("milestoneId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }),
                    ) { backStack ->
                        val rawId = backStack.arguments?.getString("milestoneId")
                        MilestoneEditorScreen(
                            viewModel = viewModel,
                            milestoneId = rawId?.toLongOrNull(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "taskEditor?taskId={taskId}",
                        arguments = listOf(navArgument("taskId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }),
                    ) { backStack ->
                        val rawId = backStack.arguments?.getString("taskId")
                        TaskEditorScreen(
                            viewModel = viewModel,
                            taskId = rawId?.toLongOrNull(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(
    viewModel: OrbitViewModel,
    currentPane: TodayPane,
    onPaneChange: (TodayPane) -> Unit,
    onOpenHabit: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val selectedDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val overview by viewModel.todayOverview.collectAsStateWithLifecycle()
    val sections by viewModel.todaySections.collectAsStateWithLifecycle()
    val taskBoard by viewModel.taskBoard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var pendingTaskDelete by remember { mutableStateOf<TaskCardModel?>(null) }

    val dates = remember(selectedDate) { (-3L..3L).map { selectedDate.plusDays(it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("Today", "今天"), style = MaterialTheme.typography.headlineLarge)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = tr("Today options", "今天选项"))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (settings.compactView) tr("Show details", "显示详情") else tr("Hide details", "隐藏详情")) },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateSettings { copy(compactView = !compactView) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (settings.hideCompleted) tr("Show completed", "显示已完成") else tr("Hide completed", "隐藏已完成")) },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateSettings { copy(hideCompleted = !hideCompleted) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (settings.sortIncompleteFirst) tr("Sort by due date", "按截止日期排序") else tr("Sort incomplete first", "未完成优先")) },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateSettings { copy(sortIncompleteFirst = !sortIncompleteFirst) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (settings.showInSevenDayStrip) tr("Hide date strip", "隐藏日期条") else tr("Show date strip", "显示日期条")) },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateSettings { copy(showInSevenDayStrip = !showInSevenDayStrip) }
                                },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = currentPane == TodayPane.HABITS,
                        onClick = { onPaneChange(TodayPane.HABITS) },
                        label = { Text(tr("Habits", "习惯")) },
                    )
                    FilterChip(
                        selected = currentPane == TodayPane.QUADRANTS,
                        onClick = { onPaneChange(TodayPane.QUADRANTS) },
                        label = { Text(tr("Quadrants", "四象限")) },
                    )
                }
            }
        }
        if (currentPane == TodayPane.HABITS) {
            if (settings.showInSevenDayStrip) {
                item {
                    DateStrip(dates = dates, selectedDate = selectedDate, onSelect = viewModel::selectDate)
                }
            }
            item {
                TodayOverviewCard(overview = overview)
            }
            if (sections.isEmpty()) {
                item {
                    EmptyState(
                        title = tr("No habits planned", "今天没有计划习惯"),
                        description = tr("Create a new orbit or switch to a different date.", "创建一个新习惯，或切换到其他日期。"),
                    )
                }
            }
            sections.forEach { section ->
                item { SectionHeader(sectionTitle(section.title), section.habits.size) }
                items(section.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        compact = settings.compactView,
                        onClick = { onOpenHabit(habit.id) },
                        onToggleDone = {
                            if (habit.statusForDate == CheckInStatus.COMPLETED) viewModel.undoHabit(habit.id)
                            else viewModel.completeHabit(habit.id)
                        },
                    )
                }
            }
        } else {
            item {
                StatsPanel(
                    title = tr("Four quadrants", "四象限"),
                    lines = listOf(
                        "${tr("Active tasks", "进行中任务")} ${taskBoard.activeCount}",
                        "${tr("Completed tasks", "已完成任务")} ${taskBoard.completedCount}",
                        tr("Tap a task to edit, tap the check to complete", "点任务编辑，点勾选完成"),
                    ),
                )
            }
            item {
                FourQuadrantBoard(
                    board = taskBoard,
                    onToggleTask = viewModel::toggleTaskCompleted,
                    onOpenTask = onOpenTask,
                    onDeleteTask = { pendingTaskDelete = it },
                )
            }
        }
    }

    pendingTaskDelete?.let { task ->
        ConfirmationDialog(
            title = tr("Delete task?", "删除任务？"),
            message = tr("\"${task.title}\" will be removed from the quadrant board.", "\"${task.title}\" 将从四象限中删除。"),
            confirmLabel = tr("Delete", "删除"),
            onDismiss = { pendingTaskDelete = null },
            onConfirm = {
                viewModel.deleteTask(task.id)
                pendingTaskDelete = null
            },
        )
    }
}

@Composable
private fun OrbitScreen(
    viewModel: OrbitViewModel,
    onOpenHabit: (Long) -> Unit,
) {
    val activeHabits by viewModel.activeHabits.collectAsStateWithLifecycle()
    val archivedHabits by viewModel.archivedHabits.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val source = if (showArchived) archivedHabits else activeHabits
    val habits = remember(source, query) {
        if (query.isBlank()) source else source.filter { it.name.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Orbit", "习惯"), style = MaterialTheme.typography.headlineLarge)
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Search habits", "搜索习惯")) },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text(tr("Active", "进行中")) },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text(tr("Archived", "已归档")) },
                    )
                }
            }
        }
        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = if (showArchived) tr("No archived habits", "没有已归档习惯") else tr("No active habits", "没有进行中习惯"),
                    description = tr("Add your first orbit to start building momentum.", "添加第一个习惯，开始积累节奏。"),
                )
            }
        }
        val grouped = habits.groupBy { it.bucket }
        grouped.forEach { (bucket, bucketHabits) ->
            item { SectionHeader(bucketLabel(bucket), bucketHabits.size) }
            items(bucketHabits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    compact = settings.compactView,
                    onClick = { onOpenHabit(habit.id) },
                    onToggleDone = {
                        if (habit.statusForDate == CheckInStatus.COMPLETED) viewModel.undoHabit(habit.id)
                        else viewModel.completeHabit(habit.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun InsightsScreen(viewModel: OrbitViewModel) {
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(tr("Insights", "洞察"), style = MaterialTheme.typography.headlineLarge)
        }
        item {
            TodayOverviewCard(
                overview = com.xiaoguangdong.orbit.domain.model.TodayOverview(
                    date = LocalDate.now(),
                    plannedCount = insights.plannedToday,
                    completedCount = insights.completedToday,
                    skippedCount = 0,
                    progress = if (insights.plannedToday == 0) 0f else insights.completedToday.toFloat() / insights.plannedToday.toFloat(),
                    activeStreaks = insights.currentLeaders.count { it.streak > 0 },
                ),
            )
        }
        item {
            StatsPanel(
                title = tr("This week", "本周"),
                lines = listOf(
                    "${tr("Weekly completion", "本周完成率")} ${(insights.weeklyCompletionRate * 100).toInt()}%",
                    "${tr("Star credits", "星筹值")} ${insights.starCredits}",
                    "${tr("Active habits", "进行中习惯")} ${insights.activeHabits}",
                ),
            )
        }
        item { SectionHeader(tr("Current streak leaders", "当前连胜榜"), insights.currentLeaders.size) }
        items(insights.currentLeaders, key = { it.id }) { habit ->
            HabitCard(habit = habit, compact = true, onClick = {}, onToggleDone = {})
        }
        item { SectionHeader(tr("Best runs", "最佳连续记录"), insights.longestLeaders.size) }
        items(insights.longestLeaders, key = { it.id }) { habit ->
            HabitCard(habit = habit, compact = true, onClick = {}, onToggleDone = {})
        }
        item {
            StatsPanel(
                title = tr("Last 60 days", "最近 60 天"),
                content = { HeatmapGrid(records = insights.heatmap) },
            )
        }
    }
}

@Composable
private fun MilestonesScreen(
    viewModel: OrbitViewModel,
    onEditMilestone: (Long) -> Unit,
) {
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()
    var pendingMilestoneDelete by remember { mutableStateOf<MilestoneCardModel?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(tr("Milestones", "里程碑"), style = MaterialTheme.typography.headlineLarge) }
        if (milestones.isEmpty()) {
            item {
                EmptyState(
                    title = tr("No milestones yet", "还没有里程碑"),
                    description = tr("Track count downs, anniversaries, and habit target dates here.", "在这里记录倒计时、纪念日和习惯目标日期。"),
                )
            }
        }
        items(milestones, key = { it.id }) { milestone ->
            MilestoneCard(
                milestone = milestone,
                onEdit = { onEditMilestone(milestone.id) },
                onDelete = { pendingMilestoneDelete = milestone },
            )
        }
    }

    pendingMilestoneDelete?.let { milestone ->
        ConfirmationDialog(
            title = tr("Delete milestone?", "删除里程碑？"),
            message = tr("\"${milestone.title}\" will be removed from the milestone list.", "\"${milestone.title}\" 将从里程碑列表中删除。"),
            confirmLabel = tr("Delete", "删除"),
            onDismiss = { pendingMilestoneDelete = null },
            onConfirm = {
                viewModel.deleteMilestone(milestone.id)
                pendingMilestoneDelete = null
            },
        )
    }
}

@Composable
private fun MeScreen(viewModel: OrbitViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatsPanel(
                title = tr("Orbit / 星筹", "Orbit / 星筹"),
                lines = listOf(
                    tr("Offline-first Android MVP", "离线优先 Android MVP"),
                    "${tr("Star credits", "星筹值")} ${insights.starCredits}",
                    tr("Login and sync are reserved for later backend work", "登录和同步留待后续后端接入"),
                ),
            )
        }
        item {
            StatsPanel(
                title = tr("Language", "语言"),
                lines = listOf(tr("Switch the app copy instantly without changing system language.", "无需修改系统语言，立即切换应用文案。")),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.appLanguage.name == "CHINESE",
                        onClick = { viewModel.updateSettings { copy(appLanguage = com.xiaoguangdong.orbit.domain.model.AppLanguage.CHINESE) } },
                        label = { Text(appLanguageLabel(com.xiaoguangdong.orbit.domain.model.AppLanguage.CHINESE)) },
                    )
                    FilterChip(
                        selected = settings.appLanguage.name == "ENGLISH",
                        onClick = { viewModel.updateSettings { copy(appLanguage = com.xiaoguangdong.orbit.domain.model.AppLanguage.ENGLISH) } },
                        label = { Text(appLanguageLabel(com.xiaoguangdong.orbit.domain.model.AppLanguage.ENGLISH)) },
                    )
                }
            }
        }
        item {
            SettingsToggleCard(
                title = tr("Habit reminders", "习惯提醒"),
                checked = settings.notificationsEnabled,
                description = tr("Enable local reminder scheduling only.", "只启用本地提醒调度。"),
                onCheckedChange = { enabled ->
                    viewModel.updateSettings { copy(notificationsEnabled = enabled) }
                },
            )
        }
        item {
            SettingsToggleCard(
                title = tr("Compact orbit view", "紧凑习惯视图"),
                checked = settings.compactView,
                description = tr("Hide secondary details and make the list denser.", "隐藏次要信息，让列表更紧凑。"),
                onCheckedChange = { compact ->
                    viewModel.updateSettings { copy(compactView = compact) }
                },
            )
        }
        item {
            SettingsToggleCard(
                title = tr("Hide completed in Today", "今天页隐藏已完成"),
                checked = settings.hideCompleted,
                description = tr("Keep focus on unfinished habits first.", "优先聚焦未完成的习惯。"),
                onCheckedChange = { hidden ->
                    viewModel.updateSettings { copy(hideCompleted = hidden) }
                },
            )
        }
        item {
            StatsPanel(
                title = tr("Backend placeholders", "后端占位"),
                lines = listOf(
                    tr("Account login: not implemented", "账号登录：未实现"),
                    tr("Cloud sync: placeholder only", "云同步：仅占位"),
                    tr("Community and premium modules: not implemented", "社区和会员模块：未实现"),
                ),
            )
        }
    }
}

@Composable
private fun FourQuadrantBoard(
    board: TaskBoardModel,
    onToggleTask: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onDeleteTask: (TaskCardModel) -> Unit,
) {
    val sections = board.sections.associateBy { it.quadrant }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuadrantCard(
                modifier = Modifier.weight(1f),
                section = sections.getValue(TaskQuadrant.IMPORTANT_URGENT),
                onToggleTask = onToggleTask,
                onOpenTask = onOpenTask,
                onDeleteTask = onDeleteTask,
            )
            QuadrantCard(
                modifier = Modifier.weight(1f),
                section = sections.getValue(TaskQuadrant.IMPORTANT_NOT_URGENT),
                onToggleTask = onToggleTask,
                onOpenTask = onOpenTask,
                onDeleteTask = onDeleteTask,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuadrantCard(
                modifier = Modifier.weight(1f),
                section = sections.getValue(TaskQuadrant.NOT_IMPORTANT_URGENT),
                onToggleTask = onToggleTask,
                onOpenTask = onOpenTask,
                onDeleteTask = onDeleteTask,
            )
            QuadrantCard(
                modifier = Modifier.weight(1f),
                section = sections.getValue(TaskQuadrant.NOT_IMPORTANT_NOT_URGENT),
                onToggleTask = onToggleTask,
                onOpenTask = onOpenTask,
                onDeleteTask = onDeleteTask,
            )
        }
    }
}

@Composable
private fun QuadrantCard(
    modifier: Modifier = Modifier,
    section: QuadrantSectionModel,
    onToggleTask: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onDeleteTask: (TaskCardModel) -> Unit,
) {
    val accent = Color(android.graphics.Color.parseColor(section.quadrant.colorHex))
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${section.quadrant.shortLabel}. ${taskQuadrantLabel(section.quadrant)}",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            if (section.tasks.isEmpty()) {
                Text(
                    text = when (section.quadrant) {
                        TaskQuadrant.IMPORTANT_URGENT -> tr("No fires to put out.", "没有需要立刻救火的事。")
                        TaskQuadrant.IMPORTANT_NOT_URGENT -> tr("Plan the next meaningful move.", "规划下一步真正重要的事。")
                        TaskQuadrant.NOT_IMPORTANT_URGENT -> tr("Nothing delegated or queued.", "暂无待委派或待处理事项。")
                        TaskQuadrant.NOT_IMPORTANT_NOT_URGENT -> tr("Keep this quadrant intentionally light.", "这一象限尽量保持轻量。")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                section.tasks.take(6).forEach { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onOpen = { onOpenTask(task.id) },
                        onDelete = { onDeleteTask(task) },
                    )
                }
                if (section.tasks.size > 6) {
                    Text(
                        text = tr("+${section.tasks.size - 6} more", "还有 ${section.tasks.size - 6} 项"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskCardModel,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Outlined.Restore else Icons.Outlined.CheckCircleOutline,
                contentDescription = if (task.isCompleted) tr("Mark incomplete", "标记为未完成") else tr("Mark complete", "标记为完成"),
                tint = if (task.isCompleted) Color(0xFF24C99A) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            val dueColor = when (task.dueState) {
                TaskDueState.OVERDUE -> Color(0xFFDF3B3B)
                TaskDueState.TODAY -> Color(0xFFF6B938)
                TaskDueState.UPCOMING -> Color(0xFF2F6BFF)
                TaskDueState.SOMEDAY -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val dueLabel = when {
                task.dueDate == null -> taskDueStateLabel(task.dueState)
                else -> "${taskDueStateLabel(task.dueState)} · ${task.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
            }
            Text(
                text = dueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = dueColor,
            )
        }
        Column {
            IconButton(onClick = onOpen) {
                Icon(Icons.Outlined.Edit, contentDescription = tr("Edit task", "编辑任务"))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = tr("Delete task", "删除任务"))
            }
        }
    }
}

@Composable
private fun StatsPanel(
    title: String,
    lines: List<String> = emptyList(),
    content: @Composable () -> Unit = {},
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    description: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "取消"))
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
    )
}

@Composable
private fun MilestoneCard(
    milestone: MilestoneCardModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = Color(android.graphics.Color.parseColor(milestone.colorHex))
    androidx.compose.material3.Card(
        onClick = onEdit,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(milestone.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        milestoneTypeLabel(milestone.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
                Text(
                    text = milestone.daysDelta.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = accent,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = tr("Delete milestone", "删除里程碑"), tint = accent)
                }
            }
        }
    }
}
