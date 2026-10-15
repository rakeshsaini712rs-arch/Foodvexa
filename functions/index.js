const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();

exports.deleteFoodvexaAccount = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Please sign in to delete your account.");
  }

  // Require a recent sign-in before performing this irreversible action.
  const authTime = Number(request.auth.token.auth_time || 0);
  if (!authTime || Math.floor(Date.now() / 1000) - authTime > 10 * 60) {
    throw new HttpsError(
      "failed-precondition",
      "For your security, sign in again before deleting your account."
    );
  }

  const uid = request.auth.uid;
  const db = getFirestore();

  // Preserve order totals/items where needed for accounting, but remove personal
  // contact/address details and unlink retained orders from the deleted account.
  const orders = await db.collection("orders").where("customerUid", "==", uid).get();
  const docs = orders.docs;
  for (let i = 0; i < docs.length; i += 400) {
    const batch = db.batch();
    docs.slice(i, i + 400).forEach((doc) => {
      batch.update(doc.ref, {
        customerUid: "deleted-account",
        customerName: "Deleted customer",
        phone: FieldValue.delete(),
        address: FieldValue.delete(),
        customerEmail: FieldValue.delete(),
        updatedAt: FieldValue.serverTimestamp(),
      });
    });
    await batch.commit();
  }

  // Remove common per-user documents if present; order records are handled above.
  for (const collectionName of ["users", "customers", "customerProfiles", "favorites"]) {
    await db.collection(collectionName).doc(uid).delete().catch((error) => {
      if (error.code !== 5 && error.code !== "not-found") throw error;
    });
  }

  await getAuth().deleteUser(uid);
  return { success: true, anonymizedOrders: docs.length };
});
