package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseCategory
import com.example.data.ExpenseItem
import com.example.data.PaymentMethod
import com.example.data.UserPreferences
import com.example.export.ReportExporter
import com.example.ui.components.CategoryBadge
import com.example.ui.components.CategoryBreakdownChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MaterialDatePickerModal
import com.example.ui.components.MaterialTimePickerModal
import com.example.ui.components.SingleSeriesBarChart
import com.example.ui.state.UiState
import com.example.ui.theme.JetBrainsMono
import com.example.util.FormatUtils
import java.util.Calendar

enum class ExpensePeriod(val label: String) {
  Day("Day"),
  Week("Week"),
  Month("Month"),
}

@Composable
fun ExpensesScreen(
  expensesState: UiState<List<ExpenseItem>>,
  preferences: UserPreferences,
  openNewExpenseTrigger: Int,
  onSaveExpense: (ExpenseItem) -> Unit,
  onDeleteExpense: (String) -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  val today = FormatUtils.todayIso()

  var period by remember { mutableStateOf(ExpensePeriod.Month) }
  var anchorDate by remember { mutableStateOf(today) }
  var selectedCategory by remember { mutableStateOf("All") }
  var searchQuery by remember { mutableStateOf("") }

  var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
  var showEditor by remember(openNewExpenseTrigger) { mutableStateOf(openNewExpenseTrigger > 0) }
  var showAnchorDatePicker by remember { mutableStateOf(false) }

  val allExpenses = (expensesState as? UiState.Success)?.data ?: emptyList()

  val periodLabel =
    when (period) {
      ExpensePeriod.Day -> FormatUtils.formatDisplayDate(anchorDate)
      ExpensePeriod.Week -> {
        val (s, e) = FormatUtils.weekRangeContaining(anchorDate)
        "${FormatUtils.formatDisplayDate(s)} - ${FormatUtils.formatDisplayDate(e)}"
      }
      ExpensePeriod.Month -> {
        val cal = FormatUtils.parseIsoDate(anchorDate)
        if (cal != null) {
          FormatUtils.formatMonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        } else {
          anchorDate
        }
      }
    }

  val filteredExpenses =
    remember(allExpenses, period, anchorDate, selectedCategory, searchQuery) {
      allExpenses.filter { item ->
        val matchesPeriod =
          when (period) {
            ExpensePeriod.Day -> item.date == anchorDate
            ExpensePeriod.Week -> {
              val (s, e) = FormatUtils.weekRangeContaining(anchorDate)
              item.date in s..e
            }
            ExpensePeriod.Month -> item.date.startsWith(FormatUtils.monthPrefix(anchorDate))
          }
        val matchesCat = selectedCategory == "All" || item.category == selectedCategory
        val q = searchQuery.trim().lowercase()
        val matchesQuery =
          q.isEmpty() ||
            item.description.lowercase().contains(q) ||
            item.category.lowercase().contains(q) ||
            item.paymentMethod.lowercase().contains(q) ||
            item.notes.lowercase().contains(q)
        matchesPeriod && matchesCat && matchesQuery
      }
    }

  val totalFiltered = filteredExpenses.sumOf { it.amount }
  val categoryTotals =
    remember(filteredExpenses) {
      ExpenseCategory.entries
        .mapNotNull { cat ->
          val sum = filteredExpenses.filter { it.category == cat.label }.sumOf { it.amount }
          if (sum > 0.0) cat.label to sum else null
        }
        .sortedByDescending { it.second }
    }

  val monthlySeries =
    remember(allExpenses, anchorDate) {
      (5 downTo 0).map { offset ->
        val mDate = FormatUtils.addMonthsIso(anchorDate, -offset)
        val prefix = FormatUtils.monthPrefix(mDate)
        val sum = allExpenses.filter { it.date.startsWith(prefix) }.sumOf { it.amount }
        FormatUtils.formatShortMonth(prefix) to sum
      }
    }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingExpense = null
          showEditor = true
        },
        modifier = Modifier.testTag("add_expense_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add expense")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("expenses_screen"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Total Summary Card + Export Buttons
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Total Spend ($periodLabel)",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = FormatUtils.formatInr(totalFiltered),
              style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMono),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${filteredExpenses.size} transactions",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = {
                  if (filteredExpenses.isEmpty()) {
                    onFeedback("No expenses in this period to export")
                    return@OutlinedButton
                  }
                  try {
                    val xlsx =
                      ReportExporter.exportExpensesXlsx(
                        context = context,
                        expenses = filteredExpenses,
                        periodLabel = periodLabel,
                        use24Hour = preferences.use24HourTime,
                      )
                    ReportExporter.shareFile(
                      context = context,
                      file = xlsx,
                      mimeType =
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                      title = "Share Expense Report (.xlsx)",
                    )
                  } catch (e: Exception) {
                    onFeedback("Excel export failed: ${e.message}")
                  }
                },
                modifier = Modifier.weight(1f).testTag("export_xlsx_button"),
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Excel .xlsx")
              }
              OutlinedButton(
                onClick = {
                  if (filteredExpenses.isEmpty()) {
                    onFeedback("No expenses in this period to export")
                    return@OutlinedButton
                  }
                  try {
                    val pdf =
                      ReportExporter.exportExpensesPdf(
                        context = context,
                        expenses = filteredExpenses,
                        periodLabel = periodLabel,
                        use24Hour = preferences.use24HourTime,
                      )
                    ReportExporter.shareFile(
                      context = context,
                      file = pdf,
                      mimeType = "application/pdf",
                      title = "Share Expense Report (.pdf)",
                    )
                  } catch (e: Exception) {
                    onFeedback("PDF export failed: ${e.message}")
                  }
                },
                modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PDF Report")
              }
            }
          }
        }
      }

      // Period Selector
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ExpensePeriod.entries.forEachIndexed { idx, p ->
            SegmentedButton(
              selected = period == p,
              onClick = { period = p },
              shape = SegmentedButtonDefaults.itemShape(idx, ExpensePeriod.entries.size),
            ) {
              Text(p.label)
            }
          }
        }
      }

      // Period Stepper
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(
            onClick = {
              anchorDate =
                when (period) {
                  ExpensePeriod.Day -> FormatUtils.addDaysIso(anchorDate, -1)
                  ExpensePeriod.Week -> FormatUtils.addDaysIso(anchorDate, -7)
                  ExpensePeriod.Month -> FormatUtils.addMonthsIso(anchorDate, -1)
                }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
          }
          TextButton(onClick = { showAnchorDatePicker = true }) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(periodLabel, style = MaterialTheme.typography.titleSmall)
          }
          IconButton(
            onClick = {
              anchorDate =
                when (period) {
                  ExpensePeriod.Day -> FormatUtils.addDaysIso(anchorDate, 1)
                  ExpensePeriod.Week -> FormatUtils.addDaysIso(anchorDate, 7)
                  ExpensePeriod.Month -> FormatUtils.addMonthsIso(anchorDate, 1)
                }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
          }
        }
      }

      // Search + Category Filter
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search description, method, notes...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("expense_search_input"),
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedCategory == "All",
            onClick = { selectedCategory = "All" },
            label = { Text("All") },
          )
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat.label,
              onClick = { selectedCategory = cat.label },
              label = { Text(cat.label) },
            )
          }
        }
      }

      // Monthly Comparison Chart + Category Breakdown
      if (allExpenses.isNotEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Text("6-Month Spending Trend", style = MaterialTheme.typography.titleSmall)
              SingleSeriesBarChart(
                values = monthlySeries,
                barColor = MaterialTheme.colorScheme.primary,
                showAllLabels = true,
              )
              if (categoryTotals.isNotEmpty()) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleSmall)
                CategoryBreakdownChart(categoryTotals = categoryTotals)
              }
            }
          }
        }
      }

      // Expense Ledger List
      if (expensesState is UiState.Error) {
        item { EmptyStateCard("Could not load expenses", expensesState.message) }
      } else if (filteredExpenses.isEmpty()) {
        item {
          EmptyStateCard(
            title = "No expenses for this selection",
            subtitle = "Tap + to record an expense in ₹ INR with category and payment method.",
            icon = Icons.Filled.AccountBalanceWallet,
            actionLabel = "Record Expense",
            onAction = {
              editingExpense = null
              showEditor = true
            },
          )
        }
      } else {
        items(filteredExpenses, key = { it.id }) { item ->
          Card(
            modifier = Modifier.fillMaxWidth().testTag("expense_item_${item.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                  Text(
                    text = item.description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      "${FormatUtils.formatDisplayDate(item.date)} • ${FormatUtils.formatTime(item.time, preferences.use24HourTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = FormatUtils.formatInr(item.amount),
                  style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  CategoryBadge(item.category)
                  CategoryBadge(item.paymentMethod)
                }
                Row {
                  IconButton(
                    onClick = {
                      editingExpense = item
                      showEditor = true
                    }
                  ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit expense")
                  }
                  IconButton(onClick = { onDeleteExpense(item.id) }) {
                    Icon(
                      Icons.Filled.Delete,
                      contentDescription = "Delete expense",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
              if (item.notes.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(72.dp)) }
    }
  }

  if (showAnchorDatePicker) {
    MaterialDatePickerModal(
      initialDateIso = anchorDate,
      onDismiss = { showAnchorDatePicker = false },
      onDateSelected = { anchorDate = it },
    )
  }

  if (showEditor) {
    ExpenseEditorDialog(
      initialExpense = editingExpense,
      defaultDateIso = anchorDate,
      use24Hour = preferences.use24HourTime,
      onDismiss = { showEditor = false },
      onSave = { saved ->
        onSaveExpense(saved)
        showEditor = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseEditorDialog(
  initialExpense: ExpenseItem?,
  defaultDateIso: String,
  use24Hour: Boolean,
  onDismiss: () -> Unit,
  onSave: (ExpenseItem) -> Unit,
) {
  var amountText by remember {
    mutableStateOf(
      if (initialExpense != null && initialExpense.amount > 0.0) {
        initialExpense.amount.toString()
      } else {
        ""
      }
    )
  }
  var description by remember { mutableStateOf(initialExpense?.description ?: "") }
  var category by remember {
    mutableStateOf(initialExpense?.category ?: ExpenseCategory.Grocery.label)
  }
  var date by remember {
    mutableStateOf(initialExpense?.date?.ifBlank { defaultDateIso } ?: defaultDateIso)
  }
  var time by remember {
    mutableStateOf(initialExpense?.time?.ifBlank { FormatUtils.nowTime24() } ?: FormatUtils.nowTime24())
  }
  var paymentMethod by remember {
    mutableStateOf(initialExpense?.paymentMethod ?: PaymentMethod.UPI.label)
  }
  var notes by remember { mutableStateOf(initialExpense?.notes ?: "") }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  var pickDate by remember { mutableStateOf(false) }
  var pickTime by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        if (initialExpense == null || initialExpense.id.isBlank()) "Record Expense" else "Edit Expense",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            errorMsg = null
          },
          label = { Text("Amount (₹ INR) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = {
            description = it.take(200)
            errorMsg = null
          },
          label = { Text("Description *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_description_input"),
        )

        if (errorMsg != null) {
          Text(
            text = errorMsg!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(FormatUtils.formatDisplayDate(date), maxLines = 1)
          }
          OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(FormatUtils.formatTime(time, use24Hour), maxLines = 1)
          }
        }

        Text("Payment Method", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = paymentMethod == method.label,
              onClick = { paymentMethod = method.label },
              label = { Text(method.label) },
            )
          }
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ExpenseCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat.label,
              onClick = { category = cat.label },
              label = { Text(cat.label) },
            )
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it.take(1000) },
          label = { Text("Notes (optional)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = amountText.toDoubleOrNull()
          if (parsed == null || parsed <= 0.0) {
            errorMsg = "Enter a valid amount greater than 0"
            return@Button
          }
          if (description.isBlank()) {
            errorMsg = "Description is required"
            return@Button
          }
          onSave(
            (initialExpense ?: ExpenseItem()).copy(
              amount = parsed,
              description = description.trim(),
              category = category,
              date = date,
              time = time,
              paymentMethod = paymentMethod,
              notes = notes.trim(),
            )
          )
        },
        modifier = Modifier.testTag("save_expense_button"),
      ) {
        Text("Save Expense")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )

  if (pickDate) {
    MaterialDatePickerModal(date, { pickDate = false }) { date = it }
  }
  if (pickTime) {
    MaterialTimePickerModal(time, use24Hour, { pickTime = false }) { time = it }
  }
}
