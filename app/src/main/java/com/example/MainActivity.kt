package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notifications.TaskNotificationScheduler
import com.example.ui.SajidsZoneRoot

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    TaskNotificationScheduler.ensureNotificationChannel(this)
    setContent {
      SajidsZoneRoot()
    }
  }
}
