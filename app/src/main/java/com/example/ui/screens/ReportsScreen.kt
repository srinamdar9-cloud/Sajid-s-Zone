package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseCategory
import com.example.data.ExpenseItem
import com.example.data.TaskItem
import com.example.ui.components.BarDatum
import com.example.ui.components.CategoryBreakdownChart
import com.example.ui.components.CompletionRingChart
import com.example.ui.components.DualBarChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SingleSeriesBarChart
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.util.FormatUtils

@Composable
fun ReportsScreen(
  tasksState: UiState<List<TaskItem>>,
  expensesState: UiState<List<ExpenseItem>>,
) {
  val today = FormatUtils.todayIso()
  val monthPrefix = FormatUtils.monthPrefix(today)

  val tasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()

  val todayTasks = tasks.filter { it.date == today }
  val todayDone = todayTasks.count { it.isCompleted }
  val todayPct =
    if (todayTasks.isEmpty()) 0f else (todayDone.toFloat() / todayTasks.size.toFloat()) * 100f

  val monthTasks = tasks.filter { it.date.startsWith(monthPrefix) }
  val monthDone = monthTasks.count { it.isCompleted }
  val monthPct =
    if (monthTasks.isEmpty()) 0f else (monthDone.toFloat() / monthTasks.size.toFloat()) * 100f

  val pendingCount = tasks.count { !it.isCompleted && it.date >= today }
  val overdueCount = tasks.count { !it.isCompleted && it.date < today }

  val weeklyProductivity =
    remember(tasks, today) {
      (6 downTo 0).map { offset ->
        val dayIso = FormatUtils.addDaysIso(today, -offset)
        val dayList = tasks.filter { it.date == dayIso }
        BarDatum(
          label = FormatUtils.formatShortDay(dayIso),
          primaryValue = dayList.count { it.isCompleted }.toFloat(),
          secondaryValue = dayList.count { !it.isCompleted }.toFloat(),
        )
      }
    }

  val last30DaysExpenses =
    remember(expenses, today) {
      (29 downTo 0).map { offset ->
        val dayIso = FormatUtils.addDaysIso(today, -offset)
        val sum = expenses.filter { it.date == dayIso }.sumOf { it.amount }
        FormatUtils.formatDisplayDate(dayIso).take(5) to sum
      }
    }

  val monthlyComparison =
    remember(expenses, today) {
      (5 downTo 0).map { offset ->
        val mDate = FormatUtils.addMonthsIso(today, -offset)
        val prefix = FormatUtils.monthPrefix(mDate)
        val sum = expenses.filter { it.date.startsWith(prefix) }.sumOf { it.amount }
        FormatUtils.formatShortMonth(prefix) to sum
      }
    }

  val categoryBreakdown =
    remember(expenses, monthPrefix) {
      val monthExp = expenses.filter { it.date.startsWith(monthPrefix) }
      ExpenseCategory.entries
        .mapNotNull { cat ->
          val sum = monthExp.filter { it.category == cat.label }.sumOf { it.amount }
          if (sum > 0.0) cat.label to sum else null
        }
        .sortedByDescending { it.second }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("reports_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text("Productivity & Expense Reports", style = MaterialTheme.typography.headlineSmall)
    }

    // Daily & Monthly Task Completion %
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Task Completion Rate", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            CompletionRingChart(
              percentage = todayPct,
              label = "Daily",
              sublabel = "$todayDone / ${todayTasks.size} tasks",
              ringColor = StatusEmerald,
            )
            CompletionRingChart(
              percentage = monthPct,
              label = "Monthly",
              sublabel = "$monthDone / ${monthTasks.size} tasks",
              ringColor = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }

    // Pending & Overdue Report
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StatusAmber.copy(alpha = 0.12f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = pendingCount.toString(),
              style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
              color = StatusAmber,
              fontWeight = FontWeight.Bold,
            )
            Text("Pending Tasks", style = MaterialTheme.typography.labelLarge)
          }
        }
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StatusCoral.copy(alpha = 0.12f)),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = overdueCount.toString(),
              style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
              color = StatusCoral,
              fontWeight = FontWeight.Bold,
            )
            Text("Overdue Tasks", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    // Weekly Productivity Chart
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Weekly Productivity (Completed vs Pending)", style = MaterialTheme.typography.titleMedium)
          DualBarChart(
            data = weeklyProductivity,
            primaryColor = StatusEmerald,
            secondaryColor = StatusAmber,
          )
        }
      }
    }

    // Daily Expense Summary (Last 30 Days)
    item {
      val total30 = last30DaysExpenses.sumOf { it.second }
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Daily Expenses (Last 30 Days)", style = MaterialTheme.typography.titleMedium)
            Text(
              text = FormatUtils.formatInr(total30),
              style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
              color = MaterialTheme.colorScheme.primary,
            )
          }
          SingleSeriesBarChart(
            values = last30DaysExpenses,
            barColor = MaterialTheme.colorScheme.primary,
            showAllLabels = false,
          )
        }
      }
    }

    // Monthly Expense Comparison
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Monthly Expense Comparison (6 Months)", style = MaterialTheme.typography.titleMedium)
          SingleSeriesBarChart(
            values = monthlyComparison,
            barColor = MaterialTheme.colorScheme.tertiary,
            showAllLabels = true,
          )
        }
      }
    }

    // Category Breakdown
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("This Month's Category Breakdown", style = MaterialTheme.typography.titleMedium)
          if (categoryBreakdown.isEmpty()) {
            EmptyStateCard(
              title = "No expenses this month",
              subtitle = "Record expenses to view category proportions here.",
            )
          } else {
            CategoryBreakdownChart(categoryTotals = categoryBreakdown)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }
}
