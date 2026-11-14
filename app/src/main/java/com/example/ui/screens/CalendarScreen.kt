package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseItem
import com.example.data.JournalEntry
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarViewMode(val label: String) {
  Day("Day"),
  Week("Week"),
  Month("Month"),
}

@Composable
fun CalendarScreen(
  tasksState: UiState<List<TaskItem>>,
  journalState: UiState<List<JournalEntry>>,
  expensesState: UiState<List<ExpenseItem>>,
  preferences: UserPreferences,
  isJournalUnlocked: Boolean,
  onToggleTask: (TaskItem) -> Unit,
  onSaveTask: (TaskItem) -> Unit,
  onOpenJournal: () -> Unit,
) {
  val today = FormatUtils.todayIso()
  var mode by remember { mutableStateOf(CalendarViewMode.Month) }
  var selectedDate by remember { mutableStateOf(today) }
  var showAddTaskDialog by remember { mutableStateOf(false) }

  val tasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val journals = (journalState as? UiState.Success)?.data ?: emptyList()
  val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()

  val tasksByDate = remember(tasks) { tasks.groupBy { it.date } }

  val dayTasks = tasksByDate[selectedDate] ?: emptyList()
  val daySpend = expenses.filter { it.date == selectedDate }.sumOf { it.amount }
  val dayJournals = journals.filter { it.date == selectedDate }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("calendar_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // View Mode Toggle
    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        CalendarViewMode.entries.forEachIndexed { idx, m ->
          SegmentedButton(
            selected = mode == m,
            onClick = { mode = m },
            shape = SegmentedButtonDefaults.itemShape(idx, CalendarViewMode.entries.size),
          ) {
            Text(m.label)
          }
        }
      }
    }

    // Period Stepper
    item {
      val headerTitle =
        when (mode) {
          CalendarViewMode.Day -> FormatUtils.formatFullDayAndDate(selectedDate)
          CalendarViewMode.Week -> {
            val (s, e) = FormatUtils.weekRangeContaining(selectedDate)
            "${FormatUtils.formatDisplayDate(s)} – ${FormatUtils.formatDisplayDate(e)}"
          }
          CalendarViewMode.Month -> {
            val cal = FormatUtils.parseIsoDate(selectedDate) ?: Calendar.getInstance()
            FormatUtils.formatMonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
          }
        }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            selectedDate =
              when (mode) {
                CalendarViewMode.Day -> FormatUtils.addDaysIso(selectedDate, -1)
                CalendarViewMode.Week -> FormatUtils.addDaysIso(selectedDate, -7)
                CalendarViewMode.Month -> FormatUtils.addMonthsIso(selectedDate, -1)
              }
          }
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }
        TextButton(onClick = { selectedDate = today }) {
          Text(headerTitle, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(
          onClick = {
            selectedDate =
              when (mode) {
                CalendarViewMode.Day -> FormatUtils.addDaysIso(selectedDate, 1)
                CalendarViewMode.Week -> FormatUtils.addDaysIso(selectedDate, 7)
                CalendarViewMode.Month -> FormatUtils.addMonthsIso(selectedDate, 1)
              }
          }
        ) {
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
      }
    }

    // Calendar Grid (Month or Week)
    if (mode == CalendarViewMode.Month) {
      item {
        MonthCalendarGrid(
          selectedDate = selectedDate,
          todayIso = today,
          tasksByDate = tasksByDate,
          onSelectDate = { selectedDate = it },
        )
      }
    } else if (mode == CalendarViewMode.Week) {
      item {
        WeekStripCalendar(
          selectedDate = selectedDate,
          todayIso = today,
          tasksByDate = tasksByDate,
          onSelectDate = { selectedDate = it },
        )
      }
    }

    // Legend
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        DotLegend("Overdue", StatusCoral)
        DotLegend("Pending", StatusAmber)
        DotLegend("Done", StatusEmerald)
      }
    }

    // Selected Date Summary + Add Task Button
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = FormatUtils.formatFullDayAndDate(selectedDate),
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Spending: ${FormatUtils.formatInr(daySpend)}  •  ${dayTasks.size} tasks",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Button(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("calendar_add_task_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Task for ${FormatUtils.formatDisplayDate(selectedDate)}")
          }
        }
      }
    }

    // Day's Tasks
    item {
      Text(
        text = "Tasks on ${FormatUtils.formatDisplayDate(selectedDate)}",
        style = MaterialTheme.typography.titleSmall,
      )
    }
    if (dayTasks.isEmpty()) {
      item {
        EmptyStateCard(
          title = "No tasks scheduled",
          subtitle = "Tap the button above to schedule a task for ${FormatUtils.formatDisplayDate(selectedDate)}.",
        )
      }
    } else {
      items(dayTasks, key = { it.id }) { task ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { onToggleTask(task) }) {
              Icon(
                imageVector =
                  if (task.isCompleted) Icons.Filled.CheckCircle
                  else Icons.Filled.RadioButtonUnchecked,
                contentDescription = "Toggle task",
                tint = if (task.isCompleted) StatusEmerald else MaterialTheme.colorScheme.primary,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val timeStr =
                when {
                  task.startTime.isNotBlank() && task.deadlineTime.isNotBlank() ->
                    "${FormatUtils.formatTime(task.startTime, preferences.use24HourTime)} – ${FormatUtils.formatTime(task.deadlineTime, preferences.use24HourTime)}"
                  task.deadlineTime.isNotBlank() ->
                    "Due ${FormatUtils.formatTime(task.deadlineTime, preferences.use24HourTime)}"
                  task.startTime.isNotBlank() ->
                    "At ${FormatUtils.formatTime(task.startTime, preferences.use24HourTime)}"
                  else -> task.category
                }
              Text(
                text = timeStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            PriorityBadge(task.priority)
          }
        }
      }
    }

    // Day's Journal Entries
    item {
      Text(
        text = "Journal Entries (${dayJournals.size})",
        style = MaterialTheme.typography.titleSmall,
      )
    }
    if (preferences.pinEnabled && !isJournalUnlocked) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().clickable { onOpenJournal() },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text(
              text = "Journal is PIN locked. Tap to unlock in Journal.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }
    } else if (dayJournals.isEmpty()) {
      item {
        Text(
          text = "No journal entries recorded on this date.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      items(dayJournals, key = { it.id }) { entry ->
        Card(
          modifier = Modifier.fillMaxWidth().clickable { onOpenJournal() },
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              Icons.Filled.Book,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = entry.title.ifBlank { "Journal Entry" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = entry.content.ifBlank { entry.accomplished },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  if (showAddTaskDialog) {
    TaskEditorDialog(
      initialTask = null,
      defaultDateIso = selectedDate,
      use24Hour = preferences.use24HourTime,
      onDismiss = { showAddTaskDialog = false },
      onSave = { task ->
        onSaveTask(task)
        showAddTaskDialog = false
      },
    )
  }
}

@Composable
private fun MonthCalendarGrid(
  selectedDate: String,
  todayIso: String,
  tasksByDate: Map<String, List<TaskItem>>,
  onSelectDate: (String) -> Unit,
) {
  val isoFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
  val cal = remember(selectedDate) { FormatUtils.parseIsoDate(selectedDate) ?: Calendar.getInstance() }
  val year = cal.get(Calendar.YEAR)
  val month = cal.get(Calendar.MONTH)

  val firstOfMonth =
    Calendar.getInstance().apply {
      set(year, month, 1, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
  val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
  val firstDow = firstOfMonth.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
  val leadingBlanks = (firstDow - Calendar.MONDAY + 7) % 7

  val cells = mutableListOf<String?>()
  repeat(leadingBlanks) { cells.add(null) }
  for (day in 1..daysInMonth) {
    val c =
      Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    cells.add(isoFmt.format(c.time))
  }
  while (cells.size % 7 != 0) cells.add(null)

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dow ->
          Text(
            text = dow,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
        }
      }

      cells.chunked(7).forEach { weekRow ->
        Row(modifier = Modifier.fillMaxWidth()) {
          weekRow.forEach { dateIso ->
            if (dateIso == null) {
              Spacer(modifier = Modifier.weight(1f).height(48.dp))
            } else {
              val isSelected = dateIso == selectedDate
              val isToday = dateIso == todayIso
              val dayNum = dateIso.takeLast(2).toIntOrNull()?.toString() ?: ""
              val dayTasks = tasksByDate[dateIso] ?: emptyList()

              val hasOverdue = dayTasks.any { !it.isCompleted && it.date < todayIso }
              val hasPending = dayTasks.any { !it.isCompleted && it.date >= todayIso }
              val hasDone = dayTasks.any { it.isCompleted }

              Box(
                modifier =
                  Modifier.weight(1f)
                    .height(48.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                      }
                    )
                    .then(
                      if (isToday && !isSelected) {
                        Modifier.border(
                          1.dp,
                          MaterialTheme.colorScheme.primary,
                          RoundedCornerShape(10.dp),
                        )
                      } else {
                        Modifier
                      }
                    )
                    .clickable { onSelectDate(dateIso) },
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = dayNum,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color =
                      if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                  )
                  Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (hasOverdue) DotIndicator(StatusCoral)
                    if (hasPending) DotIndicator(StatusAmber)
                    if (hasDone) DotIndicator(StatusEmerald)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeekStripCalendar(
  selectedDate: String,
  todayIso: String,
  tasksByDate: Map<String, List<TaskItem>>,
  onSelectDate: (String) -> Unit,
) {
  val (weekStart, _) = FormatUtils.weekRangeContaining(selectedDate)
  val weekDays = (0..6).map { FormatUtils.addDaysIso(weekStart, it) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      weekDays.forEach { dateIso ->
        val isSelected = dateIso == selectedDate
        val dayTasks = tasksByDate[dateIso] ?: emptyList()
        val hasOverdue = dayTasks.any { !it.isCompleted && it.date < todayIso }
        val hasPending = dayTasks.any { !it.isCompleted && it.date >= todayIso }
        val hasDone = dayTasks.any { it.isCompleted }

        Column(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
              )
              .clickable { onSelectDate(dateIso) }
              .padding(vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = FormatUtils.formatShortDay(dateIso),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = dateIso.takeLast(2),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasOverdue) DotIndicator(StatusCoral)
            if (hasPending) DotIndicator(StatusAmber)
            if (hasDone) DotIndicator(StatusEmerald)
          }
        }
      }
    }
  }
}

@Composable
private fun DotIndicator(color: androidx.compose.ui.graphics.Color) {
  Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
}

@Composable
private fun DotLegend(label: String, color: androidx.compose.ui.graphics.Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    DotIndicator(color)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
