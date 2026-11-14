package com.example.util

import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
  private val isoDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

  private val displayDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }

  private val dayOfWeekShortFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEE", Locale.US)

  private val dayOfWeekFullFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEEE", Locale.US)

  private val monthYearFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMMM yyyy", Locale.US)

  private val shortMonthFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM yy", Locale.US)

  fun todayIso(): String = isoDateFormat.format(Date())

  fun nowTime24(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

  fun nowClockFormatted(use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm:ss" else "hh:mm:ss a"
    return SimpleDateFormat(pattern, Locale.US).format(Date())
  }

  fun parseIsoDate(iso: String): Calendar? {
    if (iso.length != 10) return null
    return try {
      val date = isoDateFormat.parse(iso) ?: return null
      Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    } catch (_: Exception) {
      null
    }
  }

  fun formatDisplayDate(iso: String): String {
    val cal = parseIsoDate(iso) ?: return iso
    return displayDateFormat.format(cal.time)
  }

  fun formatFullDayAndDate(iso: String): String {
    val cal = parseIsoDate(iso) ?: return iso
    return "${dayOfWeekFullFormat.format(cal.time)}, ${displayDateFormat.format(cal.time)}"
  }

  fun formatShortDay(iso: String): String {
    val cal = parseIsoDate(iso) ?: return ""
    return dayOfWeekShortFormat.format(cal.time)
  }

  fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroIndexed)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    return monthYearFormat.format(cal.time)
  }

  fun formatShortMonth(yearMonthIso: String): String {
    val cal = parseIsoDate("$yearMonthIso-01") ?: return yearMonthIso
    return shortMonthFormat.format(cal.time)
  }

  fun formatTime(time24: String, use24Hour: Boolean): String {
    if (time24.isBlank()) return ""
    val parts = time24.split(":")
    if (parts.size != 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1].toIntOrNull() ?: return time24
    if (use24Hour) {
      return String.format(Locale.US, "%02d:%02d", hour, minute)
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
      hour == 0 -> 12
      hour > 12 -> hour - 12
      else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
  }

  fun addDaysIso(iso: String, days: Int): String {
    val cal = parseIsoDate(iso) ?: Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, days)
    return isoDateFormat.format(cal.time)
  }

  fun addMonthsIso(iso: String, months: Int): String {
    val cal = parseIsoDate(iso) ?: Calendar.getInstance()
    cal.add(Calendar.MONTH, months)
    return isoDateFormat.format(cal.time)
  }

  fun weekRangeContaining(iso: String): Pair<String, String> {
    val cal = parseIsoDate(iso) ?: Calendar.getInstance()
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val diffToMonday = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
    cal.add(Calendar.DAY_OF_YEAR, diffToMonday)
    val start = isoDateFormat.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, 6)
    val end = isoDateFormat.format(cal.time)
    return start to end
  }

  fun monthPrefix(iso: String): String = if (iso.length >= 7) iso.substring(0, 7) else todayIso().substring(0, 7)

  fun combineDateTimeMillis(dateIso: String, time24: String): Long? {
    val cal = parseIsoDate(dateIso) ?: return null
    val parts = time24.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    cal.set(Calendar.HOUR_OF_DAY, h)
    cal.set(Calendar.MINUTE, m)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  fun formatInr(amount: Double, symbol: String = "₹"): String {
    val negative = amount < 0
    val absVal = abs(amount)
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalPart = DecimalFormat("0.00", symbols).format(absVal)
    val dotIdx = decimalPart.indexOf('.')
    val intPart = decimalPart.substring(0, dotIdx)
    val fracPart = decimalPart.substring(dotIdx)

    val formattedInt = formatIndianGrouping(intPart)
    val prefix = if (negative) "-$symbol" else symbol
    return "$prefix$formattedInt$fracPart"
  }

  private fun formatIndianGrouping(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val remaining = digits.dropLast(3)
    val sb = StringBuilder()
    var i = remaining.length
    while (i > 0) {
      val start = maxOf(0, i - 2)
      if (sb.isNotEmpty()) sb.insert(0, ',')
      sb.insert(0, remaining.substring(start, i))
      i = start
    }
    sb.append(',').append(lastThree)
    return sb.toString()
  }

  fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
