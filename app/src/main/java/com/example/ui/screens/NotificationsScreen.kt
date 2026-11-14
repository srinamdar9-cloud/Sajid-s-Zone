package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TaskItem
import com.example.data.UserPreferences
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityBadge
import com.example.ui.state.NotificationFeedItem
import com.example.ui.state.NotificationKind
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCoral

@Composable
fun NotificationsScreen(
  feed: List<NotificationFeedItem>,
  preferences: UserPreferences,
  onMarkDone: (TaskItem) -> Unit,
  onReschedule: (TaskItem, String, String, String, String, String) -> Unit,
  onGoToTasks: () -> Unit,
) {
  var rescheduleTarget by remember { mutableStateOf<TaskItem?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("notifications_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Notification Centre", style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "${feed.size} active due, overdue & upcoming reminders",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(onClick = onGoToTasks) {
          Text("All Tasks")
        }
      }
    }

    if (feed.isEmpty()) {
      item {
        EmptyStateCard(
          title = "You're all caught up",
          subtitle = "No overdue, due today, or upcoming task reminders right now.",
          icon = Icons.Filled.NotificationsNone,
          actionLabel = "Open Tasks",
          onAction = onGoToTasks,
        )
      }
    } else {
      items(feed, key = { it.task.id }) { item ->
        val accent =
          when (item.kind) {
            NotificationKind.OVERDUE -> StatusCoral
            NotificationKind.DUE_TODAY -> StatusAmber
            NotificationKind.UPCOMING -> MaterialTheme.colorScheme.primary
          }
        val icon =
          when (item.kind) {
            NotificationKind.OVERDUE -> Icons.Filled.Warning
            NotificationKind.DUE_TODAY -> Icons.Filled.Alarm
            NotificationKind.UPCOMING -> Icons.Filled.Schedule
          }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.padding(10.dp).size(20.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.task.title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = item.detail,
                  style = MaterialTheme.typography.bodySmall,
                  color = accent,
                )
              }
              PriorityBadge(item.task.priority)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = { onMarkDone(item.task) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark Done")
              }
              OutlinedButton(
                onClick = { rescheduleTarget = item.task },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reschedule")
              }
            }
          }
        }
      }
    }
  }

  rescheduleTarget?.let { target ->
    RescheduleTaskDialog(
      task = target,
      use24Hour = preferences.use24HourTime,
      onDismiss = { rescheduleTarget = null },
      onConfirm = { d, s, dl, rd, rt ->
        onReschedule(target, d, s, dl, rd, rt)
        rescheduleTarget = null
      },
    )
  }
}
