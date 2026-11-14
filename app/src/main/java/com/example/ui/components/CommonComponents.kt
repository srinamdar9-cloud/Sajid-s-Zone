package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral
import com.example.ui.theme.StatusEmerald
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerModal(
  initialDateIso: String,
  onDismiss: () -> Unit,
  onDateSelected: (String) -> Unit,
) {
  val initialUtcMillis =
    remember(initialDateIso) {
      val cal = FormatUtils.parseIsoDate(initialDateIso) ?: Calendar.getInstance()
      val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      utc.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
      utc.set(Calendar.MILLISECOND, 0)
      utc.timeInMillis
    }
  val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val millis = state.selectedDateMillis
          if (millis != null) {
            val fmt =
              SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
              }
            onDateSelected(fmt.format(millis))
          }
          onDismiss()
        }
      ) {
        Text("Select")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  ) {
    DatePicker(state = state)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialTimePickerModal(
  initialTime24: String,
  use24Hour: Boolean,
  onDismiss: () -> Unit,
  onTimeSelected: (String) -> Unit,
) {
  val parts = initialTime24.split(":")
  val initHour = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  val initMin = parts.getOrNull(1)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MINUTE)
  val state =
    rememberTimePickerState(
      initialHour = initHour.coerceIn(0, 23),
      initialMinute = initMin.coerceIn(0, 59),
      is24Hour = use24Hour,
    )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Select Time", style = MaterialTheme.typography.titleLarge) },
    text = {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TimePicker(state = state)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onTimeSelected(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
          onDismiss()
        }
      ) {
        Text("Set Time")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun PriorityBadge(priority: String) {
  val (bg, fg) =
    when (priority) {
      "High" -> StatusCoral.copy(alpha = 0.14f) to StatusCoral
      "Medium" -> StatusAmber.copy(alpha = 0.16f) to StatusAmber
      else -> StatusEmerald.copy(alpha = 0.14f) to StatusEmerald
    }
  Surface(
    color = bg,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = priority,
      style = MaterialTheme.typography.labelSmall,
      color = fg,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun CategoryBadge(label: String, containerColor: Color = MaterialTheme.colorScheme.secondaryContainer) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  subtitle: String,
  icon: ImageVector = Icons.Outlined.Inbox,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun PinLockGate(
  title: String,
  subtitle: String,
  onVerifyPin: (String) -> Boolean,
) {
  var pin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier.fillMaxWidth().padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Protected section",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(14.dp).size(28.dp),
          )
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        OutlinedTextField(
          value = pin,
          onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() }.take(6)
            pin = filtered
            error = null
            if (filtered.length == 4 && onVerifyPin(filtered)) {
              pin = ""
            }
          },
          label = { Text("Enter PIN") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          isError = error != null,
          supportingText = error?.let { { Text(it) } },
          textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
          modifier = Modifier.fillMaxWidth().testTag("pin_unlock_input"),
        )

        Button(
          onClick = {
            if (pin.length < 4) {
              error = "Enter your 4–6 digit PIN"
            } else if (!onVerifyPin(pin)) {
              error = "Incorrect PIN. Try again."
              pin = ""
            }
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pin_unlock_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("Unlock")
        }
      }
    }
  }
}

@Composable
fun SectionHeaderRow(
  title: String,
  trailingText: String? = null,
  onTrailingClick: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (trailingText != null && onTrailingClick != null) {
      TextButton(onClick = onTrailingClick) {
        Text(trailingText, style = MaterialTheme.typography.labelLarge)
      }
    } else if (trailingText != null) {
      Text(
        text = trailingText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
