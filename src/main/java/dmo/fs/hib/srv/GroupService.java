package dmo.fs.hib.srv;

import dmo.fs.db.MessageUser;
import dmo.fs.entities.Group;
import dmo.fs.entities.Member;
import dmo.fs.entities.Users;
import dmo.fs.hib.DodexDatabaseBase;
import dmo.fs.hib.fac.DbConfiguration;
import dmo.fs.utils.DodexUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GroupService implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  protected final static Logger logger = LoggerFactory.getLogger(GroupService.class.getName());
  private final static DodexDatabaseBase dodexDatabase;

  static {
    try {
      dodexDatabase = DbConfiguration.getDefaultDb();
    } catch (InterruptedException | IOException | SQLException e) {
      throw new RuntimeException(e);
    }
  }

  static Boolean isCheckForOwner = true;

  public GroupService(Boolean isCheckForOwner) {
    GroupService.isCheckForOwner = isCheckForOwner;
  }

  public GroupService() {
  }

  private final static DodexUtil dodexUtil = new DodexUtil();

  protected static boolean qmark = true;
  protected final static DateTimeFormatter formatter =
      DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());
  ZoneId zoneId = ZoneId.of("Europe/London");

  protected Group getGroupByName(String name, EntityManager entityManager)
      throws NoResultException {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Group> groupQuery = builder.createQuery(Group.class);
    Root<Group> root = groupQuery.from(Group.class);

    groupQuery.select(root).where(builder.equal(root.get("name"), name));
    TypedQuery<Group> query = entityManager.createQuery(groupQuery);
    return query.getSingleResult();
  }

  protected Users getUserByName(String name, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> userQuery = builder.createQuery(Users.class);
    Root<Users> root = userQuery.from(Users.class);

    userQuery.select(root).where(builder.equal(root.get("name"), name));
    TypedQuery<Users> query = entityManager.createQuery(userQuery);
    return query.getSingleResult();
  }

  protected List<Users> getMembersByGroup(Group group, EntityManager entityManager) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Users> criteriaQuery = builder.createQuery(Users.class);
    CriteriaQuery<Group> rootQuery = builder.createQuery(Group.class);

    Root<Group> groupRoot = criteriaQuery.from(Group.class);
    Join<Group, Users> members = groupRoot.join("users");
    criteriaQuery.where(builder.equal(groupRoot.get("id"), group.getId()));

    return entityManager
        .createQuery(criteriaQuery.select(members))
        .getResultList();
  }

  public JsonObject addGroupAndMembers(JsonObject addGroupJson)
      throws IOException {
    if (dodexDatabase.getEntityManager() == null) {
      return addGroupJson;
    }
    EntityManager em = dodexDatabase.getEntityManager().getEntityManagerFactory().createEntityManager();

    final Map<String, String> selected = dodexUtil.commandMessage(addGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    addGroupJson = addGroup(addGroupJson, em);

    String entry0 = selectedUsers.getFirst();
    if (addGroupJson.getInteger("status") == 0 &&
        entry0 != null && !entry0.isEmpty()) {
      try {
        addGroupJson = addMembers(em, selectedUsers, addGroupJson);
      } catch (SQLException | InterruptedException | IOException err) {
        addGroupJson
            .put("status", -1)
            .put("errorMessage", err.getMessage());
        err.printStackTrace();
      }
    }

    em.close();
    return addGroupJson;
  }

  @Transactional
  protected JsonObject addGroup(JsonObject addGroupJson, EntityManager em)
      throws IOException {
    Timestamp current = new Timestamp(new Date().getTime());
    OffsetDateTime time = OffsetDateTime.now();
    LocalDateTime ldt = LocalDateTime.now();

    MessageUser messageUser = dodexDatabase.createMessageUser();
    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));

    MessageUser messageUserSelected = dodexDatabase.selectUser(messageUser);

    try {
      Group group = getGroupByName(addGroupJson.getString("groupName"), em);
      if (group.getId() != 0) {
        addGroupJson
            .put("ownerKey", messageUserSelected.getId())
            .put("id", group.getId())
            .put("status", 0)
            .put("errorMessage", "");
      }
    } catch (NoResultException nre) {
      Group newGroup = new Group();
      newGroup.setName(addGroupJson.getString("groupName"));
      newGroup.setCreated(ldt);
      newGroup.setUpdated(ldt);
      newGroup.setOwner(messageUserSelected.getId().intValue());

      em.getTransaction().begin();
      em.persist(newGroup);
      em.getTransaction().commit();

      ZoneOffset preferredOffset = zoneId.getRules().getOffset(ldt);
      ZonedDateTime zonedDateTime = ZonedDateTime.ofLocal(ldt, zoneId, preferredOffset);

      String openApiDate = zonedDateTime.format(formatter);

      addGroupJson
          .put("ownerKey", messageUserSelected.getId())
          .put("id", newGroup.getId())
          .put("status", 0)
          .put("created", openApiDate)
          .put("groupMessage", "Group added.")
          .put("errorMessage", "Group added.");
    }

    return addGroupJson;
  }

  @Transactional
  protected JsonObject addMembers(EntityManager em, List<String> selectedUsers, JsonObject addGroupJson)
      throws InterruptedException, SQLException, IOException {

    Group group = em.find(Group.class, addGroupJson.getInteger("id"));
    JsonObject checkedJson = checkOnGroupOwner(addGroupJson, em);

    if (checkedJson.getBoolean("isValidForOperation")) {
      List<String> allUsers = new ArrayList<>();
      allUsers.add(addGroupJson.getString("groupOwner"));
      allUsers.addAll(selectedUsers);

      List<String> newMembers = checkOnMembers(group, allUsers, em);

      List<Long> userIds = new ArrayList<>();
      for (String memberName : newMembers) {
        Users user = getUserByName(memberName, em);
        userIds.add(user.getId());
      }

      if (!userIds.isEmpty()) {
        em.getTransaction().begin();
        for (Long userId : userIds) {
          Member member = new Member();
          Member.MemberId memberId = new Member.MemberId(group.getId(), userId);
          member.setMemberId(memberId);
          em.persist(member);
        }
        em.getTransaction().commit();
      }
    } else {
      addGroupJson = checkedJson;
    }

    return addGroupJson;
  }

  public JsonObject deleteGroupOrMembers(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException, NoResultException {
    if (dodexDatabase.getEntityManager() == null) {
      return deleteGroupJson;
    }
    EntityManager em = dodexDatabase.getEntityManager().getEntityManagerFactory().createEntityManager();
    MessageUser messageUser = dodexDatabase.createMessageUser();

    Map<String, String> selected = dodexUtil.commandMessage(deleteGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    messageUser.setName(deleteGroupJson.getString("groupOwner"));
    messageUser.setPassword(deleteGroupJson.getString("ownerId"));

    String entry0 = selectedUsers.getFirst();

    if (deleteGroupJson.getInteger("status") == 0 && "".equals(entry0)) {
      try {

        deleteGroupJson = deleteGroup(deleteGroupJson, em);

      } catch (Exception err) {
        errData(err, deleteGroupJson);
      }
    } else if (deleteGroupJson.getInteger("status") == 0) {
      try {

        deleteGroupJson = deleteMembers(selectedUsers, deleteGroupJson, em);

      } catch (InterruptedException | SQLException | IOException err) {
        errData(err, deleteGroupJson);
      }
    }

    em.close();
    return deleteGroupJson;
  }

  @Transactional
  public JsonObject deleteGroup(JsonObject deleteGroupJson, EntityManager em) {
    JsonObject checkedJson = checkOnGroupOwner(deleteGroupJson, em);

    if (deleteGroupJson.getString("errorMessage") != null &&
        !deleteGroupJson.getString("errorMessage").startsWith("No result")) {
      if (checkedJson.getBoolean("isValidForOperation")) {
        Group group = getGroupByName(deleteGroupJson.getString("groupName"), em);
        int deletedMembers = group.getMembers().size();

        em.getTransaction().begin();
        em.remove(group);
        em.getTransaction().commit();

        String deleteMessage = null;
        if (deletedMembers > 0) {
          deleteMessage = deletedMembers + " members with group deleted";
        } else {
          deleteMessage = "Group " + group.getName() + " deleted.";
        }

        deleteGroupJson.put("errorMessage", deleteMessage);
      } else {
        deleteGroupJson
            .put("isValidForOperation", checkedJson.getBoolean("isValidForOperation"))
            .put("errorMessage", "Contact owner for group administration");
      }
    }
    return deleteGroupJson;
  }

  @Transactional
  protected JsonObject deleteMembers(List<String> selectedUsers, JsonObject deleteGroupJson, EntityManager em)
      throws InterruptedException, SQLException, IOException {
    JsonObject checkedJson = checkOnGroupOwner(deleteGroupJson, em);
    if (checkedJson.getBoolean("isValidForOperation")) {
      Group group = getGroupByName(deleteGroupJson.getString("groupName"), em);
      deleteGroupJson.put("id", group.getId());

      em.getTransaction().begin();
      em.clear();
      for (Member member : group.getMembers()) {
        Users user = em.find(Users.class, member.getMemberId().getUser_id());
        if (selectedUsers.contains(user.getName())) {
          if (user.getId() == member.getMemberId().getUser_id() &&
              group.getId() == member.getMemberId().getGroup_id()) {
            Member removeMember = em.find(Member.class, new Member.MemberId(group.getId(), user.getId()));
            em.remove(removeMember);
          }
        }
      }

      em.getTransaction().commit();

      deleteGroupJson
          .put("groupMessage", "")
          .put("errorMessage", "Group member(s) removed");
    } else {
      deleteGroupJson
          .put("isValidForOperation", checkedJson.getBoolean("isValidForOperation"))
          .put("errorMessage", "Contact owner for group administration");
    }

    return deleteGroupJson;
  }

  public JsonObject getMembersList(JsonObject getGroupJson)
      throws InterruptedException, SQLException, IOException {
    if (dodexDatabase.getEntityManager() == null) {
      return getGroupJson;
    }
    EntityManager em = dodexDatabase.getEntityManager().getEntityManagerFactory().createEntityManager();
    MessageUser messageUser = dodexDatabase.createMessageUser();

    messageUser.setName(getGroupJson.getString("groupOwner"));
    messageUser.setPassword(getGroupJson.getString("ownerId"));

    messageUser = dodexDatabase.selectUser(messageUser);

    getGroupJson.put("ownerKey", messageUser.getId());

    try {
      Group group = getGroupByName(getGroupJson.getString("groupName"), em);
      List<Users> users = getMembersByGroup(group, em);
      JsonObject userEntry = new JsonObject();
      JsonArray members = new JsonArray();

      if (!users.isEmpty()) {
        for (Users user : users) {
          if (!user.getName().equals(getGroupJson.getString("groupOwner"))) {
            members.add(new JsonObject().put("name", user.getName()));
          }
        }
        getGroupJson.put("groupMessage", "")
            .put("members", members.encode())
            .put("errorMessage", "")
            .put("id", group.getId());
      } else {
        getGroupJson
            .put("errorMessage", "Group not found: " + getGroupJson.getString("groupName"))
            .put("id", 0);
      }
    } catch (NoResultException nre) {
      getGroupJson
          .put("errorMessage", nre.getMessage())
          .put("groupMessage", "");
    }
    return getGroupJson;
  }

  protected JsonObject checkOnGroupOwner(JsonObject groupJson, EntityManager em) {
    Group group;
    try {
      group = getGroupByName(groupJson.getString("groupName"), em);
      Users user = getUserByName(groupJson.getString("groupOwner"), em);
      groupJson.put("isValidForOperation", false);

      if (group.getId() != 0) {
        groupJson.put("id", group.getId())
            .put("ownerKey", group.getOwner());
      }

      JsonObject checkGroupJson = new JsonObject()
          .put("checkGroupOwnerId", user.getId())
          .put("checkGroupOwner", user.getName())
          .put("checkForOwner", isCheckForOwner != null && isCheckForOwner)
          .put("isValidForOperation", groupJson.getInteger("status") != -1 &&
              !isCheckForOwner || user.getId() == groupJson.getInteger("ownerKey"));

      groupJson
          .put("isValidForOperation", checkGroupJson.getBoolean("isValidForOperation"))
          .put("errorMessage", !checkGroupJson.getBoolean("isValidForOperation") ?
              "Contact owner for group administration" : "");
    } catch (NoResultException nre) {
      groupJson.put("errorMessage", nre.getMessage());
      groupJson.put("id", 0);
    }

    return groupJson;
  }

  protected List<String> checkOnMembers(Group group, List<String> selectedList, EntityManager em) {
    List<String> newSelected = new ArrayList<>(selectedList);

    for (String userName : selectedList) {
      Set<Member> members = group.getMembers();
      if (members == null) {
        return selectedList;
      }
      members.forEach(member -> {
        Users user = getUserByName(userName, em);
        if (user.getId() == member.getMemberId().getUser_id()) {
          newSelected.remove(userName);
        }
      });
    }

    return newSelected;
  }

  protected JsonObject errData(Throwable err, JsonObject groupJson) {
    if (err != null && err.getMessage() != null) {
      groupJson
          .put("status", -1)
          .put("errorMessage", !err.getMessage().contains("batch execution") ?
              err.getMessage() : err.getMessage() + " -- some actions may have succeeded.");

      if (!err.getMessage().contains("batch execution")) {
        err.printStackTrace();
      } else {
        logger.error(err.getMessage());
      }
    }
    return groupJson;
  }
}
