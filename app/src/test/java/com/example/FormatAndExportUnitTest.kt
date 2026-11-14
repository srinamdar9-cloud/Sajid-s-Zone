package com.example

import com.example.data.ExpenseCategory
import com.example.data.ExpenseItem
import com.example.data.JournalEntry
import com.example.data.PaymentMethod
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.export.ReportExporter
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FormatAndExportUnitTest {

  @Test
  fun formatInr_usesIndianNumberGrouping() {
    assertEquals("₹0.00", FormatUtils.formatInr(0.0))
    assertEquals("₹950.50", FormatUtils.formatInr(950.5))
    assertEquals("₹12,345.00", FormatUtils.formatInr(12345.0))
    assertEquals("₹1,23,456.75", FormatUtils.formatInr(123456.75))
    assertEquals("Rs. 1,00,00,000.00", FormatUtils.formatInr(10000000.0, "Rs. "))
  }

  @Test
  fun formatDisplayDate_andTime_formatCorrectly() {
    assertEquals("14/11/2026", FormatUtils.formatDisplayDate("2026-11-14"))
    assertEquals("02:30 PM", FormatUtils.formatTime("14:30", use24Hour = false))
    assertEquals("14:30", FormatUtils.formatTime("14:30", use24Hour = true))
  }

  @Test
  fun backupJson_roundTripPreservesRecords() {
    val tasks = listOf(TaskItem(title = "Review Budget", date = "2026-11-14"))
    val journal = listOf(JournalEntry(date = "2026-11-14", time = "20:00", title = "Day 1"))
    val expenses =
      listOf(
        ExpenseItem(
          amount = 250.0,
          description = "Fuel",
          category = ExpenseCategory.PetrolAndTransportation.label,
          date = "2026-11-14",
          time = "09:15",
          paymentMethod = PaymentMethod.UPI.label,
        )
      )
    val prefs = UserPreferences(themeMode = "Dark", use24HourTime = true)

    val json = ReportExporter.createBackupJson(tasks, journal, expenses, prefs)
    val parsed = ReportExporter.parseBackupJson(json)

    assertEquals(1, parsed.tasks.size)
    assertEquals("Review Budget", parsed.tasks.first().title)
    assertEquals(1, parsed.journal.size)
    assertEquals("Day 1", parsed.journal.first().title)
    assertEquals(1, parsed.expenses.size)
    assertTrue(parsed.expenses.first().amount == 250.0)
    assertEquals("Dark", parsed.preferences?.themeMode)
    assertTrue(parsed.preferences?.use24HourTime == true)
  }
}
