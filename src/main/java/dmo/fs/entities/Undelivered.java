package dmo.fs.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;

@Entity(name = "Undelivered")
@Table(name = "undelivered")
@Access(AccessType.FIELD)
public class Undelivered implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private UndeliveredId undeliveredId;

  public UndeliveredId getUndeliveredId() {
    return undeliveredId;
  }

  public void setUndeliveredId(UndeliveredId undeliveredId) {
    this.undeliveredId = undeliveredId;
  }

  @Embeddable
  public static class UndeliveredId implements Serializable {

    @Basic(optional = false)
    @Column(name = "USER_ID", nullable = false, updatable = false)
    private long user_id;

    @Basic(optional = false)
    @Column(name = "MESSAGE_ID", nullable = false, updatable = false)
    private long message_id;

    public UndeliveredId() {}

    @Override
    public int hashCode() {
      return (int)(user_id/message_id) * "&".hashCode() * "$".hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if(o == null) return false;
      if (o == this) return true;
      if (!(o instanceof UndeliveredId)) return false;
      return false;
    }

    public UndeliveredId(Long user_id, Long message_id) {
      this.user_id = user_id;
      this.message_id = message_id;
    }
  }
}
