const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
initializeApp();
const db = getFirestore();

async function sendRole(role, title, body, data) {
  const docs = await db.collection("notificationTokens").where("role", "==", role).get();
  const entries = docs.docs.filter(d => typeof d.get("token") === "string" && d.get("token"));
  if (!entries.length) return;
  const result = await getMessaging().sendEachForMulticast({
    tokens: entries.map(d => d.get("token")), notification: { title, body },
    data: Object.fromEntries(Object.entries(data).map(([k,v]) => [k, String(v ?? "")])),
    android: { priority: "high", notification: { channelId: "foodvexa_orders", sound: "default" } }
  });
  await Promise.all(result.responses.map((r,i) => {
    if (!r.success && ["messaging/registration-token-not-registered","messaging/invalid-registration-token"].includes(r.error?.code)) return entries[i].ref.delete();
  }));
}
exports.notifyAdminOnNewOrder = onDocumentCreated("orders/{orderId}", async event => {
  if (!event.data) return;
  await sendRole("admin", "Foodvexa: New Order", "A new customer order has been received.", { orderId: event.params.orderId, event: "new_order" });
});
exports.notifyDeliveryOnAssignment = onDocumentUpdated("orders/{orderId}", async event => {
  const before = event.data?.before.data(), after = event.data?.after.data();
  if (!before || !after || !after.deliveryBoyUid || after.deliveryBoyUid === before.deliveryBoyUid) return;
  const doc = await db.collection("notificationTokens").doc(after.deliveryBoyUid).get();
  if (doc.get("role") !== "delivery" || !doc.get("token")) return;
  await getMessaging().send({
    token: doc.get("token"), notification: { title: "Foodvexa: New Delivery Assigned", body: "A new order has been assigned to you." },
    data: { orderId: event.params.orderId, event: "assigned_order" },
    android: { priority: "high", notification: { channelId: "foodvexa_orders", sound: "default" } }
  });
});
