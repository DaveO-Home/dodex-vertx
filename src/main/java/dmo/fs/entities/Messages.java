package dmo.fs.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "Messages")
@Table(name = "messages")

public class Messages implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  @Id
  @Column(name = "ID", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SequenceGenerator(name="messagesSeq", sequenceName="messages_SEQ", allocationSize=50)
  private long id;

  @Basic(optional = false)
  @Column(name = "MESSAGE", nullable = false, columnDefinition = "mediumtext", length = 16777215)
  private String message;

  @Basic(optional = false)
  @Column(name = "FROM_HANDLE", nullable = false)
  private String from_handle;

  @Basic(optional = false)
  @Column(name = "POST_DATE", nullable = false)
  private LocalDateTime post_date;

  public Set<Users> getUsers() {
    return users;
  }

  public void setUsers(Set<Users> users) {
    this.users = users;
  }

  @ManyToMany(mappedBy = "messages")
  private Set<Users> users = new HashSet<>();

  @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
  @JoinColumn(name="MESSAGE_ID")
  private Set<Undelivered> undelivered;

  public long getId() {
    return id;
  }

  public void setUndelivered(Set<Undelivered> undelivered) {
    this.undelivered = undelivered;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getFromHandle() {
    return from_handle;
  }

  public void setFromHandle(String from_Handle) {
    this.from_handle = from_Handle;
  }

  public LocalDateTime getPostDate() {
    return post_date;
  }

  public void setPostDate(LocalDateTime post_date) {
    this.post_date = post_date;
  }

  public Set<Undelivered> getUndelivered() {
    return undelivered;
  }
}
