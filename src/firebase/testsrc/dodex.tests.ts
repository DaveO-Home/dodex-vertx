import assert from "assert";
import * as firebase from "@firebase/rules-unit-testing";
import * as firebaseAdmin from "firebase-admin";
import { v4 as uuidv4 } from "uuid";
import * as dodex from "./dodex.types";
import {
  DocumentSnapshot,
  DocumentData,
  QuerySnapshot,
  QueryDocumentSnapshot,
  CollectionReference,
  Query
} from "@firebase/firestore-types"
import firebaseApp from "firebase";

process.env.FIREBASE_EMULATOR_HUB = "localhost:4400";
process.env.FIRESTORE_AUTHENTICATION_HOST = "localhost:9099";
process.env.FIRESTORE_EMULATOR_HOST = "localhost:6080";
// process.env.HOSTING_EMULATOR_HOST="localhost:5000";

const DODEX_PROJECT_ID: string = "dodex-firebase";
const adminUser: string = "admin";
const appUser: string = "user";
const dodexId: string = "user2"
const adminAuth: dodex.AdminAuth = { uid: dodexId, email: "user2@gmail.com", role: "admin" };
const userAuth: dodex.AdminAuth = { uid: dodexId, email: "user2@gmail.com", role: "user" };
const message: string = "A Message from user1 to user2";

function getFirestoreApp(auth: dodex.AdminAuth | undefined): firebaseApp.app.App {
  return firebase
    .initializeTestApp({ projectId: DODEX_PROJECT_ID, auth: auth });
}

function getAdminFirestoreApp(): firebaseApp.app.App {
  // return firebaseAdmin.initializeApp({ projectId: MY_PROJECT_ID, auth: auth });
  return firebase.initializeAdminApp({ projectId: DODEX_PROJECT_ID });
}

function getFirestore(auth: dodex.AdminAuth | undefined): firebaseApp.firestore.Firestore {
  return getFirestoreApp(auth).firestore();
}

function getAdminFirestore(): firebaseApp.firestore.Firestore {
  return getAdminFirestoreApp().firestore();
}

function buildNewUser(newUser: string, newPassword: string): dodex.User {
  return {
    name: newUser,
    password: newPassword,
    user_id: uuidv4(),
    ip: "127.0.0.1",
    last_login: firebaseAdmin.firestore.Timestamp.now()
  }
}

function buildNewMessage(
  newMessageName: string,
  newPassword: string,
  userId: string,
  fromHandle: string,
  message: string): dodex.Message {
  return {
    name: newMessageName,
    password: newPassword,
    message_id: uuidv4(),
    user_id: userId,
    message: message,
    from_handle: fromHandle,
    post_date: firebaseAdmin.firestore.Timestamp.now()
  }
}

function addDocUser(newUser: string, password: string, db: firebaseApp.firestore.Firestore): Promise<void> {
  let userObject = buildNewUser(newUser, password);
  const userDoc = db.collection("users").doc(userObject.name);
  return userDoc.set(userObject);
}

function addDocMessage(
  name: string,
  password: string,
  userId: string,
  fromHandle: string,
  message: string,
  db: firebaseApp.firestore.Firestore): Promise<void> {
  let messageObject = buildNewMessage(name, password, userId, fromHandle, message);
  const messageDoc = db.collection("messages").doc(messageObject.name);
  return messageDoc.set(messageObject);
}

before(async () => {
  const db = getFirestore(undefined);
  await firebase.clearFirestoreData({ projectId: DODEX_PROJECT_ID });
});

