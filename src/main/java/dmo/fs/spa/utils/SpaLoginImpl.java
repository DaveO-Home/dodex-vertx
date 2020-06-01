package dmo.fs.spa.utils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpaLoginImpl implements SpaLogin {

    private Long id;
    private String name;
    private String password;
    private Timestamp lastLogin;
    private String status;

    @Override
    public void setId(final Long id) {
        if (id instanceof Long) {
            this.id = id;
        } else {
            this.id = Long.parseLong(id.toString());
        }
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setPassword(final String password) {
        this.password = password;
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
    public <T> void setLastLogin(T lastLogin) {
        Optional<?> login = Optional.of(lastLogin);
        Optional<Timestamp> loginTimestamp = login.filter(Timestamp.class::isInstance).map(Timestamp.class::cast);
        if (loginTimestamp.isPresent()) {
            this.lastLogin = loginTimestamp.get();
        } else {
            Optional<Date> loginDate = login.filter(Date.class::isInstance).map(Date.class::cast);
            if (loginDate.isPresent()) {
                this.lastLogin = new Timestamp(loginDate.get().getTime());
            }
        }
    }

    @Override
    public Timestamp getLastLogin() {
        return lastLogin;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public Map<String, Object> getMap() {
        Map<String, Object> map = new ConcurrentHashMap<String, Object>();
        map.put("id", getId());
        map.put("name", getName());
        map.put("password", getPassword());
        map.put("lastlogin", getLastLogin());
        map.put("status", getStatus());

        return map;
    }
}