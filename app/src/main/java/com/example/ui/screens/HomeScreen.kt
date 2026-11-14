package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseItem
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.ui.components.CompletionRingChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.SingleSeriesBarChart
import com.example.ui.state.NotificationFeedItem
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.util.FormatUtils
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
  tasksState: UiState<List<TaskItem>>,
  expensesState: UiState<List<ExpenseItem>>,
  preferences: UserPreferences,
  notificationFeed: List<NotificationFeedItem>,
  onQuickAddTask: () -> Unit,
  onQuickWriteJournal: () -> Unit,
  onQuickRecordExpense: () -> Unit,
  onOpenTasks: () -> Unit,
  onOpenExpenses: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenReports: () -> Unit,
) {
  var liveClock by remember(preferences.use24HourTime) {
    mutableStateOf(FormatUtils.nowClockFormatted(preferences.use24HourTime))
  }
  LaunchedEffect(preferences.use24HourTime) {
    while (true) {
      liveClock = FormatUtils.nowClockFormatted(preferences.use24HourTime)
      delay(1000L)
    }
  }

  val today = FormatUtils.todayIso()
  val monthPrefix = FormatUtils.monthPrefix(today)

  val tasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()

  val todayTasks = tasks.filter { it.date == today }
  val completedToday = todayTasks.count { it.isCompleted }
  val pendingToday = todayTasks.count { !it.isCompleted }
  val overdueCount = tasks.count { !it.isCompleted && it.date < today }

  val todaySpend = expenses.filter { it.date == today }.sumOf { it.amount }
  val monthSpend = expenses.filter { it.date.startsWith(monthPrefix) }.sumOf { it.amount }

  val last7DaysSpend =
    remember(expenses, today) {
      (6 downTo 0).map { offset ->
        val dayIso = FormatUtils.addDaysIso(today, -offset)
        val label = FormatUtils.formatShortDay(dayIso)
        val sum = expenses.filter { it.date == dayIso }.sumOf { it.amount }
        label to sum
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("home_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Hero Date & Live Clock Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = FormatUtils.formatFullDayAndDate(today),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = liveClock,
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.testTag("home_live_clock"),
          )
          Text(
            text =
              if (pendingToday == 0 && overdueCount == 0) {
                "All clear for today. Stay focused."
              } else {
                "$pendingToday pending today • $overdueCount overdue"
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
          )
        }
      }
    }

    // Quick Actions
    item {
      SectionHeaderRow(title = "Quick Actions")
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickActionButton(
          icon = Icons.Filled.AddTask,
          label = "Add Task",
          onClick = onQuickAddTask,
          modifier = Modifier.weight(1f).testTag("quick_add_task_button"),
        )
        QuickActionButton(
          icon = Icons.Filled.EditNote,
          label = "Journal",
          onClick = onQuickWriteJournal,
          modifier = Modifier.weight(1f).testTag("quick_write_journal_button"),
        )
        QuickActionButton(
          icon = Icons.Filled.AccountBalanceWallet,
          label = "Expense",
          onClick = onQuickRecordExpense,
          modifier = Modifier.weight(1f).testTag("quick_add_expense_button"),
        )
      }
    }

    // Tasks Summary KPI Grid
    item {
      SectionHeaderRow(
        title = "Today's Tasks",
        trailingText = "All Tasks",
        onTrailingClick = onOpenTasks,
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (tasksState is UiState.Loading) {
        BoxLoadingCard()
      } else if (tasksState is UiState.Error) {
        ErrorNoticeCard(tasksState.message)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            KpiStatCard(
              title = "Total Today",
              value = todayTasks.size.toString(),
              icon = Icons.Filled.PendingActions,
              accent = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f).clickable { onOpenTasks() },
            )
            KpiStatCard(
              title = "Completed",
              value = completedToday.toString(),
              icon = Icons.Filled.CheckCircle,
              accent = StatusEmerald,
              modifier = Modifier.weight(1f).clickable { onOpenTasks() },
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            KpiStatCard(
              title = "Pending",
              value = pendingToday.toString(),
              icon = Icons.Filled.Alarm,
              accent = StatusAmber,
              modifier = Modifier.weight(1f).clickable { onOpenTasks() },
            )
            KpiStatCard(
              title = "Overdue",
              value = overdueCount.toString(),
              icon = Icons.Filled.Warning,
              accent = StatusCoral,
              modifier = Modifier.weight(1f).clickable { onOpenNotifications() },
            )
          }
        }
      }
    }

    // Expenses Snapshot
    item {
      SectionHeaderRow(
        title = "Expenses Snapshot",
        trailingText = "Ledger",
        onTrailingClick = onOpenExpenses,
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (expensesState is UiState.Loading) {
        BoxLoadingCard()
      } else if (expensesState is UiState.Error) {
        ErrorNoticeCard(expensesState.message)
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ExpenseMetricCard(
            label = "Spent Today",
            amount = FormatUtils.formatInr(todaySpend),
            modifier = Modifier.weight(1f).clickable { onOpenExpenses() },
          )
          ExpenseMetricCard(
            label = "This Month",
            amount = FormatUtils.formatInr(monthSpend),
            modifier = Modifier.weight(1f).clickable { onOpenExpenses() },
          )
        }
      }
    }

    // Simple Charts (Task Completion + 7-Day Spend)
    item {
      SectionHeaderRow(
        title = "Pulse & Trends",
        trailingText = "Full Reports",
        onTrailingClick = onOpenReports,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val pct =
            if (todayTasks.isEmpty()) 0f
            else (completedToday.toFloat() / todayTasks.size.toFloat()) * 100f
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            CompletionRingChart(
              percentage = pct,
              label = "Today",
              sublabel = "$completedToday of ${todayTasks.size} done",
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "7-Day Spending",
                style = MaterialTheme.typography.titleSmall,
              )
              Spacer(modifier = Modifier.height(6.dp))
              SingleSeriesBarChart(
                values = last7DaysSpend,
                barColor = MaterialTheme.colorScheme.primary,
                showAllLabels = true,
              )
            }
          }
        }
      }
    }

    // Upcoming Reminders
    item {
      SectionHeaderRow(
        title = "Upcoming & Due Reminders",
        trailingText = "View All (${notificationFeed.size})",
        onTrailingClick = onOpenNotifications,
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (notificationFeed.isEmpty()) {
        EmptyStateCard(
          title = "No upcoming reminders",
          subtitle = "Add a reminder date/time or deadline to any task to see alerts here.",
          icon = Icons.Filled.Alarm,
          actionLabel = "Add a Task",
          onAction = onQuickAddTask,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          notificationFeed.take(4).forEach { item ->
            Card(
              modifier = Modifier.fillMaxWidth().clickable { onOpenNotifications() },
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                PriorityBadge(item.task.priority)
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun QuickActionButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier.height(54.dp),
    shape = RoundedCornerShape(14.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
  ) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
  }
}

@Composable
private fun KpiStatCard(
  title: String,
  value: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.padding(10.dp).size(20.dp),
        )
      }
      Column {
        Text(
          text = value,
          style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ExpenseMetricCard(
  label: String,
  amount: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = amount,
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun BoxLoadingCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Text("Syncing...", style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
private fun ErrorNoticeCard(message: String) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.onErrorContainer,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(14.dp),
    )
  }
}
