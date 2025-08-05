package dmo.fs.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity(name = "Group")
@Table(name = "groups")
public class Group implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "groupsSeq", sequenceName = "groups_SEQ")
    private long id;

    @Basic(optional = false)
    @Column(name = "NAME", nullable = false)
    private String name;

    @Basic(optional = false)
    @Column(name = "OWNER", nullable = false)
    private Integer owner;

    @Basic(optional = false)
    @Column(name = "CREATED")
    private LocalDateTime created;

    @Basic(optional = false)
    @Column(name = "UPDATED")
    private LocalDateTime updated;

    @ManyToMany(mappedBy = "groups")
    private Set<Users> users = new HashSet<>();

    public Set<Users> getUsers() {
        return users;
    }

    public void setUsers(Set<Users> users) {
        this.users = users;
    }

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name="GROUP_ID", updatable = false, nullable = false, insertable = false)
    private Set<Member> members;

    public Set<Member> getMembers() {
        return members;
    }

    @Transient
    public Map<String, Object> getMap() {
        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("id", this.id);
        groupMap.put("name", this.name);
        groupMap.put("ownerId", this.owner);
        groupMap.put("created", this.created.toString());
        groupMap.put("updated", this.updated.toString());
        return groupMap;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    public Integer getOwner() {
        return owner;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

}

