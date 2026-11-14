package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.notifications.TaskNotificationScheduler
import com.example.ui.components.CategoryBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.state.UiState
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.util.FormatUtils

enum class TaskFilter(val label: String) {
  Today("Today"),
  Upcoming("Upcoming"),
  Overdue("Overdue"),
  Done("Done"),
  All("All"),
}

enum class TaskViewMode(val label: String) {
  Daily("Daily"),
  Weekly("Weekly"),
  Monthly("Monthly"),
}

@Composable
fun TasksScreen(
  tasksState: UiState<List<TaskItem>>,
  preferences: UserPreferences,
  openNewTaskTrigger: Int,
  onSaveTask: (TaskItem) -> Unit,
  onToggleTask: (TaskItem) -> Unit,
  onRescheduleTask: (TaskItem, String, String, String, String, String) -> Unit,
  onDeleteTask: (String) -> Unit,
) {
  val context = LocalContext.current
  val today = FormatUtils.todayIso()

  var selectedFilter by remember { mutableStateOf(TaskFilter.All) }
  var viewMode by remember { mutableStateOf(TaskViewMode.Daily) }
  var anchorDate by remember { mutableStateOf(today) }
  var searchQuery by remember { mutableStateOf("") }

  var editorTask by remember { mutableStateOf<TaskItem?>(null) }
  var showEditor by remember(openNewTaskTrigger) { mutableStateOf(openNewTaskTrigger > 0) }
  var rescheduleTarget by remember { mutableStateOf<TaskItem?>(null) }

  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
    }

  val canExactAlarm = remember(tasksState) { TaskNotificationScheduler.canScheduleExactAlarms(context) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editorTask = null
          showEditor = true
        },
        modifier = Modifier.testTag("add_task_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add task")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("tasks_screen"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Android 13+ & Exact Alarm Permission Banner
      if (!hasNotificationPermission || !canExactAlarm) {
        item {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
              ),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Filled.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                  text = "Enable Reliable Reminders",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
              Text(
                text =
                  "Grant notification and exact alarm access so task alerts fire on time even when the app is closed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  Button(
                    onClick = {
                      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                  ) {
                    Text("Allow Notifications")
                  }
                }
                if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  OutlinedButton(
                    onClick = {
                      val intent =
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                          data = Uri.parse("package:${context.packageName}")
                        }
                      context.startActivity(intent)
                    }
                  ) {
                    Text("Exact Alarms")
                  }
                }
              }
            }
          }
        }
      }

      // Search
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search tasks, descriptions, categories...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("task_search_input"),
        )
      }

      // View mode (Daily / Weekly / Monthly)
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          TaskViewMode.entries.forEachIndexed { idx, mode ->
            SegmentedButton(
              selected = viewMode == mode,
              onClick = { viewMode = mode },
              shape = SegmentedButtonDefaults.itemShape(idx, TaskViewMode.entries.size),
            ) {
              Text(mode.label)
            }
          }
        }
      }

      // Period Stepper
      item {
        val periodLabel =
          when (viewMode) {
            TaskViewMode.Daily -> FormatUtils.formatDisplayDate(anchorDate)
            TaskViewMode.Weekly -> {
              val (s, e) = FormatUtils.weekRangeContaining(anchorDate)
              "${FormatUtils.formatDisplayDate(s)} – ${FormatUtils.formatDisplayDate(e)}"
            }
            TaskViewMode.Monthly -> {
              val cal = FormatUtils.parseIsoDate(anchorDate)
              if (cal != null) {
                FormatUtils.formatMonthYear(
                  cal.get(java.util.Calendar.YEAR),
                  cal.get(java.util.Calendar.MONTH),
                )
              } else {
                anchorDate
              }
            }
          }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(
            onClick = {
              anchorDate =
                when (viewMode) {
                  TaskViewMode.Daily -> FormatUtils.addDaysIso(anchorDate, -1)
                  TaskViewMode.Weekly -> FormatUtils.addDaysIso(anchorDate, -7)
                  TaskViewMode.Monthly -> FormatUtils.addMonthsIso(anchorDate, -1)
                }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period")
          }
          TextButton(onClick = { anchorDate = today }) {
            Text(periodLabel, style = MaterialTheme.typography.titleSmall)
          }
          IconButton(
            onClick = {
              anchorDate =
                when (viewMode) {
                  TaskViewMode.Daily -> FormatUtils.addDaysIso(anchorDate, 1)
                  TaskViewMode.Weekly -> FormatUtils.addDaysIso(anchorDate, 7)
                  TaskViewMode.Monthly -> FormatUtils.addMonthsIso(anchorDate, 1)
                }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next period")
          }
        }
      }

      // Filter Chips
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TaskFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = { Text(filter.label) },
            )
          }
        }
      }

      when (tasksState) {
        is UiState.Loading -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalArrangement = Arrangement.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            EmptyStateCard(
              title = "Could not load tasks",
              subtitle = tasksState.message,
            )
          }
        }
        is UiState.Success -> {
          val filtered =
            tasksState.data.filter { task ->
              val matchesFilter =
                when (selectedFilter) {
                  TaskFilter.Today -> task.date == today
                  TaskFilter.Upcoming -> !task.isCompleted && task.date > today
                  TaskFilter.Overdue -> !task.isCompleted && task.date < today
                  TaskFilter.Done -> task.isCompleted
                  TaskFilter.All -> true
                }
              val matchesPeriod =
                if (selectedFilter == TaskFilter.All) {
                  when (viewMode) {
                    TaskViewMode.Daily -> task.date == anchorDate
                    TaskViewMode.Weekly -> {
                      val (s, e) = FormatUtils.weekRangeContaining(anchorDate)
                      task.date in s..e
                    }
                    TaskViewMode.Monthly ->
                      task.date.startsWith(FormatUtils.monthPrefix(anchorDate))
                  }
                } else {
                  true
                }
              val q = searchQuery.trim().lowercase()
              val matchesQuery =
                q.isEmpty() ||
                  task.title.lowercase().contains(q) ||
                  task.description.lowercase().contains(q) ||
                  task.category.lowercase().contains(q)

              matchesFilter && matchesPeriod && matchesQuery
            }

          if (filtered.isEmpty()) {
            item {
              EmptyStateCard(
                title = "No tasks match this view",
                subtitle = "Tap + to add a task with start time, deadline, priority, and local alarm reminders.",
                actionLabel = "Create Task",
                onAction = {
                  editorTask = null
                  showEditor = true
                },
              )
            }
          } else {
            items(filtered, key = { it.id }) { task ->
              TaskCard(
                task = task,
                todayIso = today,
                use24Hour = preferences.use24HourTime,
                onToggle = { onToggleTask(task) },
                onEdit = {
                  editorTask = task
                  showEditor = true
                },
                onReschedule = { rescheduleTarget = task },
                onDelete = { onDeleteTask(task.id) },
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(72.dp)) }
    }
  }

  if (showEditor) {
    TaskEditorDialog(
      initialTask = editorTask,
      defaultDateIso = anchorDate,
      use24Hour = preferences.use24HourTime,
      onDismiss = { showEditor = false },
      onSave = { saved ->
        onSaveTask(saved)
        showEditor = false
      },
    )
  }

  rescheduleTarget?.let { target ->
    RescheduleTaskDialog(
      task = target,
      use24Hour = preferences.use24HourTime,
      onDismiss = { rescheduleTarget = null },
      onConfirm = { newDate, newStart, newDeadline, newRemDate, newRemTime ->
        onRescheduleTask(target, newDate, newStart, newDeadline, newRemDate, newRemTime)
        rescheduleTarget = null
      },
    )
  }
}

