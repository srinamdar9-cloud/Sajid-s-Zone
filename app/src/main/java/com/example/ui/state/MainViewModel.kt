package com.example.ui.state

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExpenseItem
import com.example.data.JournalEntry
import com.example.data.LocalPreferencesStore
import com.example.data.ProductivityRepository
import com.example.data.TaskItem
import com.example.data.TaskRecurrence
import com.example.data.TaskStatus
import com.example.data.UserPreferences
import com.example.data.unwrapFirestoreError
import com.example.export.ReportExporter
import com.example.notifications.TaskNotificationScheduler
import com.example.util.FormatUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class NotificationFeedItem(
  val task: TaskItem,
  val kind: NotificationKind,
  val detail: String,
)

enum class NotificationKind {
  OVERDUE,
  DUE_TODAY,
  UPCOMING,
}

class MainViewModel(
  private val repository: ProductivityRepository,
  private val localStore: LocalPreferencesStore,
  private val appContext: Context,
  val currentUserId: String,
) : ViewModel() {

  val tasksState: StateFlow<UiState<List<TaskItem>>> =
    repository
      .observeTasks(currentUserId)
      .map<List<TaskItem>, UiState<List<TaskItem>>> { tasks ->
        UiState.Success(
          tasks.sortedWith(
            compareBy<TaskItem> { it.isCompleted }
              .thenBy { it.date }
              .thenBy { it.startTime.ifBlank { "99:99" } }
          )
        )
      }
      .catch { error ->
        val unwrapped = unwrapFirestoreError(error)
        if (unwrapped is CancellationException && error is CancellationException) throw error
        Log.w(TAG, "Error observing tasks", unwrapped)
        emit(UiState.Error(unwrapped.message ?: "Failed to load tasks"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val journalState: StateFlow<UiState<List<JournalEntry>>> =
    repository
      .observeJournalEntries(currentUserId)
      .map<List<JournalEntry>, UiState<List<JournalEntry>>> { entries ->
        UiState.Success(
          entries.sortedWith(
            compareByDescending<JournalEntry> { it.date }.thenByDescending { it.time }
          )
        )
      }
      .catch { error ->
        val unwrapped = unwrapFirestoreError(error)
        if (unwrapped is CancellationException && error is CancellationException) throw error
        Log.w(TAG, "Error observing journal entries", unwrapped)
        emit(UiState.Error(unwrapped.message ?: "Failed to load journal entries"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val expensesState: StateFlow<UiState<List<ExpenseItem>>> =
    repository
      .observeExpenses(currentUserId)
      .map<List<ExpenseItem>, UiState<List<ExpenseItem>>> { expenses ->
        UiState.Success(
          expenses.sortedWith(
            compareByDescending<ExpenseItem> { it.date }.thenByDescending { it.time }
          )
        )
      }
      .catch { error ->
        val unwrapped = unwrapFirestoreError(error)
        if (unwrapped is CancellationException && error is CancellationException) throw error
        Log.w(TAG, "Error observing expenses", unwrapped)
        emit(UiState.Error(unwrapped.message ?: "Failed to load expenses"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val preferencesState: StateFlow<UserPreferences> =
    combine(
      localStore.preferencesFlow,
      repository.observePreferences(currentUserId).catch { emit(UserPreferences(userId = currentUserId)) },
    ) { local, remote ->
      if (remote.createdAt != null) remote else local
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UserPreferences(userId = currentUserId),
    )

  private val _isJournalUnlocked = MutableStateFlow(false)
  val isJournalUnlocked: StateFlow<Boolean> = _isJournalUnlocked.asStateFlow()

  private val _isAppUnlocked = MutableStateFlow(false)
  val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  init {
    viewModelScope.launch {
      combine(tasksState, preferencesState) { tState, prefs ->
        val tasks = (tState as? UiState.Success)?.data ?: emptyList()
        tasks to prefs
      }.collect { (tasks, prefs) ->
        localStore.save(prefs)
        if (tasksState.value is UiState.Success) {
          TaskNotificationScheduler.rescheduleAll(appContext, tasks, prefs)
        }
      }
    }
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun showFeedback(msg: String) {
    _feedbackMessage.value = msg
  }

  // --- PIN LOCK ---

  fun verifyJournalPin(pin: String): Boolean {
    val prefs = preferencesState.value
    if (!prefs.pinEnabled || prefs.pinHash.isBlank()) {
      _isJournalUnlocked.value = true
      return true
    }
    val ok = FormatUtils.sha256(pin) == prefs.pinHash
    if (ok) _isJournalUnlocked.value = true
    return ok
  }

  fun lockJournal() {
    _isJournalUnlocked.value = false
  }

  fun verifyAppPin(pin: String): Boolean {
    val prefs = preferencesState.value
    if (!prefs.pinEnabled || prefs.lockJournalOnly || prefs.pinHash.isBlank()) {
      _isAppUnlocked.value = true
      return true
    }
    val ok = FormatUtils.sha256(pin) == prefs.pinHash
    if (ok) {
      _isAppUnlocked.value = true
      _isJournalUnlocked.value = true
    }
    return ok
  }

  // --- TASKS ---

  fun saveTask(task: TaskItem, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      val result =
        if (task.id.isBlank()) {
          repository.createTask(task)
        } else {
          repository.updateTask(task).map { task.id }
        }
      result
        .onSuccess {
          _feedbackMessage.value = if (task.id.isBlank()) "Task added" else "Task updated"
          onSaved()
        }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to save task" }
    }
  }

  fun toggleTaskStatus(task: TaskItem) {
    viewModelScope.launch {
      val markingDone = !task.isCompleted
      val updated =
        if (markingDone) {
          task.copy(
            status = TaskStatus.Completed.label,
            completedAtEpochMs = System.currentTimeMillis(),
          )
        } else {
          task.copy(
            status = TaskStatus.Pending.label,
            completedAtEpochMs = 0L,
          )
        }
      repository
        .updateTask(updated)
        .onSuccess {
          if (markingDone) {
            TaskNotificationScheduler.cancelForTask(appContext, task.id)
            val rec = TaskRecurrence.from(task.recurrence)
            if (rec != TaskRecurrence.None) {
              val nextDate =
                when (rec) {
                  TaskRecurrence.Daily -> FormatUtils.addDaysIso(task.date, 1)
                  TaskRecurrence.Weekly -> FormatUtils.addDaysIso(task.date, 7)
                  TaskRecurrence.Monthly -> FormatUtils.addMonthsIso(task.date, 1)
                  TaskRecurrence.None -> task.date
                }
              val nextRemDate =
                if (task.reminderDate.isNotBlank()) {
                  when (rec) {
                    TaskRecurrence.Daily -> FormatUtils.addDaysIso(task.reminderDate, 1)
                    TaskRecurrence.Weekly -> FormatUtils.addDaysIso(task.reminderDate, 7)
                    TaskRecurrence.Monthly -> FormatUtils.addMonthsIso(task.reminderDate, 1)
                    TaskRecurrence.None -> task.reminderDate
                  }
                } else {
                  ""
                }
              val nextTask =
                task.copy(
                  id = "",
                  date = nextDate,
                  reminderDate = nextRemDate,
                  status = TaskStatus.Pending.label,
                  completedAtEpochMs = 0L,
                  createdAt = null,
                  updatedAt = null,
                )
              repository.createTask(nextTask)
              _feedbackMessage.value = "Completed & next ${rec.label.lowercase()} occurrence scheduled"
            } else {
              _feedbackMessage.value = "Task marked completed"
            }
          } else {
            _feedbackMessage.value = "Task moved back to pending"
          }
        }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to update task" }
    }
  }

  fun rescheduleTask(
    task: TaskItem,
    newDate: String,
    newStartTime: String,
    newDeadlineTime: String,
    newReminderDate: String,
    newReminderTime: String,
  ) {
    saveTask(
      task.copy(
        date = newDate,
        startTime = newStartTime,
        deadlineTime = newDeadlineTime,
        reminderDate = newReminderDate,
        reminderTime = newReminderTime,
        status = TaskStatus.Pending.label,
        completedAtEpochMs = 0L,
      )
    )
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      TaskNotificationScheduler.cancelForTask(appContext, taskId)
      repository
        .deleteTask(taskId)
        .onSuccess { _feedbackMessage.value = "Task deleted" }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to delete task" }
    }
  }

  // --- JOURNAL ---

  suspend fun saveJournalEntrySync(entry: JournalEntry): Result<String> {
    return repository.upsertJournalEntry(entry)
  }

  fun deleteJournalEntry(entryId: String) {
    viewModelScope.launch {
      repository
        .deleteJournalEntry(entryId)
        .onSuccess { _feedbackMessage.value = "Journal entry deleted" }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to delete entry" }
    }
  }

  // --- EXPENSES ---

  fun saveExpense(expense: ExpenseItem, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      val result =
        if (expense.id.isBlank()) {
          repository.createExpense(expense)
        } else {
          repository.updateExpense(expense).map { expense.id }
        }
      result
        .onSuccess {
          _feedbackMessage.value = if (expense.id.isBlank()) "Expense recorded" else "Expense updated"
          onSaved()
        }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to save expense" }
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      repository
        .deleteExpense(expenseId)
        .onSuccess { _feedbackMessage.value = "Expense deleted" }
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to delete expense" }
    }
  }

  // --- PREFERENCES & BACKUP ---

  fun updatePreferences(updated: UserPreferences) {
    viewModelScope.launch {
      localStore.save(updated)
      repository
        .savePreferences(updated.copy(userId = currentUserId))
        .onFailure { e -> _feedbackMessage.value = e.message ?: "Failed to sync settings" }
    }
  }

  fun enablePin(pin: String, lockJournalOnly: Boolean) {
    val hash = FormatUtils.sha256(pin)
    val current = preferencesState.value
    _isJournalUnlocked.value = true
    _isAppUnlocked.value = true
    updatePreferences(
      current.copy(
        pinEnabled = true,
        pinHash = hash,
        lockJournalOnly = lockJournalOnly,
      )
    )
    _feedbackMessage.value = "PIN lock enabled"
  }

  fun disablePin() {
    val current = preferencesState.value
    _isJournalUnlocked.value = true
    _isAppUnlocked.value = true
    updatePreferences(current.copy(pinEnabled = false, pinHash = ""))
    _feedbackMessage.value = "PIN lock removed"
  }

  fun restoreFromJson(jsonText: String) {
    viewModelScope.launch {
      try {
        val payload = ReportExporter.parseBackupJson(jsonText)
        var restoredCount = 0
        payload.tasks.forEach { t ->
          if (repository.createTask(t).isSuccess) restoredCount++
        }
        payload.journal.forEach { j ->
          if (repository.upsertJournalEntry(j).isSuccess) restoredCount++
        }
        payload.expenses.forEach { e ->
          if (repository.createExpense(e).isSuccess) restoredCount++
        }
        payload.preferences?.let { p ->
          val current = preferencesState.value
          updatePreferences(
            current.copy(
              themeMode = p.themeMode,
              use24HourTime = p.use24HourTime,
              notificationsEnabled = p.notificationsEnabled,
              headsUpMinutes = p.headsUpMinutes,
              overdueNudgeEnabled = p.overdueNudgeEnabled,
              lockJournalOnly = p.lockJournalOnly,
            )
          )
        }
        _feedbackMessage.value = "Restored $restoredCount records from backup"
      } catch (e: Exception) {
        _feedbackMessage.value = "Invalid backup file: ${e.message}"
      }
    }
  }

  fun buildNotificationFeed(tasks: List<TaskItem>, use24Hour: Boolean): List<NotificationFeedItem> {
    val today = FormatUtils.todayIso()
    val nowTime = FormatUtils.nowTime24()
    val items = mutableListOf<NotificationFeedItem>()

    for (task in tasks) {
      if (task.isCompleted) continue
      val effectiveTime = task.deadlineTime.ifBlank { task.startTime }
      val isOverdueDate = task.date < today
      val isOverdueTimeToday =
        task.date == today && effectiveTime.isNotBlank() && effectiveTime < nowTime

      when {
        isOverdueDate || isOverdueTimeToday -> {
          val dueStr =
            if (effectiveTime.isNotBlank()) {
              "${FormatUtils.formatDisplayDate(task.date)} at ${FormatUtils.formatTime(effectiveTime, use24Hour)}"
            } else {
              FormatUtils.formatDisplayDate(task.date)
            }
          items.add(NotificationFeedItem(task, NotificationKind.OVERDUE, "Overdue • $dueStr"))
        }
        task.date == today -> {
          val timeStr =
            when {
              task.reminderTime.isNotBlank() ->
                "Reminder at ${FormatUtils.formatTime(task.reminderTime, use24Hour)}"
              effectiveTime.isNotBlank() ->
                "Due at ${FormatUtils.formatTime(effectiveTime, use24Hour)}"
              else -> "Due today"
            }
          items.add(NotificationFeedItem(task, NotificationKind.DUE_TODAY, timeStr))
        }
        task.reminderTime.isNotBlank() || task.date > today -> {
          val remDate = task.reminderDate.ifBlank { task.date }
          val remTime = task.reminderTime.ifBlank { effectiveTime }
          val label =
            if (remTime.isNotBlank()) {
              "${FormatUtils.formatDisplayDate(remDate)} • ${FormatUtils.formatTime(remTime, use24Hour)}"
            } else {
              FormatUtils.formatDisplayDate(remDate)
            }
          items.add(NotificationFeedItem(task, NotificationKind.UPCOMING, "Upcoming • $label"))
        }
      }
    }
    return items.sortedWith(compareBy<NotificationFeedItem> { it.kind.ordinal }.thenBy { it.task.date })
  }

  private companion object {
    const val TAG = "MainViewModel"
  }
}
