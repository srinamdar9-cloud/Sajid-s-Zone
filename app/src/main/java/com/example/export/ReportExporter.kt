package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.ExpenseCategory
import com.example.data.ExpenseItem
import com.example.data.JournalEntry
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.util.FormatUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

object ReportExporter {

  private fun exportDir(context: Context): File {
    val dir = File(context.cacheDir, "exports")
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser = Intent.createChooser(intent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  fun exportExpensesXlsx(
    context: Context,
    expenses: List<ExpenseItem>,
    periodLabel: String,
    use24Hour: Boolean,
  ): File {
    val safeName = periodLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(exportDir(context), "SajidsZone_Expenses_$safeName.xlsx")

    val sorted = expenses.sortedWith(compareBy<ExpenseItem> { it.date }.thenBy { it.time })
    val totalAmount = sorted.sumOf { it.amount }

    val categoryTotals =
      ExpenseCategory.entries.mapNotNull { cat ->
        val items = sorted.filter { it.category == cat.label }
        if (items.isEmpty()) null else Triple(cat.label, items.size, items.sumOf { it.amount })
      }.sortedByDescending { it.third }

    val sheet1Xml = buildExpensesSheetXml(sorted, totalAmount, use24Hour)
    val sheet2Xml = buildSummarySheetXml(periodLabel, sorted.size, totalAmount, categoryTotals)

    ZipOutputStream(FileOutputStream(file)).use { zip ->
      writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
      writeZipEntry(zip, "_rels/.rels", rootRelsXml())
      writeZipEntry(zip, "xl/workbook.xml", workbookXml())
      writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
      writeZipEntry(zip, "xl/styles.xml", stylesXml())
      writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml)
      writeZipEntry(zip, "xl/worksheets/sheet2.xml", sheet2Xml)
    }
    return file
  }

  fun exportExpensesPdf(
    context: Context,
    expenses: List<ExpenseItem>,
    periodLabel: String,
    use24Hour: Boolean,
  ): File {
    val safeName = periodLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(exportDir(context), "SajidsZone_Expenses_$safeName.pdf")

    val sorted = expenses.sortedWith(compareBy<ExpenseItem> { it.date }.thenBy { it.time })
    val totalAmount = sorted.sumOf { it.amount }
    val categoryTotals =
      ExpenseCategory.entries.mapNotNull { cat ->
        val items = sorted.filter { it.category == cat.label }
        if (items.isEmpty()) null else Triple(cat.label, items.size, items.sumOf { it.amount })
      }.sortedByDescending { it.third }

    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f

    val doc = PdfDocument()
    var pageNumber = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas

    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val subtitlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 11f
      }
    val headerPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F766E")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val textPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        textSize = 9.5f
      }
    val boldTextPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val linePaint =
      Paint().apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 1f
      }

    var y = 48f
    canvas.drawText("Sajid's Zone — Expense Report", margin, y, titlePaint)
    y += 18f
    canvas.drawText(
      "Period: $periodLabel  |  Generated: ${FormatUtils.formatDisplayDate(FormatUtils.todayIso())}",
      margin,
      y,
      subtitlePaint,
    )
    y += 16f
    canvas.drawText(
      "Total Transactions: ${sorted.size}   |   Total Spend: ${FormatUtils.formatInr(totalAmount, "Rs. ")}",
      margin,
      y,
      boldTextPaint,
    )
    y += 12f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 22f

    // Category Summary Section
    canvas.drawText("Category Summary", margin, y, headerPaint)
    y += 16f
    canvas.drawText("Category", margin, y, boldTextPaint)
    canvas.drawText("Count", 310f, y, boldTextPaint)
    canvas.drawText("Amount", 380f, y, boldTextPaint)
    canvas.drawText("Share", 500f, y, boldTextPaint)
    y += 6f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 14f

    for ((cat, count, sum) in categoryTotals) {
      if (y > pageHeight - 60f) {
        doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = 48f
      }
      val pct = if (totalAmount > 0.0) (sum / totalAmount) * 100.0 else 0.0
      canvas.drawText(cat.take(36), margin, y, textPaint)
      canvas.drawText(count.toString(), 310f, y, textPaint)
      canvas.drawText(FormatUtils.formatInr(sum, "Rs. "), 380f, y, textPaint)
      canvas.drawText(String.format(Locale.US, "%.1f%%", pct), 500f, y, textPaint)
      y += 15f
    }

    y += 14f
    if (y > pageHeight - 100f) {
      doc.finishPage(page)
      pageNumber++
      page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
      canvas = page.canvas
      y = 48f
    }

    // Itemized Expenses Table
    canvas.drawText("Expenses Ledger", margin, y, headerPaint)
    y += 16f
    canvas.drawText("Date", margin, y, boldTextPaint)
    canvas.drawText("Time", 105f, y, boldTextPaint)
    canvas.drawText("Description", 162f, y, boldTextPaint)
    canvas.drawText("Category", 320f, y, boldTextPaint)
    canvas.drawText("Method", 420f, y, boldTextPaint)
    canvas.drawText("Amount", 488f, y, boldTextPaint)
    y += 6f
    canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
    y += 14f

    for (item in sorted) {
      if (y > pageHeight - 50f) {
        doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = 48f
      }
      canvas.drawText(FormatUtils.formatDisplayDate(item.date), margin, y, textPaint)
      canvas.drawText(FormatUtils.formatTime(item.time, use24Hour), 105f, y, textPaint)
      canvas.drawText(item.description.take(26), 162f, y, textPaint)
      canvas.drawText(item.category.take(16), 320f, y, textPaint)
      canvas.drawText(item.paymentMethod.take(12), 420f, y, textPaint)
      canvas.drawText(FormatUtils.formatInr(item.amount, "Rs. "), 488f, y, textPaint)
      y += 15f
    }

    doc.finishPage(page)
    FileOutputStream(file).use { out -> doc.writeTo(out) }
    doc.close()
    return file
  }

  fun createBackupJson(
    tasks: List<TaskItem>,
    journalEntries: List<JournalEntry>,
    expenses: List<ExpenseItem>,
    preferences: UserPreferences,
  ): String {
    val root = JSONObject()
    root.put("app", "Sajid's Zone")
    root.put("version", 1)
    root.put("exportedAt", FormatUtils.todayIso() + " " + FormatUtils.nowTime24())

    val tasksArr = JSONArray()
    tasks.forEach { t ->
      tasksArr.put(
        JSONObject().apply {
          put("title", t.title)
          put("description", t.description)
          put("date", t.date)
          put("startTime", t.startTime)
          put("deadlineTime", t.deadlineTime)
          put("reminderDate", t.reminderDate)
          put("reminderTime", t.reminderTime)
          put("priority", t.priority)
          put("category", t.category)
          put("status", t.status)
          put("recurrence", t.recurrence)
          put("completedAtEpochMs", t.completedAtEpochMs)
        }
      )
    }
    root.put("tasks", tasksArr)

    val journalArr = JSONArray()
    journalEntries.forEach { j ->
      journalArr.put(
        JSONObject().apply {
          put("date", j.date)
          put("time", j.time)
          put("title", j.title)
          put("content", j.content)
          put("accomplished", j.accomplished)
          put("wentWell", j.wentWell)
          put("couldImprove", j.couldImprove)
          put("tomorrowFocus", j.tomorrowFocus)
        }
      )
    }
    root.put("journal", journalArr)

    val expArr = JSONArray()
    expenses.forEach { e ->
      expArr.put(
        JSONObject().apply {
          put("amount", e.amount)
          put("description", e.description)
          put("category", e.category)
          put("date", e.date)
          put("time", e.time)
          put("paymentMethod", e.paymentMethod)
          put("notes", e.notes)
        }
      )
    }
    root.put("expenses", expArr)

    root.put(
      "preferences",
      JSONObject().apply {
        put("themeMode", preferences.themeMode)
        put("use24HourTime", preferences.use24HourTime)
        put("notificationsEnabled", preferences.notificationsEnabled)
        put("headsUpMinutes", preferences.headsUpMinutes)
        put("overdueNudgeEnabled", preferences.overdueNudgeEnabled)
        put("lockJournalOnly", preferences.lockJournalOnly)
      },
    )
    return root.toString(2)
  }

  data class BackupPayload(
    val tasks: List<TaskItem>,
    val journal: List<JournalEntry>,
    val expenses: List<ExpenseItem>,
    val preferences: UserPreferences?,
  )

  fun parseBackupJson(jsonText: String): BackupPayload {
    val root = JSONObject(jsonText)
    val tasks = mutableListOf<TaskItem>()
    val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
    for (i in 0 until tasksArr.length()) {
      val o = tasksArr.getJSONObject(i)
      tasks.add(
        TaskItem(
          title = o.optString("title", "Untitled"),
          description = o.optString("description", ""),
          date = o.optString("date", FormatUtils.todayIso()),
          startTime = o.optString("startTime", ""),
          deadlineTime = o.optString("deadlineTime", ""),
          reminderDate = o.optString("reminderDate", ""),
          reminderTime = o.optString("reminderTime", ""),
          priority = o.optString("priority", "Medium"),
          category = o.optString("category", "Personal"),
          status = o.optString("status", "Pending"),
          recurrence = o.optString("recurrence", "None"),
          completedAtEpochMs = o.optLong("completedAtEpochMs", 0L),
        )
      )
    }

    val journal = mutableListOf<JournalEntry>()
    val journalArr = root.optJSONArray("journal") ?: JSONArray()
    for (i in 0 until journalArr.length()) {
      val o = journalArr.getJSONObject(i)
      journal.add(
        JournalEntry(
          date = o.optString("date", FormatUtils.todayIso()),
          time = o.optString("time", FormatUtils.nowTime24()),
          title = o.optString("title", ""),
          content = o.optString("content", ""),
          accomplished = o.optString("accomplished", ""),
          wentWell = o.optString("wentWell", ""),
          couldImprove = o.optString("couldImprove", ""),
          tomorrowFocus = o.optString("tomorrowFocus", ""),
        )
      )
    }

    val expenses = mutableListOf<ExpenseItem>()
    val expArr = root.optJSONArray("expenses") ?: JSONArray()
    for (i in 0 until expArr.length()) {
      val o = expArr.getJSONObject(i)
      val amount = o.optDouble("amount", 0.0)
      if (amount > 0.0) {
        expenses.add(
          ExpenseItem(
            amount = amount,
            description = o.optString("description", "Expense"),
            category = o.optString("category", "Other"),
            date = o.optString("date", FormatUtils.todayIso()),
            time = o.optString("time", FormatUtils.nowTime24()),
            paymentMethod = o.optString("paymentMethod", "UPI"),
            notes = o.optString("notes", ""),
          )
        )
      }
    }

    val prefObj = root.optJSONObject("preferences")
    val prefs =
      prefObj?.let {
        UserPreferences(
          themeMode = it.optString("themeMode", "System"),
          use24HourTime = it.optBoolean("use24HourTime", false),
          notificationsEnabled = it.optBoolean("notificationsEnabled", true),
          headsUpMinutes = it.optLong("headsUpMinutes", 15L),
          overdueNudgeEnabled = it.optBoolean("overdueNudgeEnabled", true),
          lockJournalOnly = it.optBoolean("lockJournalOnly", true),
        )
      }

    return BackupPayload(tasks, journal, expenses, prefs)
  }

  private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
  }

  private fun xmlEscape(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  private fun colName(indexZeroBased: Int): String {
    var n = indexZeroBased
    val sb = StringBuilder()
    while (n >= 0) {
      sb.insert(0, ('A' + (n % 26)))
      n = n / 26 - 1
    }
    return sb.toString()
  }

  private fun strCell(col: Int, row: Int, value: String, style: Int = 0): String {
    val ref = "${colName(col)}$row"
    return """<c r="$ref" t="inlineStr" s="$style"><is><t>${xmlEscape(value)}</t></is></c>"""
  }

  private fun numCell(col: Int, row: Int, value: Double, style: Int = 2): String {
    val ref = "${colName(col)}$row"
    return """<c r="$ref" s="$style"><v>${String.format(Locale.US, "%.2f", value)}</v></c>"""
  }

  private fun intCell(col: Int, row: Int, value: Int, style: Int = 0): String {
    val ref = "${colName(col)}$row"
    return """<c r="$ref" s="$style"><v>$value</v></c>"""
  }

  private fun buildExpensesSheetXml(
    expenses: List<ExpenseItem>,
    totalAmount: Double,
    use24Hour: Boolean,
  ): String {
    val rows = StringBuilder()
    val headers = listOf("Date", "Time", "Description", "Category", "Payment Method", "Amount (INR)", "Notes")
    rows.append("""<row r="1">""")
    headers.forEachIndexed { idx, h -> rows.append(strCell(idx, 1, h, style = 1)) }
    rows.append("</row>")

    expenses.forEachIndexed { i, item ->
      val r = i + 2
      rows.append("""<row r="$r">""")
      rows.append(strCell(0, r, FormatUtils.formatDisplayDate(item.date)))
      rows.append(strCell(1, r, FormatUtils.formatTime(item.time, use24Hour)))
      rows.append(strCell(2, r, item.description))
      rows.append(strCell(3, r, item.category))
      rows.append(strCell(4, r, item.paymentMethod))
      rows.append(numCell(5, r, item.amount, style = 2))
      rows.append(strCell(6, r, item.notes))
      rows.append("</row>")
    }

    val totalRow = expenses.size + 2
    rows.append("""<row r="$totalRow">""")
    rows.append(strCell(0, totalRow, "TOTAL", style = 1))
    rows.append(numCell(5, totalRow, totalAmount, style = 3))
    rows.append("</row>")

    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="14" customWidth="1"/>
    <col min="2" max="2" width="12" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="26" customWidth="1"/>
    <col min="5" max="5" width="16" customWidth="1"/>
    <col min="6" max="6" width="16" customWidth="1"/>
    <col min="7" max="7" width="30" customWidth="1"/>
  </cols>
  <sheetData>$rows</sheetData>
</worksheet>"""
  }

  private fun buildSummarySheetXml(
    periodLabel: String,
    count: Int,
    totalAmount: Double,
    categories: List<Triple<String, Int, Double>>,
  ): String {
    val rows = StringBuilder()
    rows.append("""<row r="1">""")
    rows.append(strCell(0, 1, "Sajid's Zone Expense Summary", style = 1))
    rows.append(strCell(1, 1, periodLabel, style = 1))
    rows.append("</row>")

    rows.append("""<row r="2">""")
    rows.append(strCell(0, 2, "Total Transactions"))
    rows.append(intCell(1, 2, count))
    rows.append("</row>")

    rows.append("""<row r="3">""")
    rows.append(strCell(0, 3, "Total Spend (INR)"))
    rows.append(numCell(1, 3, totalAmount, style = 3))
    rows.append("</row>")

    rows.append("""<row r="5">""")
    rows.append(strCell(0, 5, "Category", style = 1))
    rows.append(strCell(1, 5, "Transactions", style = 1))
    rows.append(strCell(2, 5, "Total (INR)", style = 1))
    rows.append(strCell(3, 5, "Share %", style = 1))
    rows.append("</row>")

    categories.forEachIndexed { idx, (cat, cCount, cSum) ->
      val r = 6 + idx
      val pct = if (totalAmount > 0.0) (cSum / totalAmount) * 100.0 else 0.0
      rows.append("""<row r="$r">""")
      rows.append(strCell(0, r, cat))
      rows.append(intCell(1, r, cCount))
      rows.append(numCell(2, r, cSum, style = 2))
      rows.append(numCell(3, r, pct, style = 2))
      rows.append("</row>")
    }

    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="18" customWidth="1"/>
    <col min="4" max="4" width="14" customWidth="1"/>
  </cols>
  <sheetData>$rows</sheetData>
</worksheet>"""
  }

  private fun contentTypesXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

  private fun rootRelsXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private fun workbookXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Expenses" sheetId="1" r:id="rId1"/>
    <sheet name="Summary" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

  private fun workbookRelsXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

  private fun stylesXml(): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="2" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
    <xf numFmtId="2" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/>
  </cellXfs>
</styleSheet>"""
}
