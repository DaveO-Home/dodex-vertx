package dmo.fs.utils;

import com.google.cloud.Timestamp;

public class FirebaseMessage {

    public FirebaseMessage() {
    }
    
    public FirebaseMessage(String name, String password, String message_id, String user_id, String message,
            String from_handle, Timestamp post_date) {
        this.name = name;
        this.password = password;
        this.message_id = message_id;
        this.user_id = user_id;
        this.message = message;
        this.from_handle = from_handle;
        this.post_date = post_date;
    }

    private String name;
    private String password;
    private String message_id;
    private String user_id;
    private String message;
    private String from_handle;
    private Timestamp post_date;

    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(final String password) {
        this.password = password;
    }
    public String getMessage_id() {
        return message_id;
    }
    public void setMessage_id(final String message_id) {
        this.message_id = message_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(final String user_id) {
        this.user_id = user_id;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(final String message) {
        this.message = message;
    }
    public String getFrom_handle() {
        return from_handle;
    }
    public void setFrom_handle(final String from_handle) {
        this.from_handle = from_handle;
    }
    public Timestamp getPost_date() {
        return post_date;
    }
    public void setPost_date(final Timestamp post_date) {
        this.post_date = post_date;
    }
    @Override
    public String toString() {
        return "FirebaseMessage [from_handle=" + from_handle + ", message=" + message + ", message_id=" + message_id
                + ", name=" + name + ", password=" + password + ", post_date=" + post_date + ", user_id=" + user_id
                + "]";
    }
}
