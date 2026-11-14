package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FirestoreProvider
import com.example.data.LocalPreferencesStore
import com.example.data.ProductivityRepository
import com.example.data.ThemeMode
import com.example.ui.auth.AuthHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.components.PinLockGate
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.state.MainViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.SajidsZoneTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class AppSection(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val isPrimaryTab: Boolean,
) {
  Home("Home", Icons.Filled.Home, Icons.Outlined.Home, true),
  Tasks("Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, true),
  Journal("Journal", Icons.Filled.Book, Icons.Outlined.Book, true),
  Expenses("Expenses", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, true),
  Calendar("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, true),
  Notifications("Notifications", Icons.Filled.Notifications, Icons.Filled.Notifications, false),
  Reports("Reports", Icons.Filled.BarChart, Icons.Filled.BarChart, false),
  Settings("Settings", Icons.Filled.Settings, Icons.Filled.Settings, false),
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun SajidsZoneRoot() {
  val context = LocalContext.current
  val localStore = remember(context) { LocalPreferencesStore(context.applicationContext) }
  val coldPrefs by localStore.preferencesFlow.collectAsStateWithLifecycle(initialValue = com.example.data.UserPreferences())

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val user = currentUser
  if (user == null) {
    SajidsZoneTheme(themeMode = ThemeMode.from(coldPrefs.themeMode)) {
      AuthScreen(onAuthSuccess = {})
    }
  } else {
    key(user.uid) {
      val vm: MainViewModel =
        viewModel(
          key = user.uid,
          factory =
            viewModelFactory {
              initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirestoreProvider.getInstance(app)
                val repo = ProductivityRepository(db, Firebase.auth)
                val store = LocalPreferencesStore(app)
                MainViewModel(repo, store, app.applicationContext, user.uid)
              }
            },
        )
      AuthenticatedAppShell(
        userEmail = user.email,
        viewModel = vm,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
  userEmail: String?,
  viewModel: MainViewModel,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
  val journalState by viewModel.journalState.collectAsStateWithLifecycle()
  val expensesState by viewModel.expensesState.collectAsStateWithLifecycle()
  val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
  val isJournalUnlocked by viewModel.isJournalUnlocked.collectAsStateWithLifecycle()
  val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
  val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedback) {
    if (feedback != null) {
      snackbarHostState.showSnackbar(feedback!!)
      viewModel.clearFeedback()
    }
  }

  var activeSection by remember { mutableStateOf(AppSection.Home) }
  var lastPrimaryTab by remember { mutableStateOf(AppSection.Home) }
  var newTaskTrigger by remember { mutableIntStateOf(0) }
  var newJournalTrigger by remember { mutableIntStateOf(0) }
  var newExpenseTrigger by remember { mutableIntStateOf(0) }

  val tasksList = (tasksState as? UiState.Success)?.data ?: emptyList()
  val notificationFeed =
    remember(tasksList, preferences.use24HourTime) {
      viewModel.buildNotificationFeed(tasksList, preferences.use24HourTime)
    }

  fun navigateTo(section: AppSection) {
    if (section.isPrimaryTab) {
      lastPrimaryTab = section
    }
    activeSection = section
  }

  if (!activeSection.isPrimaryTab) {
    BackHandler { activeSection = lastPrimaryTab }
  }

  SajidsZoneTheme(themeMode = ThemeMode.from(preferences.themeMode)) {
    if (preferences.pinEnabled && !preferences.lockJournalOnly && !isAppUnlocked) {
      Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
      ) {
        Box(modifier = Modifier.widthIn(max = 460.dp)) {
          PinLockGate(
            title = "Sajid's Zone Locked",
            subtitle = "Enter your PIN to access your workspace.",
            onVerifyPin = { viewModel.verifyAppPin(it) },
          )
        }
      }
      return@SajidsZoneTheme
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useSidebar = maxWidth >= 600.dp

      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
          TopAppBar(
            title = {
              Text(
                text =
                  if (activeSection.isPrimaryTab) "Sajid's Zone"
                  else activeSection.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            navigationIcon = {
              if (!activeSection.isPrimaryTab) {
                IconButton(onClick = { activeSection = lastPrimaryTab }) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
              }
            },
            actions = {
              IconButton(
                onClick = { navigateTo(AppSection.Notifications) },
                modifier = Modifier.testTag("nav_notifications_button"),
              ) {
                BadgedBox(
                  badge = {
                    if (notificationFeed.isNotEmpty()) {
                      Badge { Text(notificationFeed.size.coerceAtMost(99).toString()) }
                    }
                  }
                ) {
                  Icon(Icons.Filled.Notifications, contentDescription = "Notification centre")
                }
              }
              IconButton(
                onClick = { navigateTo(AppSection.Reports) },
                modifier = Modifier.testTag("nav_reports_button"),
              ) {
                Icon(Icons.Filled.BarChart, contentDescription = "Reports")
              }
              IconButton(
                onClick = { navigateTo(AppSection.Settings) },
                modifier = Modifier.testTag("nav_settings_button"),
              ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
              ),
          )
        },
        bottomBar = {
          if (!useSidebar) {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
              AppSection.entries
                .filter { it.isPrimaryTab }
                .forEach { tab ->
                  val selected =
                    activeSection == tab || (!activeSection.isPrimaryTab && lastPrimaryTab == tab)
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateTo(tab) },
                    icon = {
                      Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                      )
                    },
                    label = { Text(tab.label, maxLines = 1) },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                  )
                }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useSidebar) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
              ) {
                AppSection.entries
                  .filter { it.isPrimaryTab }
                  .forEach { tab ->
                    val selected = activeSection == tab
                    NavigationRailItem(
                      selected = selected,
                      onClick = { navigateTo(tab) },
                      icon = {
                        Icon(
                          imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                          contentDescription = tab.label,
                        )
                      },
                      label = { Text(tab.label) },
                      modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                    )
                  }
                NavigationRailItem(
                  selected = activeSection == AppSection.Reports,
                  onClick = { navigateTo(AppSection.Reports) },
                  icon = { Icon(Icons.Filled.BarChart, contentDescription = "Reports") },
                  label = { Text("Reports") },
                )
                NavigationRailItem(
                  selected = activeSection == AppSection.Settings,
                  onClick = { navigateTo(AppSection.Settings) },
                  icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                  label = { Text("Settings") },
                )
              }
            }
          }

          Box(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
          ) {
            Box(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
              when (activeSection) {
                AppSection.Home ->
                  HomeScreen(
                    tasksState = tasksState,
                    expensesState = expensesState,
                    preferences = preferences,
                    notificationFeed = notificationFeed,
                    onQuickAddTask = {
                      newTaskTrigger++
                      navigateTo(AppSection.Tasks)
                    },
                    onQuickWriteJournal = {
                      newJournalTrigger++
                      navigateTo(AppSection.Journal)
                    },
                    onQuickRecordExpense = {
                      newExpenseTrigger++
                      navigateTo(AppSection.Expenses)
                    },
                    onOpenTasks = { navigateTo(AppSection.Tasks) },
                    onOpenExpenses = { navigateTo(AppSection.Expenses) },
                    onOpenNotifications = { navigateTo(AppSection.Notifications) },
                    onOpenReports = { navigateTo(AppSection.Reports) },
                  )
                AppSection.Tasks ->
                  TasksScreen(
                    tasksState = tasksState,
                    preferences = preferences,
                    openNewTaskTrigger = newTaskTrigger,
                    onSaveTask = { viewModel.saveTask(it) },
                    onToggleTask = { viewModel.toggleTaskStatus(it) },
                    onRescheduleTask = { t, d, s, dl, rd, rt ->
                      viewModel.rescheduleTask(t, d, s, dl, rd, rt)
                    },
                    onDeleteTask = { viewModel.deleteTask(it) },
                  )
                AppSection.Journal ->
                  JournalScreen(
                    journalState = journalState,
                    preferences = preferences,
                    isUnlocked = isJournalUnlocked,
                    openNewJournalTrigger = newJournalTrigger,
                    onVerifyPin = { viewModel.verifyJournalPin(it) },
                    onLockJournal = { viewModel.lockJournal() },
                    onSaveDraft = { viewModel.saveJournalEntrySync(it) },
                    onDeleteEntry = { viewModel.deleteJournalEntry(it) },
                  )
                AppSection.Expenses ->
                  ExpensesScreen(
                    expensesState = expensesState,
                    preferences = preferences,
                    openNewExpenseTrigger = newExpenseTrigger,
                    onSaveExpense = { viewModel.saveExpense(it) },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onFeedback = { viewModel.showFeedback(it) },
                  )
                AppSection.Calendar ->
                  CalendarScreen(
                    tasksState = tasksState,
                    journalState = journalState,
                    expensesState = expensesState,
                    preferences = preferences,
                    isJournalUnlocked = isJournalUnlocked,
                    onToggleTask = { viewModel.toggleTaskStatus(it) },
                    onSaveTask = { viewModel.saveTask(it) },
                    onOpenJournal = { navigateTo(AppSection.Journal) },
                  )
                AppSection.Notifications ->
                  NotificationsScreen(
                    feed = notificationFeed,
                    preferences = preferences,
                    onMarkDone = { viewModel.toggleTaskStatus(it) },
                    onReschedule = { t, d, s, dl, rd, rt ->
                      viewModel.rescheduleTask(t, d, s, dl, rd, rt)
                    },
                    onGoToTasks = { navigateTo(AppSection.Tasks) },
                  )
                AppSection.Reports ->
                  ReportsScreen(
                    tasksState = tasksState,
                    expensesState = expensesState,
                  )
                AppSection.Settings ->
                  SettingsScreen(
                    userEmail = userEmail,
                    preferences = preferences,
                    tasksState = tasksState,
                    journalState = journalState,
                    expensesState = expensesState,
                    onUpdatePreferences = { viewModel.updatePreferences(it) },
                    onEnablePin = { pin, journalOnly -> viewModel.enablePin(pin, journalOnly) },
                    onDisablePin = { viewModel.disablePin() },
                    onRestoreJson = { viewModel.restoreFromJson(it) },
                    onSignOut = {
                      AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                    },
                    onFeedback = { viewModel.showFeedback(it) },
                  )
              }
            }
          }
        }
      }
    }
  }
}
