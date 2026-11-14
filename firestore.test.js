const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

function pastDate() {
  return new Date(Date.now() - 2000);
}

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTask(uid = ALICE_UID) {
  return {
    userId: uid,
    title: "Pay electricity bill",
    description: "Monthly BESCOM bill",
    date: "2026-11-14",
    startTime: "10:00",
    deadlineTime: "18:00",
    reminderDate: "2026-11-14",
    reminderTime: "09:30",
    priority: "High",
    category: "Financial",
    status: "Pending",
    recurrence: "Monthly",
    completedAtEpochMs: 0,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validJournal(uid = ALICE_UID) {
  return {
    userId: uid,
    date: "2026-11-14",
    time: "21:30",
    title: "Evening Check-in",
    content: "Productive focus block today.",
    accomplished: "Completed quarterly review",
    wentWell: "Morning walk and deep work",
    couldImprove: "Start earlier",
    tomorrowFocus: "Plan sprint",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validExpense(uid = ALICE_UID) {
  return {
    userId: uid,
    amount: 450.5,
    description: "Fresh vegetables",
    category: "Grocery",
    date: "2026-11-14",
    time: "18:15",
    paymentMethod: "UPI",
    notes: "Local market",
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

function validPrefs(uid = ALICE_UID) {
  return {
    userId: uid,
    themeMode: "Dark",
    use24HourTime: false,
    notificationsEnabled: true,
    headsUpMinutes: 15,
    overdueNudgeEnabled: true,
    pinEnabled: false,
    pinHash: "",
    lockJournalOnly: true,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/journal`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/tasks/task_1`).set(validTask()));
});

test("Owner can create, read, update, and delete tasks, journal, expenses, and preferences", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const taskRef = aliceDb.doc(`users/${ALICE_UID}/tasks/task_1`);
  await assertSucceeds(taskRef.set(validTask()));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertSucceeds(
    taskRef.update({
      status: "Completed",
      completedAtEpochMs: 1763120000000,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(taskRef.delete());

  const journalRef = aliceDb.doc(`users/${ALICE_UID}/journal/entry_1`);
  await assertSucceeds(journalRef.set(validJournal()));
  await assertSucceeds(journalRef.update({ title: "Updated title", updatedAt: pastDate() }));

  const expRef = aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`);
  await assertSucceeds(expRef.set(validExpense()));
  await assertSucceeds(expRef.update({ amount: 500, updatedAt: pastDate() }));

  const prefRef = aliceDb.doc(`users/${ALICE_UID}/settings/preferences`);
  await assertSucceeds(prefRef.set(validPrefs()));
  await assertSucceeds(prefRef.update({ themeMode: "Light", updatedAt: pastDate() }));
});

test("Cross-user access is strictly denied across all collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/tasks/task_1`).set(validTask()));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/journal/entry_1`).set(validJournal()));
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).set(validExpense()));

  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/tasks/task_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/journal/entry_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/expenses/exp_1`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/tasks/task_2`).set(validTask(BOB_UID)));
});

test("Shadow fields, invalid enums, and spoofed userId are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/tasks/task_bad`).set({
      ...validTask(),
      isAdmin: true,
    })
  );

  // Invalid expense amount <= 0 rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_bad`).set({
      ...validExpense(),
      amount: 0,
    })
  );

  // Spoofed userId inside document body rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/journal/j_bad`).set({
      ...validJournal(BOB_UID),
    })
  );

  // Wrong settings document ID rejected
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/other_doc`).set(validPrefs())
  );
});
