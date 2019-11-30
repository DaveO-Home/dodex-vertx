package dmo.fs.db;

import java.sql.Timestamp;

public class MessageUserImpl implements MessageUser {

    public MessageUserImpl() {
    }
    private Long id;
    private String name;
    private String password;
    private String ip;
    private Timestamp lastLogin;

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public Timestamp getLastLogin() {
        return lastLogin;
    }
}