package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductivityRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ProductivityRepository(firestore, auth)
    val taskId = "task_${UUID.randomUUID().toString().replace("-", "")}"

    val task =
      TaskItem(
        title = "Quarterly Tax Prep",
        description = "Gather TDS statements",
        date = "2026-11-14",
        startTime = "10:00",
        deadlineTime = "17:00",
        reminderDate = "2026-11-14",
        reminderTime = "09:30",
        priority = TaskPriority.High.label,
        category = TaskCategory.Financial.label,
        status = TaskStatus.Pending.label,
        recurrence = TaskRecurrence.None.label,
      )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createTask(task, explicitId = taskId) }
    assertTrue("Task create failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
    }
    assertTrue(tasks.any { it.id == taskId && it.title == "Quarterly Tax Prep" })

    val updated = tasks.first { it.id == taskId }.copy(status = TaskStatus.Completed.label, completedAtEpochMs = 1763120000000L)
    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateTask(updated) }
    assertTrue("Task update failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
  }

  @Test
  fun createJournalAndExpense_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ProductivityRepository(firestore, auth)

    val journalId = "j_${UUID.randomUUID().toString().replace("-", "")}"
    val entry =
      JournalEntry(
        id = journalId,
        date = "2026-11-14",
        time = "21:00",
        title = "Focus Note",
        content = "Completed all morning blocks cleanly.",
        accomplished = "Shipped dashboard",
        wentWell = "Zero distractions",
        couldImprove = "Drink more water",
        tomorrowFocus = "Review weekly goals",
      )
    val journalRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertJournalEntry(entry) }
    assertTrue("Journal upsert failed: ${journalRes.exceptionOrNull()}", journalRes.isSuccess)

    val entries = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeJournalEntries(uid).first { list -> list.any { it.id == journalId } }
    }
    assertEquals(1, entries.count { it.id == journalId })

    val expenseId = "exp_${UUID.randomUUID().toString().replace("-", "")}"
    val expense =
      ExpenseItem(
        amount = 1250.75,
        description = "Weekly groceries",
        category = ExpenseCategory.Grocery.label,
        date = "2026-11-14",
        time = "18:30",
        paymentMethod = PaymentMethod.UPI.label,
        notes = "Supermarket",
      )
    val expRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.createExpense(expense, explicitId = expenseId) }
    assertTrue("Expense create failed: ${expRes.exceptionOrNull()}", expRes.isSuccess)

    val expenses = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeExpenses(uid).first { list -> list.any { it.id == expenseId } }
    }
    assertTrue(expenses.any { it.id == expenseId && it.amount == 1250.75 })
  }

  @Test
  fun crossUserAccess_isDeniedWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = ProductivityRepository(firestore, auth)
    val taskId = "task_${UUID.randomUUID().toString().replace("-", "")}"

    val task =
      TaskItem(
        title = "Alice Private Task",
        date = "2026-11-14",
      )
    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createTask(task, explicitId = taskId) }
    assertTrue(createResult.isSuccess)

    signInTestUser(BOB_EMAIL)
    val bobRepo = ProductivityRepository(firestore, auth)

    val exception = assertThrowsType<Exception> {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeTasks(aliceUid).first() }
    }
    val firestoreException = exception.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $exception", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = ProductivityRepository(firestore, auth)

    val exception = assertThrowsType<Exception> {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeTasks(aliceUid).first() }
    }
    val firestoreException = exception.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $exception", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_sz@test.com"
    const val BOB_EMAIL = "bob_sz@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
