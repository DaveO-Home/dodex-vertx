package dmo.fs.db;

import java.sql.Timestamp;

// import java.sql.Timestamp;

public interface MessageUser {
    public void setId(Long id);
    public void setName(String name);
    public void setPassword(String password);
    public void setIp(String ip);
    public <T>void setLastLogin(T lastLogin);
    public Long getId();
    public String getName();
    public String getPassword();
    public String getIp();
    public Timestamp getLastLogin();
}