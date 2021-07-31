package dmo.fs.spa.utils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpaLoginImpl implements SpaLogin {

    private Long id;
    private String idS;
    private String name;
    private String password;
    private Timestamp lastLogin;
    private String status;

    @Override
    public <T> void setId(T id) {
        if (id instanceof Long) {
            this.id = (Long)id;
        } else {
            this.idS = id.toString();
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getId() {
        if(idS == null) {
            return (T) id;
        }
        return (T) idS;
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
        if(Boolean.valueOf(login.isPresent()).equals(true)) {
            Optional<Timestamp> loginTimestamp = login
                .filter(Timestamp.class::isInstance)
                .map(Timestamp.class::cast);
            Optional<Date> loginDate = login
                .filter(Date.class::isInstance)
                .map(Date.class::cast);
            Optional<Long> loginLong = login
                .filter(Long.class::isInstance)
                .map(Long.class::cast);
            Optional<OffsetDateTime> loginOffsetDateTime = login
                .filter(OffsetDateTime.class::isInstance)
                .map(OffsetDateTime.class::cast);
            Optional<LocalDate> loginLocalDate = login
                .filter(LocalDate.class::isInstance)
                .map(LocalDate.class::cast);
            Optional<LocalDateTime> loginLocalDateTime = login
                .filter(LocalDateTime.class::isInstance)
                .map(LocalDateTime.class::cast);


            if(loginTimestamp.isPresent()) {
                this.lastLogin = loginTimestamp.get();
            } else if(loginDate.isPresent()) {
                this.lastLogin = new Timestamp(loginDate.get().getTime());
            } else if(loginLong.isPresent()) {
                this.lastLogin = new Timestamp(loginLong.get());
            } else if(loginOffsetDateTime.isPresent()) {
                this.lastLogin = new Timestamp(loginOffsetDateTime.get().toInstant().toEpochMilli());
            } else if(loginLocalDate.isPresent()) {
                Date local = Date.from(loginLocalDate.get().atStartOfDay(ZoneId.systemDefault()).toInstant());
                this.lastLogin = new Timestamp(local.getTime());
            } else if(loginLocalDateTime.isPresent()) {
                Date local = Date.from(loginLocalDateTime.get().atZone(ZoneId.systemDefault()).toInstant());
                this.lastLogin = new Timestamp(local.getTime());
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

    @Override
    public String toString() {
        return String.format("ID: %s, NAME: %s, PASSWORD: %s, LAST_LOGIN: %s, STATUS: %s", getId(), getName(), getPassword(), getLastLogin() != null? getLastLogin().toString(): getLastLogin(), getStatus());
    }
}