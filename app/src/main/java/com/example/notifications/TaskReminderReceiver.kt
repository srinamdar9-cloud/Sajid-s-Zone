package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class TaskReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    TaskNotificationScheduler.ensureNotificationChannel(context)

    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val title = intent.getStringExtra(TaskNotificationScheduler.EXTRA_TITLE) ?: "Sajid's Zone Reminder"
    val body = intent.getStringExtra(TaskNotificationScheduler.EXTRA_BODY) ?: "You have a task waiting."
    val notifId =
      intent.getIntExtra(
        TaskNotificationScheduler.EXTRA_NOTIF_ID,
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
      )

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val contentPendingIntent =
      PendingIntent.getActivity(
        context,
        notifId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, TaskNotificationScheduler.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(notifId, notification)
  }
}
