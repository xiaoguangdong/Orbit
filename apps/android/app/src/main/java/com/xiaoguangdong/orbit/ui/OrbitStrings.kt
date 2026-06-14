package com.xiaoguangdong.orbit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.xiaoguangdong.orbit.domain.model.AppLanguage
import com.xiaoguangdong.orbit.domain.model.CheckInStatus
import com.xiaoguangdong.orbit.domain.model.FrequencyType
import com.xiaoguangdong.orbit.domain.model.GoalType
import com.xiaoguangdong.orbit.domain.model.HabitTimeBucket
import com.xiaoguangdong.orbit.domain.model.MilestoneType
import com.xiaoguangdong.orbit.domain.model.TargetType
import com.xiaoguangdong.orbit.domain.model.TaskDueState
import com.xiaoguangdong.orbit.domain.model.TaskQuadrant
import java.time.DayOfWeek
import java.util.Locale

val LocalAppLanguage = compositionLocalOf { AppLanguage.CHINESE }

@Composable
@ReadOnlyComposable
fun tr(en: String, zh: String): String = if (LocalAppLanguage.current == AppLanguage.CHINESE) zh else en

@Composable
@ReadOnlyComposable
fun appLanguageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.ENGLISH -> tr("English", "英文")
    AppLanguage.CHINESE -> tr("Chinese", "中文")
}

@Composable
@ReadOnlyComposable
fun topLevelLabel(route: String): String = when (route) {
    "today" -> tr("Today", "今天")
    "orbit" -> tr("Orbit", "习惯")
    "insights" -> tr("Insights", "洞察")
    "milestones" -> tr("Milestones", "里程碑")
    "me" -> tr("Me", "我的")
    else -> route
}

@Composable
@ReadOnlyComposable
fun sectionTitle(raw: String): String = when (raw) {
    "Morning" -> tr("Morning", "早晨")
    "Afternoon" -> tr("Afternoon", "下午")
    "Evening" -> tr("Evening", "晚上")
    "Anytime" -> tr("Anytime", "任意时间")
    "Completed" -> tr("Completed", "已完成")
    else -> raw
}

@Composable
@ReadOnlyComposable
fun bucketLabel(bucket: HabitTimeBucket): String = when (bucket) {
    HabitTimeBucket.MORNING -> tr("Morning", "早晨")
    HabitTimeBucket.AFTERNOON -> tr("Afternoon", "下午")
    HabitTimeBucket.EVENING -> tr("Evening", "晚上")
    HabitTimeBucket.ANYTIME -> tr("Anytime", "任意时间")
}

@Composable
@ReadOnlyComposable
fun frequencyLabel(type: FrequencyType): String = when (type) {
    FrequencyType.DAILY -> tr("Every day", "每天")
    FrequencyType.WEEKLY_DAYS -> tr("Specific weekdays", "指定星期")
    FrequencyType.WEEKLY_COUNT -> tr("Weekly count", "每周次数")
    FrequencyType.MONTHLY_DATES -> tr("Monthly dates", "每月日期")
    FrequencyType.INTERVAL -> tr("Every N days", "每隔 N 天")
}

@Composable
@ReadOnlyComposable
fun targetTypeLabel(type: TargetType): String = when (type) {
    TargetType.NONE -> tr("One tap", "一键打卡")
    TargetType.COUNT -> tr("Count", "次数")
    TargetType.MINUTES -> tr("Minutes", "分钟")
    TargetType.AMOUNT -> tr("Amount", "数量")
    TargetType.DISTANCE -> tr("Distance", "距离")
}

@Composable
@ReadOnlyComposable
fun goalTypeLabel(type: GoalType): String = when (type) {
    GoalType.OPEN_ENDED -> tr("Keep going", "持续进行")
    GoalType.TARGET_DATE -> tr("Target date", "目标日期")
    GoalType.TARGET_COMPLETIONS -> tr("Target completions", "目标次数")
}

@Composable
@ReadOnlyComposable
fun milestoneTypeLabel(type: MilestoneType): String = when (type) {
    MilestoneType.COUNTDOWN -> tr("Countdown", "倒计时")
    MilestoneType.SINCE -> tr("Since", "纪念日")
    MilestoneType.HABIT_GOAL -> tr("Habit goal", "习惯目标")
}

@Composable
@ReadOnlyComposable
fun taskQuadrantLabel(quadrant: TaskQuadrant): String = when (quadrant) {
    TaskQuadrant.IMPORTANT_URGENT -> tr("Important and urgent", "重要且紧急")
    TaskQuadrant.IMPORTANT_NOT_URGENT -> tr("Important not urgent", "重要不紧急")
    TaskQuadrant.NOT_IMPORTANT_URGENT -> tr("Not important but urgent", "不重要但紧急")
    TaskQuadrant.NOT_IMPORTANT_NOT_URGENT -> tr("Not important not urgent", "不重要不紧急")
}

@Composable
@ReadOnlyComposable
fun taskDueStateLabel(state: TaskDueState): String = when (state) {
    TaskDueState.OVERDUE -> tr("Overdue", "已逾期")
    TaskDueState.TODAY -> tr("Today", "今天")
    TaskDueState.UPCOMING -> tr("Upcoming", "即将到来")
    TaskDueState.SOMEDAY -> tr("Someday", "待安排")
}

@Composable
@ReadOnlyComposable
fun checkInStatusLabel(status: CheckInStatus?): String = when (status) {
    CheckInStatus.COMPLETED -> tr("Completed", "已完成")
    CheckInStatus.SKIPPED -> tr("Skipped", "已跳过")
    CheckInStatus.FAILED -> tr("Failed", "失败")
    null -> tr("No record", "暂无记录")
}

@Composable
@ReadOnlyComposable
fun shortWeekday(dayName: String): String {
    if (LocalAppLanguage.current != AppLanguage.CHINESE) return dayName
    return when (dayName.lowercase(Locale.ROOT)) {
        "monday" -> "周一"
        "tuesday" -> "周二"
        "wednesday" -> "周三"
        "thursday" -> "周四"
        "friday" -> "周五"
        "saturday" -> "周六"
        "sunday" -> "周日"
        else -> dayName
    }
}

@Composable
@ReadOnlyComposable
fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> tr("Mon", "周一")
    DayOfWeek.TUESDAY -> tr("Tue", "周二")
    DayOfWeek.WEDNESDAY -> tr("Wed", "周三")
    DayOfWeek.THURSDAY -> tr("Thu", "周四")
    DayOfWeek.FRIDAY -> tr("Fri", "周五")
    DayOfWeek.SATURDAY -> tr("Sat", "周六")
    DayOfWeek.SUNDAY -> tr("Sun", "周日")
}
