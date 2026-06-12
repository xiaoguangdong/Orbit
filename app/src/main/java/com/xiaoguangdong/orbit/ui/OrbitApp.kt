package com.xiaoguangdong.orbit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Today
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

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val screen: AppScreen,
)

private val topLevelDestinations = listOf(
    TopLevelDestination("today", "Today", Icons.Outlined.Today, AppScreen.TODAY),
    TopLevelDestination("orbit", "Orbit", Icons.Outlined.Public, AppScreen.ORBIT),
    TopLevelDestination("insights", "Insights", Icons.Outlined.Insights, AppScreen.INSIGHTS),
    TopLevelDestination("milestones", "Milestones", Icons.Outlined.Flag, AppScreen.MILESTONES),
    TopLevelDestination("me", "Me", Icons.Outlined.AccountCircle, AppScreen.ME),
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

    LaunchedEffect(initialHabitId) {
        initialHabitId?.let { habitId ->
            navController.navigate("habitDetail/$habitId")
        }
    }

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
                                icon = { Icon(destination.icon, destination.label) },
                                label = { Text(destination.label) },
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
                        Icon(Icons.Outlined.Add, contentDescription = "Add milestone")
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
                            contentDescription = if (currentRoute == "today" && todayPane == TodayPane.QUADRANTS.name) "Add task" else "Add habit",
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

    val dates = remember(selectedDate) { (-3L..3L).map { selectedDate.plusDays(it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today", style = MaterialTheme.typography.headlineLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = currentPane == TodayPane.HABITS,
                        onClick = { onPaneChange(TodayPane.HABITS) },
                        label = { Text("Habits") },
                    )
                    FilterChip(
                        selected = currentPane == TodayPane.QUADRANTS,
                        onClick = { onPaneChange(TodayPane.QUADRANTS) },
                        label = { Text("Quadrants") },
                    )
                }
            }
        }
        if (currentPane == TodayPane.HABITS) {
            item {
                DateStrip(dates = dates, selectedDate = selectedDate, onSelect = viewModel::selectDate)
            }
            item {
                TodayOverviewCard(overview = overview)
            }
            if (sections.isEmpty()) {
                item {
                    EmptyState(
                        title = "No habits planned",
                        description = "Create a new orbit or switch to a different date.",
                    )
                }
            }
            sections.forEach { section ->
                item { SectionHeader(section.title, section.habits.size) }
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
                    title = "Four quadrants",
                    lines = listOf(
                        "Active tasks ${taskBoard.activeCount}",
                        "Completed tasks ${taskBoard.completedCount}",
                        "Tap a task to edit, tap the check to complete",
                    ),
                )
            }
            item {
                FourQuadrantBoard(
                    board = taskBoard,
                    onToggleTask = viewModel::toggleTaskCompleted,
                    onOpenTask = onOpenTask,
                    onDeleteTask = viewModel::deleteTask,
                )
            }
        }
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
                Text("Orbit", style = MaterialTheme.typography.headlineLarge)
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search habits") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("Active") },
                    )
                    androidx.compose.material3.FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("Archived") },
                    )
                }
            }
        }
        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = if (showArchived) "No archived habits" else "No active habits",
                    description = "Add your first orbit to start building momentum.",
                )
            }
        }
        val grouped = habits.groupBy { it.bucket }
        grouped.forEach { (bucket, bucketHabits) ->
            item { SectionHeader(bucket.displayName, bucketHabits.size) }
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
            Text("Insights", style = MaterialTheme.typography.headlineLarge)
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
                title = "This week",
                lines = listOf(
                    "Weekly completion ${(insights.weeklyCompletionRate * 100).toInt()}%",
                    "Star credits ${insights.starCredits}",
                    "Active habits ${insights.activeHabits}",
                ),
            )
        }
        item { SectionHeader("Current streak leaders", insights.currentLeaders.size) }
        items(insights.currentLeaders, key = { it.id }) { habit ->
            HabitCard(habit = habit, compact = true, onClick = {}, onToggleDone = {})
        }
        item { SectionHeader("Best runs", insights.longestLeaders.size) }
        items(insights.longestLeaders, key = { it.id }) { habit ->
            HabitCard(habit = habit, compact = true, onClick = {}, onToggleDone = {})
        }
        item {
            StatsPanel(
                title = "Last 60 days",
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Milestones", style = MaterialTheme.typography.headlineLarge) }
        if (milestones.isEmpty()) {
            item {
                EmptyState(
                    title = "No milestones yet",
                    description = "Track count downs, anniversaries, and habit target dates here.",
                )
            }
        }
        items(milestones, key = { it.id }) { milestone ->
            MilestoneCard(
                milestone = milestone,
                onEdit = { onEditMilestone(milestone.id) },
                onDelete = { viewModel.deleteMilestone(milestone.id) },
            )
        }
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
                title = "Orbit / 星筹",
                lines = listOf(
                    "Offline-first Android MVP",
                    "Star credits ${insights.starCredits}",
                    "Login and sync are reserved for later backend work",
                ),
            )
        }
        item {
            SettingsToggleCard(
                title = "Habit reminders",
                checked = settings.notificationsEnabled,
                description = "Enable local reminder scheduling only.",
                onCheckedChange = { enabled ->
                    viewModel.updateSettings { copy(notificationsEnabled = enabled) }
                },
            )
        }
        item {
            SettingsToggleCard(
                title = "Compact orbit view",
                checked = settings.compactView,
                description = "Hide secondary details and make the list denser.",
                onCheckedChange = { compact ->
                    viewModel.updateSettings { copy(compactView = compact) }
                },
            )
        }
        item {
            SettingsToggleCard(
                title = "Hide completed in Today",
                checked = settings.hideCompleted,
                description = "Keep focus on unfinished habits first.",
                onCheckedChange = { hidden ->
                    viewModel.updateSettings { copy(hideCompleted = hidden) }
                },
            )
        }
        item {
            StatsPanel(
                title = "Backend placeholders",
                lines = listOf(
                    "Account login: not implemented",
                    "Cloud sync: placeholder only",
                    "Community and premium modules: not implemented",
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
    onDeleteTask: (Long) -> Unit,
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
    onDeleteTask: (Long) -> Unit,
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
                text = "${section.quadrant.shortLabel}. ${section.quadrant.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            if (section.tasks.isEmpty()) {
                Text(
                    text = "No tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                section.tasks.take(6).forEach { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onOpen = { onOpenTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                    )
                }
                if (section.tasks.size > 6) {
                    Text(
                        text = "+${section.tasks.size - 6} more",
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
                imageVector = Icons.Outlined.CheckCircleOutline,
                contentDescription = if (task.isCompleted) "Mark incomplete" else "Mark complete",
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
            task.dueDate?.let { dueDate ->
                Text(
                    text = dueDate.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column {
            IconButton(onClick = onOpen) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit task")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete task")
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
                        milestone.type.displayName,
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
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete milestone", tint = accent)
                }
            }
        }
    }
}
