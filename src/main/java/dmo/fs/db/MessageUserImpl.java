package dmo.fs.db;

import jakarta.persistence.EntityManager;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

public class MessageUserImpl implements MessageUser {

  private Long id;
  private String name;
  private String password;
  private String ip;
  private Timestamp lastLogin;
  private String _id;
  private EntityManager entityManager;

  @Override
  public void setId(final Long id) {
    if (id != null) {
      this.id = id;
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
  public void setIp(final String ip) {
    this.ip = ip;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public EntityManager getEntityManager() {
    return entityManager;
  }

  @Override
  public void setEntityManager(EntityManager em) {
    this.entityManager = em;
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
  public <T> void setLastLogin(T lastLogin) {
    Optional<?> login = Optional.of(lastLogin);

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
    Optional<ZonedDateTime> loginZonedDateTime = login
        .filter(ZonedDateTime.class::isInstance)
        .map(ZonedDateTime.class::cast);

    if (loginTimestamp.isPresent()) {
      this.lastLogin = loginTimestamp.get();
    } else if (loginDate.isPresent()) {
      this.lastLogin = new Timestamp(loginDate.get().getTime());
    } else if (loginLong.isPresent()) {
      this.lastLogin = new Timestamp(loginLong.get());
    } else if (loginOffsetDateTime.isPresent()) {
      this.lastLogin = new Timestamp(loginOffsetDateTime.get().toInstant().toEpochMilli());
    } else if (loginLocalDate.isPresent()) {
      Date local = Date.from(loginLocalDate.get().atStartOfDay(ZoneId.systemDefault()).toInstant());
      this.lastLogin = new Timestamp(local.getTime());
    } else if (loginLocalDateTime.isPresent()) {
      Date local = Date.from(loginLocalDateTime.get().atZone(ZoneId.systemDefault()).toInstant());
      this.lastLogin = new Timestamp(local.getTime());
    } else if (loginZonedDateTime.isPresent()) {
      Date local = Date.from(loginZonedDateTime.get().toInstant());
      this.lastLogin = new Timestamp(local.getTime());
    }
  }

  @Override
  public Timestamp getLastLogin() {
    return lastLogin;
  }

  @Override
  public void set_id(String _id) {
    this._id = _id;
  }

  @Override
  public String get_id() {
    return _id;
  }

  @Override
  public String toString() {
    return String.format("ID: %s, NAME: %s, PASSWORD: %s, IP: %s, LAST_LOGIN: %s, _ID: %s", getId(), getName(), getPassword(), getIp(), getLastLogin() != null ? getLastLogin().toString() : getLastLogin(), get_id());
  }
}