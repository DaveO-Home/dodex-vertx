package dmo.fs.spa.utils;

import java.sql.Timestamp;
import java.util.Map;

// import java.sql.Timestamp;

public interface SpaLogin {
    public void setId(Long id);
    public void setName(String name);
    public void setPassword(String password);
    public <T>void setLastLogin(T lastLogin);
    public void setStatus(String status);
    public Long getId();
    public String getName();
    public String getPassword();
    public Timestamp getLastLogin();
    public String getStatus();
    public Map<String, Object> getMap();
}