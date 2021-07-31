package dmo.fs.utils;

import com.google.cloud.Timestamp;

public class FirebaseUser {
    private String id;
    private String name;
    private String password;
    private String ip;
    private Timestamp lastLogin;

    public FirebaseUser() {
    }

    public FirebaseUser(String id, String name, String password, String ip, Timestamp lastLogin) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.ip = ip;
        this.lastLogin = lastLogin;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

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

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(final Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "FirebaseUser [id=" + id + ", ip=" + ip + ", lastLogin=" + lastLogin + ", name=" + name + ", password="
                + password + "]";
    }
}
