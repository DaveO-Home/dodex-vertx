package dmo.fs.db;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

public class MessageUserImpl implements MessageUser {

    public MessageUserImpl() {
    }
    private Long id;
    private String name;
    private String password;
    private String ip;
    private Timestamp lastLogin;

    @Override
    public void setId(final Long id) {
        if (id instanceof Long) {
            this.id = id;
        } else
            this.id = Long.parseLong(id.toString());
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
    public void setIp(final String ip) {
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
    public <T>void setLastLogin(T lastLogin) {
        Optional<?> login = Optional.of(lastLogin);
        Optional<Timestamp> loginTimestamp = login
            .filter(Timestamp.class::isInstance)
            .map(Timestamp.class::cast);
        if(loginTimestamp.isPresent()) {
            this.lastLogin = loginTimestamp.get();
        }
        else {
            Optional<Date> loginDate = login
                .filter(Date.class::isInstance)
                .map(Date.class::cast);
            if(loginDate.isPresent()) {
                this.lastLogin = new Timestamp((loginDate.get()).getTime());
            }
        }
    }

    @Override
    public Timestamp getLastLogin() {
        return  lastLogin;
    }
}