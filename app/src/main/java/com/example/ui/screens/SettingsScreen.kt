package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseItem
import com.example.data.JournalEntry
import com.example.data.TaskItem
import com.example.data.ThemeMode
import com.example.data.UserPreferences
import com.example.export.ReportExporter
import com.example.ui.state.UiState
import java.io.File

@Composable
fun SettingsScreen(
  userEmail: String?,
  preferences: UserPreferences,
  tasksState: UiState<List<TaskItem>>,
  journalState: UiState<List<JournalEntry>>,
  expensesState: UiState<List<ExpenseItem>>,
  onUpdatePreferences: (UserPreferences) -> Unit,
  onEnablePin: (String, Boolean) -> Unit,
  onDisablePin: () -> Unit,
  onRestoreJson: (String) -> Unit,
  onSignOut: () -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  var showPinSetupDialog by remember { mutableStateOf(false) }

  val restoreLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        try {
          val text =
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
          if (!text.isNullOrBlank()) {
            onRestoreJson(text)
          }
        } catch (e: Exception) {
          onFeedback("Could not read backup file: ${e.message}")
        }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("settings_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Settings", style = MaterialTheme.typography.headlineSmall)
    }

    // Account Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
              Text("Signed-in Account", style = MaterialTheme.typography.titleSmall)
              Text(
                text = userEmail ?: "Google Account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().testTag("sign_out_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
          }
        }
      }
    }

    // Appearance & Formats
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Appearance & Regional Formats", style = MaterialTheme.typography.titleMedium)
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { idx, mode ->
              SegmentedButton(
                selected = preferences.themeMode == mode.label,
                onClick = { onUpdatePreferences(preferences.copy(themeMode = mode.label)) },
                shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
              ) {
                Text(mode.label)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("24-Hour Time Format", style = MaterialTheme.typography.bodyLarge)
              Text(
                text = if (preferences.use24HourTime) "Showing 14:30" else "Showing 02:30 PM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = preferences.use24HourTime,
              onCheckedChange = { onUpdatePreferences(preferences.copy(use24HourTime = it)) },
            )
          }

          Text(
            text = "Date Format: DD/MM/YYYY  •  Currency: ₹ INR (Indian numbering)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Notification Preferences
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Notifications & Alarms", style = MaterialTheme.typography.titleMedium)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Enable Task Reminders", style = MaterialTheme.typography.bodyLarge)
            Switch(
              checked = preferences.notificationsEnabled,
              onCheckedChange = { onUpdatePreferences(preferences.copy(notificationsEnabled = it)) },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Daily Overdue Nudge (09:00 AM)", style = MaterialTheme.typography.bodyLarge)
            Switch(
              checked = preferences.overdueNudgeEnabled,
              onCheckedChange = { onUpdatePreferences(preferences.copy(overdueNudgeEnabled = it)) },
            )
          }

          Text("Heads-up before start time", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0L, 5L, 15L, 30L).forEach { mins ->
              FilterChip(
                selected = preferences.headsUpMinutes == mins,
                onClick = { onUpdatePreferences(preferences.copy(headsUpMinutes = mins)) },
                label = { Text(if (mins == 0L) "Off" else "${mins}m") },
              )
            }
          }

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            OutlinedButton(
              onClick = {
                val intent =
                  Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                  }
                context.startActivity(intent)
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text("Manage Exact Alarm Permission")
            }
          }
        }
      }
    }

    // PIN Lock
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Privacy & PIN Lock", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text =
              if (preferences.pinEnabled) {
                if (preferences.lockJournalOnly) "PIN active: protecting Private Journal"
                else "PIN active: protecting entire app & Journal"
              } else {
                "Protect your Private Journal (or entire app) with a 4–6 digit PIN."
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showPinSetupDialog = true },
              modifier = Modifier.weight(1f).testTag("configure_pin_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(if (preferences.pinEnabled) "Change PIN" else "Set Up PIN")
            }
            if (preferences.pinEnabled) {
              OutlinedButton(
                onClick = onDisablePin,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
              ) {
                Text("Disable PIN")
              }
            }
          }
        }
      }
    }

    // Backup & Restore
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Export all tasks, journal entries, expenses, and settings to JSON or restore from an existing backup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                val tasks = (tasksState as? UiState.Success)?.data ?: emptyList()
                val journal = (journalState as? UiState.Success)?.data ?: emptyList()
                val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
                val json = ReportExporter.createBackupJson(tasks, journal, expenses, preferences)
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "SajidsZone_Backup.json")
                file.writeText(json)
                ReportExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "application/json",
                  title = "Share Sajid's Zone Backup",
                )
              },
              modifier = Modifier.weight(1f).testTag("backup_export_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Backup")
            }
            OutlinedButton(
              onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
              modifier = Modifier.weight(1f).testTag("backup_restore_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Restore")
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  if (showPinSetupDialog) {
    PinSetupDialog(
      initialLockJournalOnly = preferences.lockJournalOnly,
      onDismiss = { showPinSetupDialog = false },
      onConfirm = { pin, journalOnly ->
        onEnablePin(pin, journalOnly)
        showPinSetupDialog = false
      },
    )
  }
}

@Composable
private fun PinSetupDialog(
  initialLockJournalOnly: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String, Boolean) -> Unit,
) {
  var pin1 by remember { mutableStateOf("") }
  var pin2 by remember { mutableStateOf("") }
  var journalOnly by remember { mutableStateOf(initialLockJournalOnly) }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Configure PIN Lock") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = pin1,
          onValueChange = {
            pin1 = it.filter { c -> c.isDigit() }.take(6)
            error = null
          },
          label = { Text("New PIN (4–6 digits)") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = pin2,
          onValueChange = {
            pin2 = it.filter { c -> c.isDigit() }.take(6)
            error = null
          },
          label = { Text("Confirm PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Lock Journal section only", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = journalOnly, onCheckedChange = { journalOnly = it })
        }
        if (error != null) {
          Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (pin1.length < 4) {
            error = "PIN must be at least 4 digits"
          } else if (pin1 != pin2) {
            error = "PINs do not match"
          } else {
            onConfirm(pin1, journalOnly)
          }
        }
      ) {
        Text("Save PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
