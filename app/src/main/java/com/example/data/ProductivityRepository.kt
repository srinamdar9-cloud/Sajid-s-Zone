package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object FirestoreProvider {
  @Volatile private var configured = false

  fun getInstance(context: Context): FirebaseFirestore {
    val dbId = context.applicationContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(dbId)
    if (!configured) {
      synchronized(this) {
        if (!configured) {
          try {
            val settings =
              FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                  PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                )
                .build()
            db.firestoreSettings = settings
          } catch (_: IllegalStateException) {
            // Settings already applied before first operation
          }
          configured = true
        }
      }
    }
    return db
  }
}

class ProductivityRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Sajid's Zone data.")

  private fun tasksCollection(uid: String) =
    db.collection("users").document(uid).collection("tasks")

  private fun journalCollection(uid: String) =
    db.collection("users").document(uid).collection("journal")

  private fun expensesCollection(uid: String) =
    db.collection("users").document(uid).collection("expenses")

  private fun preferencesDoc(uid: String) =
    db.collection("users").document(uid).collection("settings").document("preferences")

  // --- TASKS ---

  fun observeTasks(userId: String): Flow<List<TaskItem>> = flow {
    val col = tasksCollection(userId)
    emitAll(
      col.snapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
      }.catch { error ->
        logIfAuthenticated(error, OperationType.LIST, col.path, userId)
        throw error
      }
    )
  }

  suspend fun createTask(task: TaskItem, explicitId: String? = null): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val col = tasksCollection(uid)
    val docRef = if (explicitId.isNullOrBlank()) col.document() else col.document(explicitId)
    return try {
      docRef.set(task.toCreateMap(uid)).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateTask(task: TaskItem): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
    val docRef = tasksCollection(uid).document(task.id)
    return try {
      docRef.update(task.toUpdateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteTask(taskId: String): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = tasksCollection(uid).document(taskId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getTaskById(taskId: String): Result<TaskItem?> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = tasksCollection(uid).document(taskId)
    return try {
      val snap = docRef.get().await()
      Result.success(
        snap.toObject(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  // --- JOURNAL ---

  fun observeJournalEntries(userId: String): Flow<List<JournalEntry>> = flow {
    val col = journalCollection(userId)
    emitAll(
      col.snapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(JournalEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
      }.catch { error ->
        logIfAuthenticated(error, OperationType.LIST, col.path, userId)
        throw error
      }
    )
  }

  suspend fun upsertJournalEntry(entry: JournalEntry): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val col = journalCollection(uid)
    val isNew = entry.id.isBlank()
    val docRef = if (isNew) col.document() else col.document(entry.id)
    return try {
      if (isNew) {
        docRef.set(entry.toCreateMap(uid)).await()
      } else {
        // Check whether doc exists in case an optimistic draft ID wasn't created yet
        val existing = docRef.get().await()
        if (existing.exists()) {
          docRef.update(entry.toUpdateMap()).await()
        } else {
          docRef.set(entry.toCreateMap(uid)).await()
        }
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteJournalEntry(entryId: String): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = journalCollection(uid).document(entryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- EXPENSES ---

  fun observeExpenses(userId: String): Flow<List<ExpenseItem>> = flow {
    val col = expensesCollection(userId)
    emitAll(
      col.snapshots().map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          doc.toObject(ExpenseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
      }.catch { error ->
        logIfAuthenticated(error, OperationType.LIST, col.path, userId)
        throw error
      }
    )
  }

  suspend fun createExpense(expense: ExpenseItem, explicitId: String? = null): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val col = expensesCollection(uid)
    val docRef = if (explicitId.isNullOrBlank()) col.document() else col.document(explicitId)
    return try {
      docRef.set(expense.toCreateMap(uid)).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateExpense(expense: ExpenseItem): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    require(expense.id.isNotBlank()) { "Expense ID must not be blank for update" }
    val docRef = expensesCollection(uid).document(expense.id)
    return try {
      docRef.update(expense.toUpdateMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteExpense(expenseId: String): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = expensesCollection(uid).document(expenseId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- PREFERENCES ---

  fun observePreferences(userId: String): Flow<UserPreferences> = flow {
    val docRef = preferencesDoc(userId)
    emitAll(
      docRef.snapshots().map { snap ->
        snap.toObject(UserPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserPreferences(userId = userId)
      }.catch { error ->
        logIfAuthenticated(error, OperationType.GET, docRef.path, userId)
        throw error
      }
    )
  }

  suspend fun savePreferences(prefs: UserPreferences): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val docRef = preferencesDoc(uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        docRef.update(prefs.toUpdateMap()).await()
      } else {
        docRef.set(prefs.toCreateMap(uid)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  private fun logIfAuthenticated(
    error: Throwable,
    op: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception) ?: error
    if (auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
  }
}
