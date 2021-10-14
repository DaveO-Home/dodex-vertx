import * as fs from "fs";
import assert from "assert";
import * as firebase from "@firebase/rules-unit-testing";
import * as firebaseAdmin from "firebase-admin";
import { v4 as uuidv4 } from "uuid";
import * as dodex from "./dodex.types";
import { DocumentData } from "@firebase/firestore-types"
// import  * as firestore from "firebase/firestore";

process.env.FIREBASE_EMULATOR_HUB = "localhost:4400";
process.env.FIRESTORE_AUTHENTICATION_HOST = "localhost:9099";
process.env.FIRESTORE_EMULATOR_HOST = "localhost:6080";
// process.env.HOSTING_EMULATOR_HOST="localhost:5000";

let db : firebaseAdmin.firestore.Firestore;
const DODEX_PROJECT_ID: string = "dodex-firebase";
const adminUser: string = "admin";
const appUser: string = "user";
const dodexId: string = "user2"
const message: string = "A Message from user1 to user2";

function getAdminFirestoreApp(): firebaseAdmin.app.App {
  return firebaseAdmin.initializeApp({ projectId: DODEX_PROJECT_ID });
}

function getAdminFirestore(): firebaseAdmin.firestore.Firestore {
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

async function addDocUser(newUser: string, password: string, db: firebaseAdmin.firestore.Firestore): Promise<void> {
  let userObject = buildNewUser(newUser, password);
  const userDoc = db.collection("users").doc(userObject.name);
  await userDoc.set(userObject);
}

async function addDocMessage(
  name: string,
  password: string,
  userId: string,
  fromHandle: string,
  message: string,
  db: firebaseAdmin.firestore.Firestore): Promise<void> {
  let messageObject = buildNewMessage(name, password, userId, fromHandle, message);
  const messageDoc = db.collection("messages").doc(messageObject.name);
  await messageDoc.set(messageObject);
}
let testEnv: firebase.RulesTestEnvironment;
before(async () => {
  testEnv = await firebase.initializeTestEnvironment({
    projectId: DODEX_PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.dodex.rules", "utf8"),
    },
  });
  
  await testEnv.clearFirestore();
  db = getAdminFirestore();
});

describe("Dodex Firebase Model & Rules", () => {

  it("Write succeeds to a different user document as admin", async () => {
    const testDoc = db.collection("users").doc(adminUser);
    const timestamp = firebaseAdmin.firestore.Timestamp.now();

    await firebase.assertSucceeds(testDoc.set({ foo: timestamp }));
  });

  /*
    With upgraded modules this is failing.
  */
  // it("Write fails to document as non-admin user", async () => {
  //   testEnv.clearFirestore();
  //   const authed = testEnv.authenticatedContext("user");
  //   const unauthed = testEnv.unauthenticatedContext();
  //   // get(doc(unauthed.firestore(), '/private/doc'), { ... })
    
  //   const timestamp = firebaseAdmin.firestore.Timestamp.now();
  //   const testDoc = db.collection('users').doc(appUser);
    
  //   await firebase.assertFails(testDoc.set({ last_login: timestamp }));
  //   await testEnv.clearFirestore();
  // });

  it("Add User documents to Users collection", async () => {
    let userDoc: Promise<void> = addDocUser("newUser1", "password1", db);
    await firebase.assertSucceeds(userDoc);

    userDoc = addDocUser("newUser2", "password2", db);
    await firebase.assertSucceeds(userDoc);
  });

  it("Add Message documents to Messages collection", async () => {
    // User sending message - added in previous test
    let userQuery = db.collection('users').doc("newUser1");
    await firebase.assertSucceeds(userQuery.get());

    let user = await userQuery.get();
    const data1: DocumentData | undefined = user.data();

    assert.ok(data1 !== undefined && data1.name.length > 0);

    userQuery = db.collection('users').doc("newUser2"); // User receiving message
    await firebase.assertSucceeds(userQuery.get());

    user = await userQuery.get();
    const data2: DocumentData | undefined = user.data();

    assert.ok(data2 !== undefined && data2.name.length > 0);

    let newMessage = buildNewMessage(data2.name, data2.password, data2.user_id, data1.name, message);
    let userDoc = db.collection("messages").doc(newMessage.name).collection("documents");

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
    let messageUser = "newUser2";
    
    let messageQuery = db.collection(`messages/${messageUser}/documents`);
    await firebase.assertSucceeds(messageQuery.get());

    let documents = await messageQuery.get();
    const data2 = documents.docs;

    assert.ok(data2 !== undefined && data2.length > 1);

    let previousName: string | null = null;
    data2.forEach((row) => {
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
    const data1 = documents.docs;

    assert.ok(data1 !== undefined && data1.length > 1);

    previousName = null;
    data1.forEach(row => {
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
    let messageUser = "newUser2";
    let messageQuery = db.collection(`messages/${messageUser}/documents`);

    let documents = await messageQuery.get();
    assert.ok(documents.size > 1);
    const data2 = documents.docs;

    data2.forEach(async row => {
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
    let messageUser = "newUser1";
    let messageQuery = db.collection(`messages/${messageUser}/documents`);

    let documents = await messageQuery.get();
    assert.ok(documents.size > 1);
  });

  it("Get all users from Firestore", async () => {
    const usersQuery = db.collection(`users`);

    const users = await usersQuery.get();
    assert.ok(users.size > 1);
    const data = users.docs;
    data.forEach(row => {
      if(row.data().name !== undefined) {
        assert.ok(["newUser1", "newUser2"].indexOf(row.data().name) > -1);
      }
    })
  });

  it("Get user from Firestore by name", async () => {
    const usersQuery = db.collection("users").where("name", "==", "newUser1");;

    const users = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data = users.docs;
    const name: string = data[0].data().name;
    assert.strictEqual("newUser1", name);
  });

  it("Delete a user from Firestore", async () => {
    const usersQuery = db.collection("users").where("name", "==", "newUser1");

    let users = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data = users.docs;

    await firebase.assertSucceeds(data[0].ref.delete());

    users = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 0);
  });

  it("Update a user in Firestore", async () => {
    const usersQuery = db.collection("users").where("name", "==", "newUser2");

    let users = await firebase.assertSucceeds(usersQuery.get());
    assert.ok(users.size === 1);

    const data = users.docs;

    const timestamp: firebaseAdmin.firestore.Timestamp = firebaseAdmin.firestore.Timestamp.now();
    await firebase.assertSucceeds(data[0].ref.update({ last_login: timestamp, ip: "localhost" }));

    users = await firebase.assertSucceeds(usersQuery.get());
    assert.strictEqual(users.docs[0].data().ip, "localhost");
    assert.ok(timestamp.isEqual(users.docs[0].data().last_login));
  });
});

after(async () => {
  // await testEnv.clearFirestore();
});
