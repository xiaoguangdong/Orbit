package com.xiaoguangdong.orbit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.DailyRecord
import com.xiaoguangdong.orbit.domain.model.HabitCardModel
import com.xiaoguangdong.orbit.domain.model.TodayOverview
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun OrbitGradientBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color.Transparent)) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF5F7FC), Color(0xFFEAF0FF), Color(0xFFF5F7FC)),
            ),
        )
        drawCircle(
            color = Color(0x222F6BFF),
            radius = size.minDimension * 0.3f,
            center = center.copy(x = size.width * 0.18f, y = size.height * 0.12f),
        )
        drawCircle(
            color = Color(0x1A24C99A),
            radius = size.minDimension * 0.24f,
            center = center.copy(x = size.width * 0.82f, y = size.height * 0.2f),
        )
    }
}

@Composable
fun TodayOverviewCard(overview: TodayOverview) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Today's orbit",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatPill("Planned", overview.plannedCount.toString(), Color(0xFF2F6BFF))
                StatPill("Completed", overview.completedCount.toString(), Color(0xFF24C99A))
                StatPill("Streaks", overview.activeStreaks.toString(), Color(0xFFF6B938))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6ECF8)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(overview.progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF2F6BFF), Color(0xFF24C99A)),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(dates) { date ->
            val selected = date == selectedDate
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color(0xFF2F6BFF) else Color.White.copy(alpha = 0.86f))
                    .clickable { onSelect(date) }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun HabitCard(
    habit: HabitCardModel,
    compact: Boolean,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
) {
    val accent = Color(android.graphics.Color.parseColor(habit.colorHex))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = habitIcon(habit.icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!compact) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${habit.streak}d streak") },
                            leadingIcon = {
                                Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            },
                        )
                        habit.targetLabel?.let {
                            AssistChip(onClick = {}, label = { Text(it) })
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = habit.statusForDate == CheckInStatus.COMPLETED,
                    onClick = onToggleDone,
                    label = {
                        Text(
                            when (habit.statusForDate) {
                                CheckInStatus.COMPLETED -> "Done"
                                CheckInStatus.SKIPPED -> "Skip"
                                CheckInStatus.FAILED -> "Fail"
                                null -> "Mark"
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.TaskAlt, null, modifier = Modifier.size(18.dp))
                    },
                )
                if (!compact) {
                    Text(
                        text = "${(habit.completionRate30d * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun HeatmapGrid(records: List<DailyRecord>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        records.forEach { record ->
            val color = when (record.status) {
                CheckInStatus.COMPLETED -> Color(0xFF24C99A)
                CheckInStatus.SKIPPED -> Color(0xFFF6B938)
                CheckInStatus.FAILED -> Color(0xFFFF6B6B)
                null -> Color(0xFFDCE4F5)
            }
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color.copy(alpha = if (record.status == null) 0.6f else 1f)),
            )
        }
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun habitIcon(icon: String): ImageVector = when (icon) {
    "run" -> Icons.Outlined.FitnessCenter
    "read" -> Icons.Outlined.MenuBook
    "journal" -> Icons.Outlined.Book
    "sleep" -> Icons.Outlined.DarkMode
    "call" -> Icons.Outlined.Call
    "water" -> Icons.Outlined.LocalDrink
    "mind" -> Icons.Outlined.Psychology
    "meditate" -> Icons.Outlined.SelfImprovement
    "target" -> Icons.Outlined.TrackChanges
    else -> Icons.Outlined.AutoAwesome
}
