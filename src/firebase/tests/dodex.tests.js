"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    Object.defineProperty(o, k2, { enumerable: true, get: function() { return m[k]; } });
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const assert_1 = __importDefault(require("assert"));
const firebase = __importStar(require("@firebase/rules-unit-testing"));
const firebaseAdmin = __importStar(require("firebase-admin"));
const uuid_1 = require("uuid");
process.env.FIREBASE_EMULATOR_HUB = "localhost:4400";
process.env.FIRESTORE_AUTHENTICATION_HOST = "localhost:9099";
process.env.FIRESTORE_EMULATOR_HOST = "localhost:6080";
// process.env.HOSTING_EMULATOR_HOST="localhost:5000";
const DODEX_PROJECT_ID = "dodex-firebase";
const adminUser = "admin";
const appUser = "user";
const dodexId = "user2";
const adminAuth = { uid: dodexId, email: "user2@gmail.com", role: "admin" };
const userAuth = { uid: dodexId, email: "user2@gmail.com", role: "user" };
const message = "A Message from user1 to user2";
function getFirestoreApp(auth) {
    return firebase
        .initializeTestApp({ projectId: DODEX_PROJECT_ID, auth: auth });
}
function getAdminFirestoreApp() {
    // return firebaseAdmin.initializeApp({ projectId: MY_PROJECT_ID, auth: auth });
    return firebase.initializeAdminApp({ projectId: DODEX_PROJECT_ID });
}
function getFirestore(auth) {
    return getFirestoreApp(auth).firestore();
}
function getAdminFirestore() {
    return getAdminFirestoreApp().firestore();
}
function buildNewUser(newUser, newPassword) {
    return {
        name: newUser,
        password: newPassword,
        user_id: uuid_1.v4(),
        ip: "127.0.0.1",
        last_login: firebaseAdmin.firestore.Timestamp.now()
    };
}
function buildNewMessage(newMessageName, newPassword, userId, fromHandle, message) {
    return {
        name: newMessageName,
        password: newPassword,
        message_id: uuid_1.v4(),
        user_id: userId,
        message: message,
        from_handle: fromHandle,
        post_date: firebaseAdmin.firestore.Timestamp.now()
    };
}
function addDocUser(newUser, password, db) {
    let userObject = buildNewUser(newUser, password);
    const userDoc = db.collection("users").doc(userObject.name);
    return userDoc.set(userObject);
}
function addDocMessage(name, password, userId, fromHandle, message, db) {
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
        let userDoc = addDocUser("newUser1", "password1", db);
        await firebase.assertSucceeds(userDoc);
        userDoc = addDocUser("newUser2", "password2", db);
        await firebase.assertSucceeds(userDoc);
    });
    it("Add Message documents to Messages collection", async () => {
        const db = getAdminFirestore();
        // User sending message - added in previous test
        let userQuery = db.collection('users').doc("newUser1");
        await firebase.assertSucceeds(userQuery.get());
        let user = await userQuery.get();
        const data1 = user.data();
        assert_1.default.ok(data1 !== undefined && data1.name.length > 0);
        userQuery = db.collection('users').doc("newUser2"); // User receiving message
        await firebase.assertSucceeds(userQuery.get());
        user = await userQuery.get();
        const data2 = user.data();
        assert_1.default.ok(data2 !== undefined && data2.name.length > 0);
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
        const db = getAdminFirestore();
        let messageUser = "newUser2";
        let messageQuery = db.collection(`messages/${messageUser}/documents`);
        await firebase.assertSucceeds(messageQuery.get());
        let documents = await messageQuery.get();
        const data2 = documents.docs;
        assert_1.default.ok(data2 !== undefined && data2.length > 1);
        let previousName = null;
        data2.forEach((row) => {
            if (previousName === null) {
                previousName = row.get("name");
            }
            else {
                assert_1.default.strictEqual(previousName, row.get("name"));
            }
            const name = row.get("name");
            // do something with row...
        });
        messageUser = "newUser1";
        messageQuery = db.collection(`messages/${messageUser}/documents`);
        await firebase.assertSucceeds(messageQuery.get());
        documents = await messageQuery.get();
        const data1 = documents.docs;
        assert_1.default.ok(data1 !== undefined && data1.length > 1);
        previousName = null;
        data1.forEach((row) => {
            if (previousName === null) {
                previousName = row.get("name");
            }
            else {
                assert_1.default.strictEqual(previousName, row.get("name"));
            }
            const name = row.get("name");
        });
    });
    it("Delete message documents for User", async () => {
        const db = getAdminFirestore();
        let messageUser = "newUser2";
        let messageQuery = db.collection(`messages/${messageUser}/documents`);
        let documents = await messageQuery.get();
        assert_1.default.ok(documents.size > 1);
        const data2 = documents.docs;
        data2.forEach(async (row) => {
            row.ref.delete().catch(err => {
                throw err;
            });
        });
        setTimeout(async () => {
            messageQuery = db.collection(`messages/${messageUser}/documents`);
            documents = await messageQuery.get();
            assert_1.default.strictEqual(documents.size, 0);
        }, 25);
    });
    it("Ensure other user messages are not deleted", async () => {
        const db = getAdminFirestore();
        let messageUser = "newUser1";
        let messageQuery = db.collection(`messages/${messageUser}/documents`);
        let documents = await messageQuery.get();
        assert_1.default.ok(documents.size > 1);
    });
    it("Get all users from Firestore", async () => {
        const db = getAdminFirestore();
        const usersQuery = db.collection(`users`);
        const users = await usersQuery.get();
        assert_1.default.ok(users.size > 1);
        const data = users.docs;
        data.forEach((row) => {
            assert_1.default.ok(["newUser1", "newUser2"].indexOf(row.data().name) > -1);
        });
    });
    it("Get user from Firestore by name", async () => {
        const db = getAdminFirestore();
        const usersQuery = db.collection("users").where("name", "==", "newUser1");
        ;
        const users = await firebase.assertSucceeds(usersQuery.get());
        assert_1.default.ok(users.size === 1);
        const data = users.docs;
        const name = data[0].data().name;
        assert_1.default.strictEqual("newUser1", name);
    });
    it("Delete a user from Firestore", async () => {
        const db = getAdminFirestore();
        const usersQuery = db.collection("users").where("name", "==", "newUser1");
        let users = await firebase.assertSucceeds(usersQuery.get());
        assert_1.default.ok(users.size === 1);
        const data = users.docs;
        await firebase.assertSucceeds(data[0].ref.delete());
        users = await firebase.assertSucceeds(usersQuery.get());
        assert_1.default.ok(users.size === 0);
    });
    it("Update a user in Firestore", async () => {
        const db = getAdminFirestore();
        const usersQuery = db.collection("users").where("name", "==", "newUser2");
        let users = await firebase.assertSucceeds(usersQuery.get());
        assert_1.default.ok(users.size === 1);
        const data = users.docs;
        const timestamp = firebaseAdmin.firestore.Timestamp.now();
        await firebase.assertSucceeds(data[0].ref.update({ last_login: timestamp, ip: "localhost" }));
        users = await firebase.assertSucceeds(usersQuery.get());
        assert_1.default.strictEqual(users.docs[0].data().ip, "localhost");
        assert_1.default.ok(timestamp.isEqual(users.docs[0].data().last_login));
    });
});
after(async () => {
    // await firebase.clearFirestoreData({ projectId: DODEX_PROJECT_ID });
});
