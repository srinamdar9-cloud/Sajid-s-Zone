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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.JournalEntry
import com.example.data.UserPreferences
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MaterialDatePickerModal
import com.example.ui.components.PinLockGate
import com.example.ui.state.UiState
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JournalScreen(
  journalState: UiState<List<JournalEntry>>,
  preferences: UserPreferences,
  isUnlocked: Boolean,
  openNewJournalTrigger: Int,
  onVerifyPin: (String) -> Boolean,
  onLockJournal: () -> Unit,
  onSaveDraft: suspend (JournalEntry) -> Result<String>,
  onDeleteEntry: (String) -> Unit,
) {
  if (preferences.pinEnabled && !isUnlocked) {
    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      PinLockGate(
        title = "Journal Locked",
        subtitle = "Enter your PIN to view and write private journal entries & daily reflections.",
        onVerifyPin = onVerifyPin,
      )
    }
    return
  }

  var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
  var isEditing by remember(openNewJournalTrigger) { mutableStateOf(openNewJournalTrigger > 0) }
  var searchQuery by remember { mutableStateOf("") }
  var filterDate by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  LaunchedEffect(openNewJournalTrigger) {
    if (openNewJournalTrigger > 0) {
      editingEntry =
        JournalEntry(
          date = FormatUtils.todayIso(),
          time = FormatUtils.nowTime24(),
        )
      isEditing = true
    }
  }

  if (isEditing) {
    JournalEditorView(
      initialEntry =
        editingEntry
          ?: JournalEntry(
            date = filterDate ?: FormatUtils.todayIso(),
            time = FormatUtils.nowTime24(),
          ),
      use24Hour = preferences.use24HourTime,
      onBack = { isEditing = false },
      onSaveDraft = onSaveDraft,
    )
    return
  }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingEntry =
            JournalEntry(
              date = filterDate ?: FormatUtils.todayIso(),
              time = FormatUtils.nowTime24(),
            )
          isEditing = true
        },
        modifier = Modifier.testTag("add_journal_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Write journal entry")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).testTag("journal_screen"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Private Journal", style = MaterialTheme.typography.headlineSmall)
          if (preferences.pinEnabled) {
            OutlinedButton(onClick = onLockJournal) {
              Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Lock")
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search keywords, reflections...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("journal_search_input"),
          )
          OutlinedButton(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = "Browse by date")
            if (filterDate != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(FormatUtils.formatDisplayDate(filterDate!!))
            }
          }
          if (filterDate != null) {
            IconButton(onClick = { filterDate = null }) {
              Icon(Icons.Filled.Clear, contentDescription = "Clear date filter")
            }
          }
        }
      }

      when (journalState) {
        is UiState.Loading -> {
          item { Text("Loading journal...", style = MaterialTheme.typography.bodyMedium) }
        }
        is UiState.Error -> {
          item { EmptyStateCard("Could not load journal", journalState.message) }
        }
        is UiState.Success -> {
          val filtered =
            journalState.data.filter { entry ->
              val matchesDate = filterDate == null || entry.date == filterDate
              val q = searchQuery.trim().lowercase()
              val matchesQuery =
                q.isEmpty() ||
                  entry.title.lowercase().contains(q) ||
                  entry.content.lowercase().contains(q) ||
                  entry.accomplished.lowercase().contains(q) ||
                  entry.wentWell.lowercase().contains(q) ||
                  entry.couldImprove.lowercase().contains(q) ||
                  entry.tomorrowFocus.lowercase().contains(q) ||
                  FormatUtils.formatDisplayDate(entry.date).contains(q)
              matchesDate && matchesQuery
            }

          if (filtered.isEmpty()) {
            item {
              EmptyStateCard(
                title = "No journal entries yet",
                subtitle =
                  "Capture your thoughts and answer the 4 daily reflection prompts. Drafts auto-save as you type.",
                icon = Icons.Filled.Book,
                actionLabel = "Write Today's Entry",
                onAction = {
                  editingEntry =
                    JournalEntry(
                      date = filterDate ?: FormatUtils.todayIso(),
                      time = FormatUtils.nowTime24(),
                    )
                  isEditing = true
                },
              )
            }
          } else {
            items(filtered, key = { it.id }) { entry ->
              Card(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      editingEntry = entry
                      isEditing = true
                    }
                    .testTag("journal_item_${entry.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        "${FormatUtils.formatDisplayDate(entry.date)} • ${FormatUtils.formatTime(entry.time, preferences.use24HourTime)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(onClick = { onDeleteEntry(entry.id) }) {
                      Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                  Text(
                    text = entry.title.ifBlank { "Untitled Entry" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val preview = entry.content.ifBlank { entry.accomplished }
                  if (preview.isNotBlank()) {
                    Text(
                      text = preview,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(72.dp)) }
    }
  }

  if (showDatePicker) {
    MaterialDatePickerModal(
      initialDateIso = filterDate ?: FormatUtils.todayIso(),
      onDismiss = { showDatePicker = false },
      onDateSelected = { filterDate = it },
    )
  }
}

@Composable
private fun JournalEditorView(
  initialEntry: JournalEntry,
  use24Hour: Boolean,
  onBack: () -> Unit,
  onSaveDraft: suspend (JournalEntry) -> Result<String>,
) {
  val scope = rememberCoroutineScope()
  var currentId by remember { mutableStateOf(initialEntry.id) }
  var date by remember { mutableStateOf(initialEntry.date.ifBlank { FormatUtils.todayIso() }) }
  var time by remember { mutableStateOf(initialEntry.time.ifBlank { FormatUtils.nowTime24() }) }
  var title by remember { mutableStateOf(initialEntry.title) }
  var content by remember { mutableStateOf(initialEntry.content) }
  var accomplished by remember { mutableStateOf(initialEntry.accomplished) }
  var wentWell by remember { mutableStateOf(initialEntry.wentWell) }
  var couldImprove by remember { mutableStateOf(initialEntry.couldImprove) }
  var tomorrowFocus by remember { mutableStateOf(initialEntry.tomorrowFocus) }
  var draftStatus by remember { mutableStateOf("Ready") }
  var hasEdited by remember { mutableStateOf(false) }

  // Debounced auto-save while typing
  LaunchedEffect(title, content, accomplished, wentWell, couldImprove, tomorrowFocus, date) {
    if (!hasEdited) return@LaunchedEffect
    draftStatus = "Saving draft..."
    delay(900L)
    val payload =
      initialEntry.copy(
        id = currentId,
        date = date,
        time = time,
        title = title,
        content = content,
        accomplished = accomplished,
        wentWell = wentWell,
        couldImprove = couldImprove,
        tomorrowFocus = tomorrowFocus,
      )
    val res = onSaveDraft(payload)
    res
      .onSuccess { savedId ->
        currentId = savedId
        draftStatus = "Draft auto-saved"
      }
      .onFailure { draftStatus = "Offline draft pending" }
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to journal")
        }
        Column {
          Text(
            text = FormatUtils.formatDisplayDate(date),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "${FormatUtils.formatTime(time, use24Hour)} • $draftStatus",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Button(
        onClick = {
          scope.launch {
            draftStatus = "Saving..."
            val payload =
              initialEntry.copy(
                id = currentId,
                date = date,
                time = time,
                title = title,
                content = content,
                accomplished = accomplished,
                wentWell = wentWell,
                couldImprove = couldImprove,
                tomorrowFocus = tomorrowFocus,
              )
            onSaveDraft(payload).onSuccess {
              currentId = it
              draftStatus = "Saved"
              onBack()
            }
          }
        },
        modifier = Modifier.testTag("save_journal_button"),
      ) {
        Text("Done")
      }
    }

    OutlinedTextField(
      value = title,
      onValueChange = {
        title = it.take(200)
        hasEdited = true
      },
      label = { Text("Entry Title") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("journal_title_input"),
    )

    OutlinedTextField(
      value = content,
      onValueChange = {
        content = it.take(20000)
        hasEdited = true
      },
      label = { Text("Journal Notes") },
      minLines = 5,
      modifier = Modifier.fillMaxWidth().testTag("journal_content_input"),
    )

    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Daily Reflection", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
          value = accomplished,
          onValueChange = {
            accomplished = it.take(3000)
            hasEdited = true
          },
          label = { Text("What did I accomplish today?") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = wentWell,
          onValueChange = {
            wentWell = it.take(3000)
            hasEdited = true
          },
          label = { Text("What went well today?") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = couldImprove,
          onValueChange = {
            couldImprove = it.take(3000)
            hasEdited = true
          },
          label = { Text("What could I improve?") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = tomorrowFocus,
          onValueChange = {
            tomorrowFocus = it.take(3000)
            hasEdited = true
          },
          label = { Text("What should I focus on tomorrow?") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
