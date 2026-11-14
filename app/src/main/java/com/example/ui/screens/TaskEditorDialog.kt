package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.TaskCategory
import com.example.data.TaskItem
import com.example.data.TaskPriority
import com.example.data.TaskRecurrence
import com.example.data.TaskStatus
import com.example.ui.components.MaterialDatePickerModal
import com.example.ui.components.MaterialTimePickerModal
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
  initialTask: TaskItem?,
  defaultDateIso: String,
  use24Hour: Boolean,
  onDismiss: () -> Unit,
  onSave: (TaskItem) -> Unit,
) {
  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var description by remember { mutableStateOf(initialTask?.description ?: "") }
  var date by remember { mutableStateOf(initialTask?.date?.ifBlank { defaultDateIso } ?: defaultDateIso) }
  var startTime by remember { mutableStateOf(initialTask?.startTime ?: "") }
  var deadlineTime by remember { mutableStateOf(initialTask?.deadlineTime ?: "") }
  var reminderDate by remember { mutableStateOf(initialTask?.reminderDate ?: "") }
  var reminderTime by remember { mutableStateOf(initialTask?.reminderTime ?: "") }
  var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.Medium.label) }
  var category by remember { mutableStateOf(initialTask?.category ?: TaskCategory.Personal.label) }
  var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: TaskRecurrence.None.label) }
  var status by remember { mutableStateOf(initialTask?.status ?: TaskStatus.Pending.label) }
  var titleError by remember { mutableStateOf(false) }

  var pickTaskDate by remember { mutableStateOf(false) }
  var pickReminderDate by remember { mutableStateOf(false) }
  var pickStartTime by remember { mutableStateOf(false) }
  var pickDeadlineTime by remember { mutableStateOf(false) }
  var pickReminderTime by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialTask == null || initialTask.id.isBlank()) "New Task" else "Edit Task",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it.take(160)
            titleError = false
          },
          label = { Text("Task Title *") },
          isError = titleError,
          supportingText = if (titleError) { { Text("Title is required") } } else null,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(2000) },
          label = { Text("Description") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth().testTag("task_description_input"),
        )

        // Task Date
        OutlinedButton(
          onClick = { pickTaskDate = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(Icons.Filled.CalendarToday, contentDescription = null)
          Spacer(modifier = Modifier.weight(0.1f))
          Text("Date: ${FormatUtils.formatDisplayDate(date)}", modifier = Modifier.weight(1f))
        }

        // Start & Deadline Time
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { pickStartTime = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Schedule, contentDescription = null)
            Text(
              text =
                if (startTime.isBlank()) " Start"
                else " ${FormatUtils.formatTime(startTime, use24Hour)}",
              maxLines = 1,
            )
          }
          OutlinedButton(
            onClick = { pickDeadlineTime = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Schedule, contentDescription = null)
            Text(
              text =
                if (deadlineTime.isBlank()) " Due"
                else " ${FormatUtils.formatTime(deadlineTime, use24Hour)}",
              maxLines = 1,
            )
          }
        }

        // Reminder Date & Time
        Text("Reminder Notification", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = { pickReminderDate = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text =
                if (reminderDate.isBlank()) "Rem. Date"
                else FormatUtils.formatDisplayDate(reminderDate),
              maxLines = 1,
            )
          }
          OutlinedButton(
            onClick = { pickReminderTime = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text =
                if (reminderTime.isBlank()) "Rem. Time"
                else FormatUtils.formatTime(reminderTime, use24Hour),
              maxLines = 1,
            )
          }
          if (reminderDate.isNotBlank() || reminderTime.isNotBlank()) {
            IconButton(
              onClick = {
                reminderDate = ""
                reminderTime = ""
              }
            ) {
              Icon(Icons.Filled.Clear, contentDescription = "Clear reminder")
            }
          }
        }

        // Priority
        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TaskPriority.entries.forEach { p ->
            FilterChip(
              selected = priority == p.label,
              onClick = { priority = p.label },
              label = { Text(p.label) },
            )
          }
        }

        // Category
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          TaskCategory.entries.forEach { c ->
            FilterChip(
              selected = category == c.label,
              onClick = { category = c.label },
              label = { Text(c.label) },
            )
          }
        }

        // Recurrence
        Text("Recurring Reminder", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          TaskRecurrence.entries.forEach { r ->
            FilterChip(
              selected = recurrence == r.label,
              onClick = { recurrence = r.label },
              label = { Text(r.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isBlank()) {
            titleError = true
            return@Button
          }
          val resolvedReminderDate =
            if (reminderTime.isNotBlank() && reminderDate.isBlank()) date else reminderDate
          val saved =
            (initialTask ?: TaskItem()).copy(
              title = title.trim(),
              description = description.trim(),
              date = date,
              startTime = startTime,
              deadlineTime = deadlineTime,
              reminderDate = resolvedReminderDate,
              reminderTime = reminderTime,
              priority = priority,
              category = category,
              recurrence = recurrence,
              status = status,
            )
          onSave(saved)
        },
        modifier = Modifier.testTag("save_task_button"),
      ) {
        Text("Save Task")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )

  if (pickTaskDate) {
    MaterialDatePickerModal(
      initialDateIso = date,
      onDismiss = { pickTaskDate = false },
      onDateSelected = { date = it },
    )
  }
  if (pickReminderDate) {
    MaterialDatePickerModal(
      initialDateIso = reminderDate.ifBlank { date },
      onDismiss = { pickReminderDate = false },
      onDateSelected = { reminderDate = it },
    )
  }
  if (pickStartTime) {
    MaterialTimePickerModal(
      initialTime24 = startTime.ifBlank { FormatUtils.nowTime24() },
      use24Hour = use24Hour,
      onDismiss = { pickStartTime = false },
      onTimeSelected = { startTime = it },
    )
  }
  if (pickDeadlineTime) {
    MaterialTimePickerModal(
      initialTime24 = deadlineTime.ifBlank { startTime.ifBlank { "18:00" } },
      use24Hour = use24Hour,
      onDismiss = { pickDeadlineTime = false },
      onTimeSelected = { deadlineTime = it },
    )
  }
  if (pickReminderTime) {
    MaterialTimePickerModal(
      initialTime24 = reminderTime.ifBlank { startTime.ifBlank { "09:00" } },
      use24Hour = use24Hour,
      onDismiss = { pickReminderTime = false },
      onTimeSelected = { reminderTime = it },
    )
  }
}

@Composable
fun RescheduleTaskDialog(
  task: TaskItem,
  use24Hour: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String) -> Unit,
) {
  var date by remember { mutableStateOf(task.date.ifBlank { FormatUtils.todayIso() }) }
  var startTime by remember { mutableStateOf(task.startTime) }
  var deadlineTime by remember { mutableStateOf(task.deadlineTime) }
  var remDate by remember { mutableStateOf(task.reminderDate) }
  var remTime by remember { mutableStateOf(task.reminderTime) }

  var pickDate by remember { mutableStateOf(false) }
  var pickStart by remember { mutableStateOf(false) }
  var pickDeadline by remember { mutableStateOf(false) }
  var pickRemDate by remember { mutableStateOf(false) }
  var pickRemTime by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reschedule Task") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(task.title, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
          Text("New Date: ${FormatUtils.formatDisplayDate(date)}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
            Text(
              if (startTime.isBlank()) "Start Time"
              else FormatUtils.formatTime(startTime, use24Hour)
            )
          }
          OutlinedButton(onClick = { pickDeadline = true }, modifier = Modifier.weight(1f)) {
            Text(
              if (deadlineTime.isBlank()) "Deadline"
              else FormatUtils.formatTime(deadlineTime, use24Hour)
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { pickRemDate = true }, modifier = Modifier.weight(1f)) {
            Text(
              if (remDate.isBlank()) "Rem. Date"
              else FormatUtils.formatDisplayDate(remDate)
            )
          }
          OutlinedButton(onClick = { pickRemTime = true }, modifier = Modifier.weight(1f)) {
            Text(
              if (remTime.isBlank()) "Rem. Time"
              else FormatUtils.formatTime(remTime, use24Hour)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val resolvedRemDate = if (remTime.isNotBlank() && remDate.isBlank()) date else remDate
          onConfirm(date, startTime, deadlineTime, resolvedRemDate, remTime)
        }
      ) {
        Text("Reschedule")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )

  if (pickDate) {
    MaterialDatePickerModal(date, { pickDate = false }) { date = it }
  }
  if (pickRemDate) {
    MaterialDatePickerModal(remDate.ifBlank { date }, { pickRemDate = false }) { remDate = it }
  }
  if (pickStart) {
    MaterialTimePickerModal(startTime.ifBlank { "09:00" }, use24Hour, { pickStart = false }) {
      startTime = it
    }
  }
  if (pickDeadline) {
    MaterialTimePickerModal(deadlineTime.ifBlank { "18:00" }, use24Hour, { pickDeadline = false }) {
      deadlineTime = it
    }
  }
  if (pickRemTime) {
    MaterialTimePickerModal(remTime.ifBlank { "08:45" }, use24Hour, { pickRemTime = false }) {
      remTime = it
    }
  }
}
