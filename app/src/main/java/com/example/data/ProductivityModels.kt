package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskPriority(val label: String) {
  High("High"),
  Medium("Medium"),
  Low("Low");

  companion object {
    fun from(value: String): TaskPriority =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: Medium
  }
}

enum class TaskCategory(val label: String) {
  Personal("Personal"),
  Office("Office"),
  Health("Health"),
  Family("Family"),
  Financial("Financial"),
  Shopping("Shopping"),
  Other("Other");

  companion object {
    fun from(value: String): TaskCategory =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: Personal
  }
}

enum class TaskStatus(val label: String) {
  Pending("Pending"),
  Completed("Completed");

  companion object {
    fun from(value: String): TaskStatus =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: Pending
  }
}

enum class TaskRecurrence(val label: String) {
  None("None"),
  Daily("Daily"),
  Weekly("Weekly"),
  Monthly("Monthly");

  companion object {
    fun from(value: String): TaskRecurrence =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: None
  }
}

enum class ExpenseCategory(val label: String) {
  Grocery("Grocery"),
  PetrolAndTransportation("Petrol and Transportation"),
  Electricity("Electricity"),
  MobileAndInternet("Mobile and Internet"),
  MedicalAndHealthcare("Medical and Healthcare"),
  HouseholdExpenses("Household Expenses"),
  FoodAndRestaurants("Food and Restaurants"),
  Shopping("Shopping"),
  Travel("Travel"),
  FamilyExpenses("Family Expenses"),
  PersonalExpenses("Personal Expenses"),
  Other("Other");

  companion object {
    fun from(value: String): ExpenseCategory =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: Other
  }
}

enum class PaymentMethod(val label: String) {
  Cash("Cash"),
  UPI("UPI"),
  DebitCard("Debit Card"),
  CreditCard("Credit Card"),
  BankTransfer("Bank Transfer");

  companion object {
    fun from(value: String): PaymentMethod =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: UPI
  }
}

enum class ThemeMode(val label: String) {
  System("System"),
  Light("Light"),
  Dark("Dark");

  companion object {
    fun from(value: String): ThemeMode =
      entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: System
  }
}

@IgnoreExtraProperties
data class TaskItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String = "",
  val date: String = "",
  val startTime: String = "",
  val deadlineTime: String = "",
  val reminderDate: String = "",
  val reminderTime: String = "",
  val priority: String = TaskPriority.Medium.label,
  val category: String = TaskCategory.Personal.label,
  val status: String = TaskStatus.Pending.label,
  val recurrence: String = TaskRecurrence.None.label,
  val completedAtEpochMs: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isCompleted: Boolean
    get() = status == TaskStatus.Completed.label

  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "title" to title.trim().take(160),
      "description" to description.trim().take(2000),
      "date" to date,
      "startTime" to startTime,
      "deadlineTime" to deadlineTime,
      "reminderDate" to reminderDate,
      "reminderTime" to reminderTime,
      "priority" to TaskPriority.from(priority).label,
      "category" to TaskCategory.from(category).label,
      "status" to TaskStatus.from(status).label,
      "recurrence" to TaskRecurrence.from(recurrence).label,
      "completedAtEpochMs" to completedAtEpochMs,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "title" to title.trim().take(160),
      "description" to description.trim().take(2000),
      "date" to date,
      "startTime" to startTime,
      "deadlineTime" to deadlineTime,
      "reminderDate" to reminderDate,
      "reminderTime" to reminderTime,
      "priority" to TaskPriority.from(priority).label,
      "category" to TaskCategory.from(category).label,
      "status" to TaskStatus.from(status).label,
      "recurrence" to TaskRecurrence.from(recurrence).label,
      "completedAtEpochMs" to completedAtEpochMs,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class JournalEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val date: String = "",
  val time: String = "",
  val title: String = "",
  val content: String = "",
  val accomplished: String = "",
  val wentWell: String = "",
  val couldImprove: String = "",
  val tomorrowFocus: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "date" to date,
      "time" to time,
      "title" to title.take(200),
      "content" to content.take(20000),
      "accomplished" to accomplished.take(3000),
      "wentWell" to wentWell.take(3000),
      "couldImprove" to couldImprove.take(3000),
      "tomorrowFocus" to tomorrowFocus.take(3000),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "date" to date,
      "time" to time,
      "title" to title.take(200),
      "content" to content.take(20000),
      "accomplished" to accomplished.take(3000),
      "wentWell" to wentWell.take(3000),
      "couldImprove" to couldImprove.take(3000),
      "tomorrowFocus" to tomorrowFocus.take(3000),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class ExpenseItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val amount: Double = 0.0,
  val description: String = "",
  val category: String = ExpenseCategory.Other.label,
  val date: String = "",
  val time: String = "",
  val paymentMethod: String = PaymentMethod.UPI.label,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "amount" to amount,
      "description" to description.trim().take(200),
      "category" to ExpenseCategory.from(category).label,
      "date" to date,
      "time" to time,
      "paymentMethod" to PaymentMethod.from(paymentMethod).label,
      "notes" to notes.trim().take(1000),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "amount" to amount,
      "description" to description.trim().take(200),
      "category" to ExpenseCategory.from(category).label,
      "date" to date,
      "time" to time,
      "paymentMethod" to PaymentMethod.from(paymentMethod).label,
      "notes" to notes.trim().take(1000),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class UserPreferences(
  @DocumentId val id: String = "preferences",
  val userId: String = "",
  val themeMode: String = ThemeMode.System.label,
  val use24HourTime: Boolean = false,
  val notificationsEnabled: Boolean = true,
  val headsUpMinutes: Long = 15L,
  val overdueNudgeEnabled: Boolean = true,
  val pinEnabled: Boolean = false,
  val pinHash: String = "",
  val lockJournalOnly: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "themeMode" to ThemeMode.from(themeMode).label,
      "use24HourTime" to use24HourTime,
      "notificationsEnabled" to notificationsEnabled,
      "headsUpMinutes" to headsUpMinutes.coerceIn(0L, 120L),
      "overdueNudgeEnabled" to overdueNudgeEnabled,
      "pinEnabled" to pinEnabled,
      "pinHash" to pinHash.take(128),
      "lockJournalOnly" to lockJournalOnly,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "themeMode" to ThemeMode.from(themeMode).label,
      "use24HourTime" to use24HourTime,
      "notificationsEnabled" to notificationsEnabled,
      "headsUpMinutes" to headsUpMinutes.coerceIn(0L, 120L),
      "overdueNudgeEnabled" to overdueNudgeEnabled,
      "pinEnabled" to pinEnabled,
      "pinHash" to pinHash.take(128),
      "lockJournalOnly" to lockJournalOnly,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}
