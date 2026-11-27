const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');
const fs = require('fs');
const path = require('path');

async function testRules() {
  const projectId = `test-project-${Date.now()}`;
  const testEnv = await initializeTestEnvironment({
    projectId,
    firestore: { rules: fs.readFileSync(path.join(__dirname, '../firestore.rules'), 'utf8') },
  });

  const alice = testEnv.authenticatedContext('alice');
  const db = alice.firestore();
  const { serverTimestamp, Timestamp } = firebase.firestore.FieldValue
    ? {
        serverTimestamp: () => firebase.firestore.FieldValue.serverTimestamp(),
        Timestamp: firebase.firestore.Timestamp,
      }
    : { serverTimestamp: () => new Date(), Timestamp: { fromDate: (d) => d } };

  // Seed active and legacy groups
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('groups').doc('group1').set({
      memberIds: ['alice'],
      status: 'ACTIVE',
    });
    await adminDb.collection('groups').doc('legacyGroup').set({
      memberIds: ['alice'],
    });
  });

  const groupRef = db.collection('groups').doc('group1');
  const legacyGroupRef = db.collection('groups').doc('legacyGroup');

  // 1. Legacy group compatibility
  await assertSucceeds(
    legacyGroupRef.collection('settlements').doc('s1').set({
      id: 's1',
      groupId: 'legacyGroup',
      amount: 100,
    })
  );

  // 2. Active writes with serverTimestamp() succeed across all 6 synced subcollections
  const subcollections = [
    ['expenses', { expenseId: 'e1', groupId: 'group1', title: 'Dinner', serverUpdatedAt: serverTimestamp() }],
    ['contributions', { contributionId: 'c1', groupId: 'group1', amountCents: 1000, serverUpdatedAt: serverTimestamp() }],
    ['cash_withdrawals', { withdrawalId: 'w1', groupId: 'group1', amountWithdrawn: 5000, serverUpdatedAt: serverTimestamp() }],
    ['cash_transfers', { groupId: 'group1', amountCents: 500, serverUpdatedAt: serverTimestamp() }],
    ['subunits', { subunitId: 'sub1', groupId: 'group1', name: 'Couple', serverUpdatedAt: serverTimestamp() }],
    ['settlements', { id: 'set1', groupId: 'group1', amountCents: '1500', serverUpdatedAt: serverTimestamp() }],
  ];
  for (const [col, payload] of subcollections) {
    await assertSucceeds(groupRef.collection(col).doc('doc-active').set(payload));
  }

  // 3. Soft-delete tombstone writes succeed
  const expireAt = Timestamp.fromDate(new Date(Date.now() + 60 * 24 * 60 * 60 * 1000));
  await assertSucceeds(
    groupRef.collection('expenses').doc('doc-active').set(
      {
        groupId: 'group1',
        isDeleted: true,
        deletedAt: serverTimestamp(),
        serverUpdatedAt: serverTimestamp(),
        expireAt,
        lastUpdatedBy: 'alice',
      },
      { merge: true }
    )
  );

  // 4. Forged non-server serverUpdatedAt timestamp is rejected
  await assertFails(
    groupRef.collection('expenses').doc('doc-forged').set({
      expenseId: 'doc-forged',
      groupId: 'group1',
      title: 'Forged',
      serverUpdatedAt: Timestamp.fromDate(new Date(Date.now() - 86400 * 1000)),
    })
  );

  // 5. Unbounded .get() / list without .limit(...) and .limit(501) are rejected on synced subcollections
  for (const [col] of subcollections) {
    await assertFails(groupRef.collection(col).get());
    await assertFails(groupRef.collection(col).limit(501).get());
  }

  // 6. Bounded delta query .where('serverUpdatedAt', '>', ts).orderBy('serverUpdatedAt').limit(500) succeeds
  const cursorTs = Timestamp.fromDate(new Date(0));
  await assertSucceeds(
    groupRef
      .collection('expenses')
      .where('serverUpdatedAt', '>', cursorTs)
      .orderBy('serverUpdatedAt')
      .limit(500)
      .get()
  );

  // 7. Oversized payloads (excess keys / oversized string) are rejected
  const excessKeysPayload = { groupId: 'group1', serverUpdatedAt: serverTimestamp() };
  for (let i = 0; i < 50; i += 1) {
    excessKeysPayload[`extra_${i}`] = i;
  }
  await assertFails(groupRef.collection('expenses').doc('doc-excess-keys').set(excessKeysPayload));
  await assertFails(
    groupRef.collection('expenses').doc('doc-long-title').set({
      groupId: 'group1',
      title: 'x'.repeat(201),
      serverUpdatedAt: serverTimestamp(),
    })
  );

  console.log('ALL FIRESTORE RULES DELTA SYNC CHECKS PASSED');
  await testEnv.cleanup();
}

testRules().catch((err) => {
  console.error('FIRESTORE RULES TEST FAILED:', err);
  process.exit(1);
});