@Composable
private fun TaskCard(
  task: TaskItem,
  todayIso: String,
  use24Hour: Boolean,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onReschedule: () -> Unit,
  onDelete: () -> Unit,
) {
  val isOverdue = !task.isCompleted && task.date < todayIso
  Card(
    modifier = Modifier.fillMaxWidth().testTag("task_item_${task.id}"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.surface
      ),
    border =
      BorderStroke(
        1.dp,
        if (isOverdue) StatusCoral.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onToggle) {
          Icon(
            imageVector =
              if (task.isCompleted) Icons.Filled.CheckCircle
              else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (task.isCompleted) "Mark pending" else "Mark completed",
            tint = if (task.isCompleted) StatusEmerald else MaterialTheme.colorScheme.primary,
          )
        }
        Column(modifier = Modifier.weight(1f).clickable { onEdit() }) {
          Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          if (task.description.isNotBlank()) {
            Text(
              text = task.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        PriorityBadge(task.priority)
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CategoryBadge(task.category)
        CategoryBadge(FormatUtils.formatDisplayDate(task.date))
        if (task.startTime.isNotBlank()) {
          CategoryBadge("Start ${FormatUtils.formatTime(task.startTime, use24Hour)}")
        }
        if (task.deadlineTime.isNotBlank()) {
          CategoryBadge("Due ${FormatUtils.formatTime(task.deadlineTime, use24Hour)}")
        }
        if (task.reminderTime.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                Icons.Filled.Alarm,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = FormatUtils.formatTime(task.reminderTime, use24Hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
        if (task.recurrence != "None") {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                Icons.Filled.EventRepeat,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
              )
              Text(
                text = task.recurrence,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        if (!task.isCompleted) {
          TextButton(onClick = onReschedule) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reschedule")
          }
        }
        IconButton(onClick = onEdit) {
          Icon(Icons.Filled.Edit, contentDescription = "Edit task")
        }
        IconButton(onClick = onDelete) {
          Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}
