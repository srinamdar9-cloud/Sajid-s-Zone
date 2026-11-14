package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.util.FormatUtils
import java.util.Calendar
import kotlin.math.abs

object TaskNotificationScheduler {
  const val CHANNEL_ID = "sajids_zone_reminders"
  const val CHANNEL_NAME = "Tasks & Reminders"
  const val CHANNEL_DESC = "Scheduled task reminders, heads-up alerts, due alerts, and overdue nudges"

  const val EXTRA_TITLE = "extra_title"
  const val EXTRA_BODY = "extra_body"
  const val EXTRA_NOTIF_ID = "extra_notif_id"
  const val EXTRA_KIND = "extra_kind"

  private const val OVERDUE_NUDGE_REQ_CODE = 990001
  private const val TAG = "TaskNotifScheduler"

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val existing = manager.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESC
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
  }

  fun rescheduleAll(
    context: Context,
    tasks: List<TaskItem>,
    preferences: UserPreferences,
  ) {
    ensureNotificationChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // First cancel any existing alarms for all known tasks so edits/completions/deletions stay clean
    tasks.forEach { cancelForTask(context, it.id) }
    cancelOverdueNudge(context)

    if (!preferences.notificationsEnabled) return

    val now = System.currentTimeMillis()
    val today = FormatUtils.todayIso()
    var overdueCount = 0

    for (task in tasks) {
      if (task.isCompleted || task.id.isBlank()) continue

      if (task.date < today) {
        overdueCount++
      }

      // 1. Custom Reminder at reminderDate + reminderTime
      val remDate = task.reminderDate.ifBlank { task.date }
      if (task.reminderTime.isNotBlank()) {
        val remMillis = FormatUtils.combineDateTimeMillis(remDate, task.reminderTime)
        if (remMillis != null && remMillis > now) {
          scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = requestCodeFor(task.id, 1),
            triggerAtMillis = remMillis,
            title = "Reminder: ${task.title}",
            body = buildTaskSubtitle(task, preferences.use24HourTime),
            kind = "reminder",
          )
        }
      }

      // 2. Heads-up notification before startTime
      if (task.startTime.isNotBlank() && preferences.headsUpMinutes > 0L) {
        val startMillis = FormatUtils.combineDateTimeMillis(task.date, task.startTime)
        if (startMillis != null) {
          val headsUpMillis = startMillis - (preferences.headsUpMinutes * 60_000L)
          if (headsUpMillis > now) {
            scheduleAlarm(
              context = context,
              alarmManager = alarmManager,
              requestCode = requestCodeFor(task.id, 2),
              triggerAtMillis = headsUpMillis,
              title = "Starting in ${preferences.headsUpMinutes}m: ${task.title}",
              body = "Starts at ${FormatUtils.formatTime(task.startTime, preferences.use24HourTime)} (${task.category})",
              kind = "headsup",
            )
          }
        }
      }

      // 3. Due notification at deadlineTime (or startTime if deadlineTime is empty)
      val dueTime = task.deadlineTime.ifBlank { task.startTime }
      if (dueTime.isNotBlank()) {
        val dueMillis = FormatUtils.combineDateTimeMillis(task.date, dueTime)
        if (dueMillis != null && dueMillis > now) {
          scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            requestCode = requestCodeFor(task.id, 3),
            triggerAtMillis = dueMillis,
            title = "Task Due: ${task.title}",
            body = "Due now • Priority: ${task.priority} • ${task.category}",
            kind = "due",
          )
        }
      }
    }

    // 4. Daily overdue nudge at 09:00 AM if overdue tasks exist
    if (preferences.overdueNudgeEnabled && overdueCount > 0) {
      val nudgeCal =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 9)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
          if (timeInMillis <= now) {
            add(Calendar.DAY_OF_YEAR, 1)
          }
        }
      val intent =
        Intent(context, TaskReminderReceiver::class.java).apply {
          putExtra(EXTRA_TITLE, "Overdue Tasks Nudge")
          putExtra(
            EXTRA_BODY,
            "You have $overdueCount overdue pending ${if (overdueCount == 1) "task" else "tasks"} in Sajid's Zone.",
          )
          putExtra(EXTRA_NOTIF_ID, OVERDUE_NUDGE_REQ_CODE)
          putExtra(EXTRA_KIND, "overdue")
        }
      val pending =
        PendingIntent.getBroadcast(
          context,
          OVERDUE_NUDGE_REQ_CODE,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        nudgeCal.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pending,
      )
    }
  }

  fun cancelForTask(context: Context, taskId: String) {
    if (taskId.isBlank()) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    for (slot in 1..3) {
      val reqCode = requestCodeFor(taskId, slot)
      val intent = Intent(context, TaskReminderReceiver::class.java)
      val pending =
        PendingIntent.getBroadcast(
          context,
          reqCode,
          intent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
      if (pending != null) {
        alarmManager.cancel(pending)
        pending.cancel()
      }
    }
  }

  fun cancelOverdueNudge(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TaskReminderReceiver::class.java)
    val pending =
      PendingIntent.getBroadcast(
        context,
        OVERDUE_NUDGE_REQ_CODE,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (pending != null) {
      alarmManager.cancel(pending)
      pending.cancel()
    }
  }

  private fun scheduleAlarm(
    context: Context,
    alarmManager: AlarmManager,
    requestCode: Int,
    triggerAtMillis: Long,
    title: String,
    body: String,
    kind: String,
  ) {
    val intent =
      Intent(context, TaskReminderReceiver::class.java).apply {
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_BODY, body)
        putExtra(EXTRA_NOTIF_ID, requestCode)
        putExtra(EXTRA_KIND, kind)
      }
    val pending =
      PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    try {
      if (canScheduleExactAlarms(context)) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
      } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
      }
    } catch (se: SecurityException) {
      Log.w(TAG, "Exact alarm permission unavailable, falling back to inexact alarm", se)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
    }
  }

  private fun requestCodeFor(taskId: String, slot: Int): Int {
    val hash = abs(taskId.hashCode()) % 100_000
    return hash * 10 + slot
  }

  private fun buildTaskSubtitle(task: TaskItem, use24Hour: Boolean): String {
    val dateText = FormatUtils.formatDisplayDate(task.date)
    val timeText =
      when {
        task.deadlineTime.isNotBlank() ->
          "Due ${FormatUtils.formatTime(task.deadlineTime, use24Hour)}"
        task.startTime.isNotBlank() ->
          "Starts ${FormatUtils.formatTime(task.startTime, use24Hour)}"
        else -> "Scheduled"
      }
    return "$dateText • $timeText • ${task.priority}"
  }
}
