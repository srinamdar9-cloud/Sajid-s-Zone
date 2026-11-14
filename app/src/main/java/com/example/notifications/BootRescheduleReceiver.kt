package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.FirestoreProvider
import com.example.data.ProductivityRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class BootRescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (
      action != Intent.ACTION_BOOT_COMPLETED &&
        action != Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      return
    }

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
          val repo = ProductivityRepository(FirestoreProvider.getInstance(context))
          val tasks = withTimeoutOrNull(8_000L) { repo.observeTasks(uid).first() } ?: emptyList()
          val prefs = withTimeoutOrNull(5_000L) { repo.observePreferences(uid).first() }
          if (prefs != null) {
            TaskNotificationScheduler.rescheduleAll(context, tasks, prefs)
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
