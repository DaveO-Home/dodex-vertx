package dmo.fs.spa.utils;

import java.sql.Timestamp;
import java.util.Map;

// import java.sql.Timestamp;

public interface SpaLogin {
    <T> void setId(T id);
    void setName(String name);
    void setPassword(String password);
    <T>void setLastLogin(T lastLogin);
    void setStatus(String status);
    <T> T getId();
    String getName();
    String getPassword();
    Timestamp getLastLogin();
    String getStatus();
    Map<String, Object> getMap();
}