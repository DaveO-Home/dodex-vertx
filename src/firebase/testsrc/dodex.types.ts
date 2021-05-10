import { firestore } from "firebase-admin";

type AdminAuth = {
  uid: string;
  email: string;
  role: string;
}

interface User  {
  name: string;
  password: string;
  user_id: string;
  ip: string;
  last_login: firestore.Timestamp;
};

interface Message  {
  name: string;
  password: string;
  message_id: string;
  user_id: string;
  message: string;
  from_handle: string;
  post_date: firestore.Timestamp;
};

interface Login {
  name: string;
  password: string;
  login_id: string;
  last_login: firestore.Timestamp;
};

export { AdminAuth, User, Message, Login };
