const crypto = require("crypto");
const { onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { defineSecret, defineString } = require("firebase-functions/params");
const { getFirestore } = require("firebase-admin/firestore");

const apiKey = defineSecret("CLOUDINARY_API_KEY");
const apiSecret = defineSecret("CLOUDINARY_API_SECRET");
const cloudName = defineString("CLOUDINARY_CLOUD_NAME");

const REGION = "us-central1";

function sign(params, secret) {
  const base = Object.keys(params)
    .sort()
    .map((key) => `${key}=${params[key]}`)
    .join("&");
  return crypto.createHash("sha1").update(base + secret).digest("hex");
}

async function destroyImage(publicId) {
  const timestamp = Math.floor(Date.now() / 1000);
  const signature = sign(
    { invalidate: "true", public_id: publicId, timestamp },
    apiSecret.value()
  );
  const body = new URLSearchParams({
    public_id: publicId,
    timestamp: String(timestamp),
    invalidate: "true",
    api_key: apiKey.value(),
    signature,
  });
  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${cloudName.value()}/image/destroy`,
    { method: "POST", body }
  );
  if (!response.ok) {
    throw new Error(`Cloudinary ${response.status}: ${await response.text()}`);
  }
  const json = await response.json();
  return json.result;
}

async function releaseBytes(coupleId, bytes) {
  const db = getFirestore();
  const coupleRef = db.collection("couples").doc(coupleId);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(coupleRef);
    if (!snap.exists) return;
    const used = Number(snap.get("mediaBytes") || 0);
    tx.update(coupleRef, { mediaBytes: Math.max(0, used - bytes) });
  });
}

exports.cleanupMemoryMedia = onDocumentDeleted(
  {
    document: "couples/{coupleId}/memories/{memoryId}",
    region: REGION,
    secrets: [apiKey, apiSecret],
  },
  async (event) => {
    const memory = event.data && event.data.data();
    if (!memory) return;
    const publicId = memory.publicId;
    if (typeof publicId !== "string" || publicId.length === 0) return;

    let result;
    try {
      result = await destroyImage(publicId);
    } catch (error) {
      console.error("cleanupMemoryMedia destroy", event.params.memoryId, error);
      return;
    }
    if (result !== "ok") return;

    const bytes = Number(memory.bytes);
    if (!Number.isInteger(bytes) || bytes <= 0) return;
    try {
      await releaseBytes(event.params.coupleId, bytes);
    } catch (error) {
      console.error("cleanupMemoryMedia quota", event.params.coupleId, error);
    }
  }
);
