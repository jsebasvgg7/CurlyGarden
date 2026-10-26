const crypto = require("crypto");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const apiKey = defineSecret("CLOUDINARY_API_KEY");
const apiSecret = defineSecret("CLOUDINARY_API_SECRET");
const cloudName = defineString("CLOUDINARY_CLOUD_NAME");

const COUPLE_QUOTA_BYTES = 200 * 1024 * 1024;
const MAX_UPLOAD_BYTES = 3 * 1024 * 1024;

function sign(params, secret) {
  const base = Object.keys(params)
    .sort()
    .map((key) => `${key}=${params[key]}`)
    .join("&");
  return crypto.createHash("sha1").update(base + secret).digest("hex");
}

exports.signCloudinaryUpload = onCall(
  { secrets: [apiKey, apiSecret] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Debes iniciar sesion.");
    }

    const bytes = Number(request.data?.bytes);
    if (!Number.isInteger(bytes) || bytes <= 0 || bytes > MAX_UPLOAD_BYTES) {
      throw new HttpsError("invalid-argument", "Tamano de archivo no valido.");
    }

    const db = getFirestore();
    const userSnap = await db.collection("users").doc(uid).get();
    const coupleId = userSnap.exists ? userSnap.data().coupleId : null;
    if (!coupleId) {
      throw new HttpsError("failed-precondition", "No tienes pareja vinculada.");
    }

    const coupleRef = db.collection("couples").doc(coupleId);
    await db.runTransaction(async (tx) => {
      const coupleSnap = await tx.get(coupleRef);
      if (!coupleSnap.exists) {
        throw new HttpsError("failed-precondition", "La pareja no existe.");
      }
      const data = coupleSnap.data();
      if (data.user1Id !== uid && data.user2Id !== uid) {
        throw new HttpsError("permission-denied", "No perteneces a esta pareja.");
      }
      const used = Number(data.mediaBytes || 0);
      if (used + bytes > COUPLE_QUOTA_BYTES) {
        throw new HttpsError("resource-exhausted", "Se lleno el espacio de recuerdos.");
      }
      tx.update(coupleRef, { mediaBytes: FieldValue.increment(bytes) });
    });

    const timestamp = Math.floor(Date.now() / 1000);
    const folder = `memories/${coupleId}`;
    const publicId = crypto.randomBytes(12).toString("hex");
    const signature = sign({ folder, public_id: publicId, timestamp }, apiSecret.value());

    return {
      cloudName: cloudName.value(),
      apiKey: apiKey.value(),
      timestamp,
      folder,
      publicId,
      signature
    };
  }
);
