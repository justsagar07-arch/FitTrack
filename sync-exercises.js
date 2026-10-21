const { getFirestore } = require("firebase-admin/firestore");
const admin = require("firebase-admin");
const fs = require("fs");

const SOURCE_FILE =
  "./app/src/main/java/com/fittrack/app/models/ExerciseDataProvider.java";
const COLLECTION = "exercise";
const DRY_RUN = true;

function normalize(value = "") {
  return value.toLowerCase().replace(/[^a-z0-9]/g, "");
}

function makeId(name = "") {
  return name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

function readExercisesFromJava(source) {
  const regex =
    /new\s+Exercise\s*\(\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*,\s*"((?:\\.|[^"\\])*)"\s*\)/gs;

  const exercises = [];
  let match;

  while ((match = regex.exec(source)) !== null) {
    exercises.push({
      name: match[1],
      category: match[2],
      description: match[3],
      difficulty: match[4],
      durationOrReps: match[5],
      safetyTips: match[6]
    });
  }

  return exercises;
}

async function main() {
  if (!fs.existsSync(SOURCE_FILE)) {
    throw new Error(
      "ExerciseDataProvider.java not found in the current folder."
    );
  }

  const source = fs.readFileSync(SOURCE_FILE, "utf8");
  const exercises = readExercisesFromJava(source);

  console.log(`Exercises read from Java: ${exercises.length}`);

  if (exercises.length !== 47) {
    throw new Error(
      `Expected 47 exercises, found ${exercises.length}. No writes performed.`
    );
  }

const serviceAccount = require("./serviceAccountKey.json");

const app = admin.initializeApp({
  credential: admin.cert(serviceAccount),
  projectId: serviceAccount.project_id
});
  const db = getFirestore(app);
  const collection = db.collection(COLLECTION);
  const snapshot = await collection.get();

  const existingNames = new Set();
  const existingIds = new Set();

  snapshot.forEach(doc => {
    existingIds.add(normalize(doc.id));

    const data = doc.data();
    if (data.name) {
      existingNames.add(normalize(data.name));
    }
  });
  const plannedIds = new Set();

  for (const exercise of exercises) {
    const id = makeId(exercise.name);

    if (plannedIds.has(id)) {
      throw new Error(`Duplicate generated Firestore ID: ${id}`);
    }

    plannedIds.add(id);
  }

  const missing = [];
  const localNames = new Set();

  for (const exercise of exercises) {
    const nameKey = normalize(exercise.name);
    const id = makeId(exercise.name);
    const idKey = normalize(id);

    if (localNames.has(nameKey)) {
      throw new Error(`Duplicate name in Java source: ${exercise.name}`);
    }
    localNames.add(nameKey);

    if (existingNames.has(nameKey) || existingIds.has(idKey)) {
      console.log(`SKIP existing: ${exercise.name}`);
      continue;
    }

    missing.push({ id, data: exercise });
  }

  console.log(`Existing Firestore documents: ${snapshot.size}`);
  console.log(`Missing exercises to add: ${missing.length}`);

  if (snapshot.size + missing.length !== 47) {
    console.warn(
      "Warning: existing count plus missing count is not 47. " +
      "Review the exercise IDs and names before writing."
    );
  }

  if (missing.length === 0) {
    console.log("Nothing to add.");
    return;
  }
if (DRY_RUN) {
  console.log("DRY RUN: No Firestore writes performed.");
  console.log("Existing documents:", snapshot.size);
  console.log("Missing exercises:", missing.length);
  console.log(
    "Expected total after sync:",
    snapshot.size + missing.length
  );
  return;
}
  // create() fails rather than overwriting a document if its ID exists.
  // Keep batches below Firestore's 500-write limit.
  let added = 0;

  for (let start = 0; start < missing.length; start += 400) {
    const chunk = missing.slice(start, start + 400);
    const batch = db.batch();

    for (const item of chunk) {
      batch.create(collection.doc(item.id), item.data);
    }

    await batch.commit();
    added += chunk.length;
  }

  const finalSnapshot = await collection.get();

  console.log(`Added this run: ${added}`);
  console.log(`Final Firestore document count: ${finalSnapshot.size}`);

  await app.delete();
}

main().catch(error => {
  console.error("SYNC FAILED:", error.message);
  process.exitCode = 1;
});