describe("Dodex Firebase Model & Rules", () => {

  it("Write succeeds to a different user document as admin", async () => {
    const db = getAdminFirestore();

    const testDoc = db.collection("users").doc(adminUser);
    const timestamp = firebaseAdmin.firestore.Timestamp.now();

    await firebase.assertSucceeds(testDoc.set({ foo: timestamp }));
  });

  it("Write fails to document as non-admin user", async () => {
    const db = getFirestore(userAuth);

    const timestamp = firebase.firestore.Timestamp.now();
    const testDoc = db.collection('users').doc(appUser);

    await firebase.assertFails(testDoc.set({ last_login: timestamp }));
    await firebase.clearFirestoreData({ projectId: DODEX_PROJECT_ID });
  });

  it("Add User documents to Users collection", async () => {
    const db = getAdminFirestore();

    let userDoc: Promise<void> = addDocUser("newUser1", "password1", db);
    await firebase.assertSucceeds(userDoc);

    userDoc = addDocUser("newUser2", "password2", db);
    await firebase.assertSucceeds(userDoc);
  });

  it("Add Message documents to Messages collection", async () => {
    const db = getAdminFirestore();
    // User sending message - added in previous test
    let userQuery = db.collection('users').doc("newUser1");
    await firebase.assertSucceeds(userQuery.get());

    let user: DocumentSnapshot<DocumentData> = await userQuery.get();
    const data1: DocumentData | undefined = user.data();

    assert.ok(data1 !== undefined && data1.name.length > 0);

    userQuery = db.collection('users').doc("newUser2"); // User receiving message
    await firebase.assertSucceeds(userQuery.get());

    user = await userQuery.get();
    const data2: DocumentData | undefined = user.data();

    assert.ok(data2 !== undefined && data2.name.length > 0);

    let newMessage = buildNewMessage(data2.name, data2.password, data2.user_id, data1.name, message);
    let userDoc: CollectionReference<DocumentData> = db.collection("messages").doc(newMessage.name).collection("documents");

    await firebase.assertSucceeds(userDoc.add(newMessage));
    newMessage.message = "This is message two";
    await firebase.assertSucceeds(userDoc.add(newMessage));

    // Add messages to 'newUser1' to ensure they are not deleted when deleting 'newUser2' messages
    userDoc = db.collection("messages").doc("newUser1").collection("documents");
    await firebase.assertSucceeds(userDoc.add(newMessage));
    newMessage.message = "This is message two";
    await firebase.assertSucceeds(userDoc.add(newMessage));
  });

  it("Return message documents for user", async () => {
    const db = getAdminFirestore();
    let messageUser = "newUser2";
    let messageQuery: CollectionReference<DocumentData> = db.collection(`messages/${messageUser}/documents`);
    await firebase.assertSucceeds(messageQuery.get());

    let documents: QuerySnapshot<DocumentData> = await messageQuery.get();
    const data2: QueryDocumentSnapshot<DocumentData>[] = documents.docs;

    assert.ok(data2 !== undefined && data2.length > 1);

    let previousName: string | null = null;
    data2.forEach((row: QueryDocumentSnapshot<DocumentData>) => {
      if (previousName === null) {
        previousName = row.get("name");
      }
      else {
        assert.strictEqual(previousName, row.get("name"));
      }
      const name = row.get("name");
      // do something with row...
    })

    messageUser = "newUser1";
    messageQuery = db.collection(`messages/${messageUser}/documents`);
    await firebase.assertSucceeds(messageQuery.get());

    documents = await messageQuery.get();
    const data1: QueryDocumentSnapshot<DocumentData>[] = documents.docs;

    assert.ok(data1 !== undefined && data1.length > 1);

    previousName = null;
    data1.forEach((row: QueryDocumentSnapshot<DocumentData>) => {
      if (previousName === null) {
        previousName = row.get("name");
      }
      else {
        assert.strictEqual(previousName, row.get("name"));
      }
      const name = row.get("name");
    });
  });

  it("Delete message documents for User", async () => {
    const db = getAdminFirestore();
    let messageUser = "newUser2";
    let messageQuery: CollectionReference<DocumentData> = db.collection(`messages/${messageUser}/documents`);

    let documents: QuerySnapshot<DocumentData> = await messageQuery.get();
    assert.ok(documents.size > 1);
    const data2: QueryDocumentSnapshot<DocumentData>[] = documents.docs;

    data2.forEach(async (row: QueryDocumentSnapshot<DocumentData>) => {
      row.ref.delete().catch(err => {
        throw err;
      });
    })
    setTimeout(async () => {
      messageQuery = db.collection(`messages/${messageUser}/documents`);
      documents = await messageQuery.get();
      assert.strictEqual(documents.size, 0);
    }, 25);
  });

  it("Ensure other user messages are not deleted", async () => {
    const db = getAdminFirestore();
    let messageUser = "newUser1";
    let messageQuery: CollectionReference<DocumentData> = db.collection(`messages/${messageUser}/documents`);

    let documents: QuerySnapshot<DocumentData> = await messageQuery.get();
    assert.ok(documents.size > 1);
  });

  it("Get all users from Firestore", async () => {
    const db = getAdminFirestore();
    const usersQuery: CollectionReference<DocumentData> = db.collection(`users`);

    const users: QuerySnapshot<DocumentData> = await usersQuery.get();
    assert.ok(users.size > 1);
    const data: QueryDocumentSnapshot<DocumentData>[] = users.docs;
    data.forEach((row: QueryDocumentSnapshot<DocumentData>) => {
      assert.ok(["newUser1", "newUser2"].indexOf(row.data().name) > -1);
    })
  });

  it("Get user from Firestore by name", async () => {
    const db = getAdminFirestore();
    const usersQuery: Query<DocumentData> = db.collection("users").where("name", "==", "newUser1");;

    const users: QuerySnapshot<DocumentData> = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data: QueryDocumentSnapshot<DocumentData>[] = users.docs;
    const name: string = data[0].data().name;
    assert.strictEqual("newUser1", name);
  });

  it("Delete a user from Firestore", async () => {
    const db = getAdminFirestore();

    const usersQuery: Query<DocumentData> = db.collection("users").where("name", "==", "newUser1");

    let users: QuerySnapshot<DocumentData> = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data: QueryDocumentSnapshot<DocumentData>[] = users.docs;

    await firebase.assertSucceeds(data[0].ref.delete());

    users = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 0);
  });

  it("Update a user in Firestore", async () => {
    const db = getAdminFirestore();

    const usersQuery: Query<DocumentData> = db.collection("users").where("name", "==", "newUser2");

    let users: QuerySnapshot<DocumentData> = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data: QueryDocumentSnapshot<DocumentData>[] = users.docs;

    const timestamp: firebaseAdmin.firestore.Timestamp = firebaseAdmin.firestore.Timestamp.now();
    await firebase.assertSucceeds(data[0].ref.update({ last_login: timestamp, ip: "localhost" }));

    users = await firebase.assertSucceeds(usersQuery.get());
    assert.strictEqual(users.docs[0].data().ip, "localhost");
    assert.ok(timestamp.isEqual(users.docs[0].data().last_login));
  });
});

after(async () => {
  // await firebase.clearFirestoreData({ projectId: DODEX_PROJECT_ID });
});
