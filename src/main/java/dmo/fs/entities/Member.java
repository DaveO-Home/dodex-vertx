package dmo.fs.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;

@Entity(name = "Member")
@Table(name = "member")

@Access(AccessType.FIELD)

public class Member implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private MemberId memberId;

  public MemberId getMemberId() {
    return memberId;
  }

  public void setMemberId(MemberId memberId) {
    this.memberId = memberId;
  }

  @Embeddable
  public static class MemberId implements Serializable {

    @Basic(optional = false)
    @Column(name = "GROUP_ID", nullable = false, updatable = false, insertable = false)
    private long group_id;

    @Basic(optional = false)
    @Column(name = "USER_ID", nullable = false, updatable = false)
    private long user_id;

    public long getGroup_id() {
      return group_id;
    }

    public long getUser_id() {
      return user_id;
    }

    public MemberId() {}

    @Override
    public int hashCode() {
      return (int)(group_id/user_id) * "&".hashCode() * "$".hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if(o == null) return false;
      if (o == this) return true;
      if (!(o instanceof MemberId)) return false;
      return false;
    }

    public MemberId(Long group_id, Long user_id) {
      this.group_id = group_id;
      this.user_id = user_id;
    }
  }
}
