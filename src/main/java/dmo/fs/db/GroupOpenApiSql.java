package dmo.fs.db;

import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.SingleHelper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.jdbcclient.JDBCPool;
import io.vertx.rxjava3.mysqlclient.MySQLClient;
import io.vertx.rxjava3.sqlclient.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.jooq.impl.DSL.*;

public class GroupOpenApiSql implements GroupOpenApi {
  private final static Logger logger = LoggerFactory.getLogger(GroupOpenApiSql.class.getName());

  private static String GETGROUPBYNAME;
  private static String GETADDGROUP;
  private static String GETMARIAADDGROUP;
  private static String GETADDMEMBER;
  private static String GETDELETEGROUP;
  private static String GETDELETEGROUPBYID;
  private static String GETDELETEMEMBERS;
  private static String GETDELETEMEMBER;
  private static String GETMEMBERSBYGROUP;
  private static String GETUSERBYNAMESQLITE;
  private static DSLContext create;
  private static Pool pool;
  private static boolean qmark = true;
  private final static DateTimeFormatter formatter =
      DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault());

  public static void buildSql() {
    GETGROUPBYNAME = qmark ? setupGroupByName().replaceAll("\\$\\d", "?") : setupGroupByName();
    GETADDGROUP = qmark ? setupAddGroup().replaceAll("\\$\\d", "?") : setupAddGroup();
    GETMARIAADDGROUP =
        qmark ? setupMariaAddGroup().replaceAll("\\$\\d", "?") : setupMariaAddGroup();
    GETADDMEMBER = qmark ? setupAddMember().replaceAll("\\$\\d", "?") : setupAddMember();
    GETDELETEGROUP = qmark ? setupDeleteGroup().replaceAll("\\$\\d", "?") : setupDeleteGroup();
    GETDELETEGROUPBYID = qmark ? setupDeleteGroupById().replaceAll("\\$\\d", "?") : setupDeleteGroupById();
    GETDELETEMEMBERS = qmark ? setupDeleteMembers().replaceAll("\\$\\d", "?") : setupDeleteMembers();
    GETDELETEMEMBER = qmark ? setupDeleteMember().replaceAll("\\$\\d", "?") : setupDeleteMember();
    GETMEMBERSBYGROUP = qmark ? setupMembersByGroup().replaceAll("\\$\\d", "?") : setupMembersByGroup();
    GETUSERBYNAMESQLITE = qmark ? setupUsersByNameSqlite().replaceAll("\\$\\d", "?") : setupUsersByNameSqlite();
  }

  private static String setupGroupByName() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
            .from(table("groups")).where(field("NAME").eq("$")));
  }

  private String getGroupByName() {
    return GETGROUPBYNAME;
  }

  private static String setupAddGroup() {
    return create.renderNamedParams(insertInto(table("groups"))
        .columns(field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
        .values("$", "$", "$", "$").returning(field("ID")));
  }

  private String getAddGroup() {
    return GETADDGROUP;
  }

  private static String setupMariaAddGroup() {
    return create.renderNamedParams(insertInto(table("groups"))
        .columns(field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
        .values("$", "$", "$", "$"));
  }

  private String getMariaAddGroup() {
    return GETMARIAADDGROUP;
  }

  private static String setupAddMember() {
    return create.renderNamedParams(
        insertInto(table("member")).columns(field("GROUP_ID"), field("USER_ID")).values("$", "$"));
  }

  private String getAddMember() {
    return GETADDMEMBER;
  }

  private static String setupDeleteGroup() {
    if (DbConfiguration.isUsingMariadb()) {
      return create.renderNamedParams(deleteFrom(table("groups")).where(field("NAME").eq("$1")));
    } else {
      return create.renderNamedParams(deleteFrom(table("groups")).where(field("NAME").eq("$1")).returning(field("ID")));
    }
  }

  private String getDeleteGroup() {
    return GETDELETEGROUP;
  }

  private static String setupDeleteGroupById() {
    if (DbConfiguration.isUsingMariadb()) {
      return create.renderNamedParams(deleteFrom(table("groups")).where(field("ID").eq("$1")));
    } else {
      return create.renderNamedParams(deleteFrom(table("groups")).where(field("ID").eq("$1")).returning(field("ID")));
    }
  }

  private String getDeleteGroupById() {
    return GETDELETEGROUPBYID;
  }

  private static String setupDeleteMembers() {
    if (DbConfiguration.isUsingMariadb()) {
      return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1")));
    } else {
      return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1"))); // .returning(field("ID")));
    }
  }

  private String getDeleteMembers() {
    return GETDELETEMEMBERS;
  }

  private static String setupDeleteMember() {
    if (DbConfiguration.isUsingMariadb()) {
      return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1").and(field("USER_ID").eq("$2"))));
    } else {
      return create.renderNamedParams(deleteFrom(table("member")).where(field("GROUP_ID").eq("$1").and(field("USER_ID").eq("$2")))); // .returning(field("ID")));
    }
  }

  private String getDeleteMember() {
    return GETDELETEMEMBER;
  }

  private static String setupMembersByGroup() {
    return create.renderNamedParams(select(field("USER_ID"), field("users.NAME"), field("GROUP_ID"))
        .from(table("groups")).join(table("member"))
        .on(field("groups.ID").eq(field("GROUP_ID")).and(field("groups.NAME").eq("$")))
        .join(table("users")).on(field("users.ID").eq(field("USER_ID")))
        .whereExists(select(field("ID"))
            .from(table("users")).join(table("member"))
            .on(field("users.ID").eq(field("USER_ID")).and(field("users.NAME").eq("$")))
        ));
  }

  private String getMembersByGroup() {
    return GETMEMBERSBYGROUP;
  }

  private static String setup() {
    return create.renderNamedParams(
        select(field("ID"), field("NAME"), field("OWNER"), field("CREATED"), field("UPDATED"))
            .from(table("groups")).where(field("NAME").eq("$")));
  }

  private static String setupUsersByNameSqlite() {
    return create.render(select(field("*")).from(table("users").where(field("NAME").in("$"))));
  }

  private String getupUsersByNameSqlite() {
    return GETUSERBYNAMESQLITE;
  }

  public Future<JsonObject> addGroupAndMembers(JsonObject addGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();
    DodexUtil dodexUtil = new DodexUtil();
    Map<String, String> selected = dodexUtil.commandMessage(addGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));
    String ownerKey = addGroupJson.getString("ownerKey");

    if (ownerKey != null) {
      messageUser.setId(Long.valueOf(ownerKey));
    }

    addGroup(addGroupJson).onSuccess(groupJson -> {
      String entry0 = selectedUsers.get(0);

      if (groupJson.getInteger("status") == 0 &&
          entry0 != null && !"".equals(entry0)) {
        try {
          addMembers(selectedUsers, groupJson).onSuccess(promise::complete).onFailure(err -> {
            logger.error("Add group/member err: " + err.getMessage());
            addGroupJson.put("status", -1);
            addGroupJson.put("errorMessage", err.getMessage());
            promise.complete(addGroupJson);
          });
        } catch (InterruptedException | SQLException | IOException err) {
          err.printStackTrace();
          addGroupJson.put("status", -1);
          addGroupJson.put("errorMessage", err.getMessage());
        }
      } else {
        promise.complete(addGroupJson);
      }
    }).onFailure(err -> {
      logger.error("Add group/member err: " + err.getMessage());
      promise.complete(addGroupJson);
    });

    return promise.future();
  }

  private Future<JsonObject> addGroup(JsonObject addGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    Timestamp current = new Timestamp(new Date().getTime());
    OffsetDateTime time = OffsetDateTime.now();
    Object currentDate = DbConfiguration.isUsingPostgres() ? time : current;

    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();
    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));

    dodexDatabase.selectUser(messageUser, null).onSuccess(userData -> {
      addGroupJson.put("ownerKey", userData.getId());

      pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getGroupByName())
          .rxExecute(Tuple.of(addGroupJson.getString("groupName"))).doOnSuccess(rows -> {
            if (rows.size() == 1) {
              Row row = rows.iterator().next();
              addGroupJson.put("id", row.getInteger(0));
            }
          })
          .doAfterSuccess(result -> {
            if (addGroupJson.getInteger("id") == null) {
              Tuple parameters = Tuple.of(addGroupJson.getString("groupName"),
                  addGroupJson.getInteger("ownerKey"), currentDate, currentDate);
              String sql = getAddGroup();
              if (DbConfiguration.isUsingSqlite3()) {
                sql = sql + " RETURNING id";
              }
              if (DbConfiguration.isUsingMariadb()) {
                sql = getMariaAddGroup();
              }
              conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
                for (Row row : rows) {
                  addGroupJson.put("id", row.getLong(0));
                }
                if (DbConfiguration.isUsingMariadb()) {
                  addGroupJson.put("id", rows.property(MySQLClient.LAST_INSERTED_ID));
                } else if (DbConfiguration.isUsingCubrid()) {
                  addGroupJson.put("id", rows.property(JDBCPool.GENERATED_KEYS).getLong(0));
                }

                LocalDate localDate = LocalDate.now();
                LocalTime localTime = LocalTime.of(LocalTime.now().getHour(),
                    LocalTime.now().getMinute(), LocalTime.now().getSecond());
                ZonedDateTime zonedDateTime =
                    ZonedDateTime.of(localDate, localTime, ZoneId.systemDefault());
                String openApiDate = zonedDateTime.format(formatter);

                addGroupJson.put("created", openApiDate);
                addGroupJson.put("status", 0);
                conn.close();
                promise.complete(addGroupJson);
              }).subscribe(rows -> {
              }, err -> {
                logger.error(String.format("%sError Adding group: %s%s", ColorUtilConstants.RED,
                    err, ColorUtilConstants.RESET));
                errData(err, promise, addGroupJson);
                if (err != null && err.getMessage() != null) {
                  conn.close();
                }
                if (addGroupJson.getInteger("id") == null) {
                  addGroupJson.put("id", -1);
                }
              });
            } else {
              promise.complete(addGroupJson);
            }
          }).subscribe()).subscribe();
    });
    return promise.future();
  }

  private Future<JsonObject> addMembers(List<String> selectedUsers, JsonObject addGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();

    checkOnGroupOwner(addGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        List<String> allUsers = new ArrayList<>();

        allUsers.add(addGroupJson.getString("groupOwner"));
        allUsers.addAll(selectedUsers);

        checkOnMembers(allUsers, addGroupJson).onSuccess(newUsers -> {
          if (!newUsers.isEmpty()) {
            pool.getConnection().doOnSuccess(connection -> {
              boolean addOwner = false;
              List<Tuple> userList = new ArrayList<>();
              StringBuilder sql = new StringBuilder();
              StringBuilder stringBuilder = new StringBuilder();

              for (String name : newUsers) {
                if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                  stringBuilder.append("'").append(name).append("',");
                } else {
                  userList.add(Tuple.of(name));
                }
              }

              // Sqlite3(jdbc client) 'select' does not work well with executeBatch
              Single<RowSet<Row>> query;
              if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                stringBuilder.append("' '");
                if (DbConfiguration.isUsingH2() && getupUsersByNameSqlite().contains("cast(?")) {
                  sql.append(getupUsersByNameSqlite().replace("cast(? as varchar)", stringBuilder.toString()));
                } else {
                  sql.append(getupUsersByNameSqlite().replace("?", stringBuilder.toString()));
                }
                query = connection.preparedQuery(sql.toString()).execute();
              } else {
                sql.append(dodexDatabase.getUserByName());
                stringBuilder.append("' '");
                query = connection.preparedQuery(sql.toString()).executeBatch(userList);
              }

              query.doOnSuccess(result -> {
                List<Tuple> list = new ArrayList<>();
                for (RowSet<Row> rows = result; rows != null; rows = rows.next()) {
                  if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                    for (Row row : rows) {
                      list.add(Tuple.of(addGroupJson.getInteger("id"), row.getInteger(0)));
                    }
                  } else {
                    if (rows.iterator().hasNext()) {
                      Row row = rows.iterator().next();
                      list.add(Tuple.of(addGroupJson.getInteger("id"), row.getInteger(0)));
                    }
                  }
                }

                connection.rxBegin().doOnSuccess(tx -> connection
                    .preparedQuery(getAddMember()).executeBatch(list)
                    .doOnSuccess(res -> {
                      int rows = 0;
                      for (RowSet<Row> s = res; s != null; s = s.next()) {
                        if (s.rowCount() != 0) {
                          rows += s.rowCount();
                        }
                      }
                      addGroupJson.put("errorMessage", "Members Added: " + rows);
                      tx.rxCommit().doFinally(() ->
                          connection.rxClose().doFinally(() -> promise.complete(addGroupJson)).subscribe()
                      ).subscribe();

                    }).subscribe(v -> {
                    }, err -> {
                      errData(err, promise, addGroupJson);
                      if (err != null && err.getMessage() != null) {
                        // committing because some of the batch inserts may have succeeded
                        tx.rxCommit().doFinally(() ->
                            connection.rxClose().subscribe()
                        ).subscribe();
                      }
                    })).subscribe(v -> {
                }, err -> {
                  errData(err, promise, addGroupJson);
                  if (err != null && err.getMessage() != null) {
                    connection.close();
                  }
                });
              }).subscribe(result -> {
                List<Tuple> list = new ArrayList<>();
                for (RowSet<Row> rows = result; rows != null; rows = rows.next()) {
                  if (rows.iterator().hasNext()) {
                    Row row = rows.iterator().next();
                    list.add(Tuple.of(addGroupJson.getInteger("id"), row.getInteger(0)));
                  }
                }
              }, err -> {
                errData(err, promise, addGroupJson);
                if (err != null && err.getMessage() != null) {
                  connection.close();
                }
              });
            }).subscribe(v -> {
            }, Throwable::printStackTrace);
          } else {
            addGroupJson.put("errorMessage", "Some member(s) already added");
            promise.complete(addGroupJson);
          }
        }).onFailure(Throwable::printStackTrace);
      } else {
        addGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(addGroupJson);
      }
    }).onFailure(Throwable::printStackTrace);

    return promise.future();
  }

  public Future<JsonObject> deleteGroupOrMembers(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();
    DodexUtil dodexUtil = new DodexUtil();
    Map<String, String> selected = dodexUtil.commandMessage(deleteGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    messageUser.setName(deleteGroupJson.getString("groupOwner"));
    messageUser.setPassword(deleteGroupJson.getString("ownerId"));
    String ownerKey = deleteGroupJson.getString("ownerKey");

    if (ownerKey != null) {
      messageUser.setId(Long.valueOf(ownerKey));
    }

    String entry0 = selectedUsers.get(0);

    if (deleteGroupJson.getInteger("status") == 0 &&
        "".equals(entry0)) {
      try {
        deleteGroup(deleteGroupJson)
            .onSuccess(deleteGroupObject -> promise.complete(deleteGroupJson))
            .onFailure(err -> errData(err, promise, deleteGroupJson));
      } catch (InterruptedException | SQLException | IOException err) {
        errData(err, promise, deleteGroupJson);
      }
    } else if (deleteGroupJson.getInteger("status") == 0) {
      try {
        deleteMembers(selectedUsers, deleteGroupJson)
            .onSuccess(promise::complete)
            .onFailure(err -> {
              errData(err, promise, deleteGroupJson);
            });
      } catch (InterruptedException | SQLException | IOException err) {
        errData(err, promise, deleteGroupJson);
      }
    } else {
      promise.complete(deleteGroupJson);
    }

    return promise.future();
  }

  public Future<JsonObject> deleteGroup(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    checkOnGroupOwner(deleteGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        pool.rxGetConnection().doOnSuccess(connection -> connection.preparedQuery(getGroupByName())
            .rxExecute(Tuple.of(deleteGroupJson.getString("groupName")))
            .doOnSuccess(rows -> {
              Integer id = 0;
              for (Row row : rows) {
                id = row.getInteger(0);
              }

              deleteGroupJson.put("id", id);
              Tuple parameters = Tuple.of(id);

              connection.rxBegin()
                  .doOnSuccess(tx -> connection
                      .preparedQuery(getDeleteMembers())
                      .rxExecute(parameters)
                      .doOnSuccess(r -> {
                        int deletedMembers = 0;
                        for (RowSet<Row> s = r; s != null; s = s.next()) {
                          if (s.rowCount() != 0) {
                            deletedMembers += s.rowCount();
                          }
                        }
                        if (deletedMembers > 0) {
                          deleteGroupJson.put("errorMessage", deletedMembers + " members with ");
                        } else {
                          parameters.clear();
                          parameters.addInteger(deleteGroupJson.getInteger("id"));
                          deleteGroupJson.put("errorMessage", deleteGroupJson.getString("groupName") + " ");
                        }
                        connection
                            .preparedQuery(getDeleteGroupById())
                            .rxExecute(parameters)
                            .doFinally(() -> {
                              deleteGroupJson.put("status", 0);
                              deleteGroupJson.put("errorMessage", deleteGroupJson.getString("errorMessage") + "group deleted");
                              tx.rxCommit().doFinally(() ->
                                  connection.rxClose().doFinally(() -> promise.complete(deleteGroupJson)).subscribe()
                              ).subscribe();
                            }).subscribe(v -> {
                            }, err -> {
                              errData(err, promise, deleteGroupJson);
                              if (err != null && err.getMessage() != null) {
                                connection.close();
                              }
                            });
                      }).doOnError(err -> {
                        errData(err, promise, deleteGroupJson);
                      }).subscribe())
                  .subscribe(v -> {
                  }, err -> {
                    errData(err, promise, deleteGroupJson);
                    if (err != null && err.getMessage() != null) {
                      connection.close();
                    }
                  });
            }).subscribe(v -> {
            }, err -> {
              errData(err, promise, deleteGroupJson);
            })
        ).subscribe(v -> {
        }, err -> {
          errData(err, promise, deleteGroupJson);
        });
      } else {
        deleteGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(deleteGroupJson);
      }
    });
    return promise.future();
  }

  private Future<JsonObject> deleteMembers(List<String> selectedUsers, JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    checkOnGroupOwner(deleteGroupJson).onSuccess(checkedJson -> {
      if (checkedJson.getBoolean("isValidForOperation")) {
        pool.rxGetConnection()
            .doOnSuccess(connection -> connection.preparedQuery(getGroupByName())
                .rxExecute(Tuple.of(deleteGroupJson.getString("groupName")))
                .doOnSuccess(rows -> {
                  Integer id = 0;
                  for (Row row : rows) {
                    id = row.getInteger(0);
                  }
                  deleteGroupJson.put("id", id);
                  connection.rxBegin().doOnSuccess(tx -> { //connection
                    StringBuilder sql = new StringBuilder();
                    StringBuilder stringBuilder = new StringBuilder();

                    List<Tuple> userList = new ArrayList<>();
                    for (String name : selectedUsers) {
                      if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                        stringBuilder.append("'").append(name).append("',");
                      } else {
                        userList.add(Tuple.of(name));
                      }
                    }

                    Single<RowSet<Row>> query;
                    if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                      stringBuilder.append("''");
                      if (DbConfiguration.isUsingH2() && getupUsersByNameSqlite().contains("cast(?")) {
                        sql.append(getupUsersByNameSqlite().replace("cast(? as varchar)", stringBuilder.toString()));
                      } else {
                        sql.append(getupUsersByNameSqlite().replace("?", stringBuilder.toString()));
                      }
                      query = connection.preparedQuery(sql.toString()).execute();
                    } else {
                      sql.append(dodexDatabase.getUserByName());
                      query = connection.preparedQuery(sql.toString()).executeBatch(userList);
                    }

                    query.doOnSuccess(result -> {
                      // Sqlite3(jdbc client) 'select' does not work well with executeBatch
                      List<Tuple> list = new ArrayList<>();
                      for (RowSet<Row> rows2 = result; rows2 != null; rows2 = rows2.next()) {
                        if (DbConfiguration.isUsingSqlite3() || DbConfiguration.isUsingH2()) {
                          for (Row row : rows2) {
                            list.add(Tuple.of(deleteGroupJson.getInteger("id"), row.getInteger(0)));
                          }
                        } else {
                          if (rows2.iterator().hasNext()) {
                            Row row = rows2.iterator().next();
                            list.add(Tuple.of(deleteGroupJson.getInteger("id"), row.getInteger(0)));
                          }
                        }
                      }
                      connection.preparedQuery(getDeleteMember()).executeBatch(list)
                          .doOnSuccess(res -> {
                            int rows3 = 0;
                            for (RowSet<Row> s = res; s != null; s = s.next()) {
                              if (s.rowCount() != 0) {
                                rows3 += s.rowCount();
                              }
                            }
                            deleteGroupJson.put("errorMessage", "Members Deleted: " + rows3);
                            tx.rxCommit().doFinally(() ->
                                connection.rxClose().doFinally(() -> promise.complete(deleteGroupJson)).subscribe()
                            ).subscribe();

                          }).subscribe(v -> {
                          }, err -> {
                            errData(err, promise, deleteGroupJson);
                            if (err != null && err.getMessage() != null) {
                              // committing because some of the batch deletes may have succeeded
                              tx.rxCommit().doFinally(() ->
                                  connection.rxClose().subscribe()
                              ).subscribe();
                            }
                          });

                    }).subscribe(v -> {
                    }, Throwable::printStackTrace);
                  }).subscribe(v -> {
                  }, Throwable::printStackTrace);
                }).subscribe(v -> {
                }, Throwable::printStackTrace))
            .subscribe(v -> {
            }, Throwable::printStackTrace);
      } else {
        deleteGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(deleteGroupJson);
      }
    });

    return promise.future();
  }

  public Future<JsonObject> getMembersList(JsonObject getGroupJson)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexDatabase.createMessageUser();

    messageUser.setName(getGroupJson.getString("groupOwner"));
    messageUser.setPassword(getGroupJson.getString("ownerId"));

    try {
      dodexDatabase.selectUser(messageUser, null).onSuccess(userData -> {

        JsonArray members = new JsonArray();
        getGroupJson.put("ownerKey", userData.getId());
        Tuple parameters = Tuple.of(getGroupJson.getString("groupName"),
            getGroupJson.getString("groupOwner"));

        pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getMembersByGroup())
            .rxExecute(parameters).doOnSuccess(rows -> {
              if (rows.size() > 0) {
                for (Row row : rows) {
                  if (!row.getString(1).equals(getGroupJson.getString("groupOwner"))) {
                    members.add(new JsonObject().put("name", row.getString(1)));
                  } else {
                    getGroupJson.put("id", row.getInteger(2));
                  }
                }
                getGroupJson.put("groupMessage", "")
                    .put("status", 0)
                    .put("members", members.encode());
                promise.complete(getGroupJson);
              } else {
                getGroupJson.put("errorMessage", "Group not found: " + getGroupJson.getString("groupName"));
                getGroupJson.put("id", 0);
                promise.complete(getGroupJson);
              }
            }).subscribe()).subscribe();
      });
    } catch (InterruptedException | SQLException e) {
      throw new RuntimeException(e);
    }


    return promise.future();
  }

  private Future<JsonObject> checkOnGroupOwner(JsonObject groupJson)
      throws SQLException, IOException, InterruptedException {
    Promise<JsonObject> waitFor = Promise.promise();
    DodexDatabase dodexDatabase = DbConfiguration.getDefaultDb();

    pool.rxGetConnection().doOnSuccess(conn -> conn.preparedQuery(getGroupByName())
            .rxExecute(Tuple.of(groupJson.getString("groupName")))
            .doOnSuccess(rows -> {
              if (rows.size() == 1) {
                Row row = rows.iterator().next();
                groupJson.put("id", row.getInteger(0));
                groupJson.put("groupOwnerId", row.getInteger(2));
              }

              conn.preparedQuery(dodexDatabase.getUserByName())
                  .rxExecute(Tuple.of(groupJson.getString("groupOwner")))
                  .doOnSuccess(rows2 -> {
                    if (rows2.size() == 1) {
                      Row row = rows2.iterator().next();
                      groupJson.put("checkGroupOwnerId", row.getInteger(0));
                      groupJson.put("checkGroupOwner", row.getString(1));
                    }
                    conn.close();
                  })
                  .doOnError(err -> {
                    errData(err, waitFor, groupJson);
                    conn.close();
                  })
                  .doFinally(() -> {
                    JsonObject config = Vertx.currentContext().config();
                    boolean isCheckForOwner =
                        config.getBoolean("dodex.groups.checkForOwner") != null &&
                            config.getBoolean("dodex.groups.checkForOwner");
                    groupJson.put("checkForOwner", isCheckForOwner);
                    groupJson.put("isValidForOperation", groupJson.getInteger("status") != -1 &&
                        !isCheckForOwner || groupJson.getInteger("checkGroupOwnerId") == groupJson.getInteger("groupOwnerId"));
                    if (!groupJson.getBoolean("isValidForOperation")) {
                      groupJson.put("errorMessage", "Contact owner for group administration");
                    }
                    waitFor.complete(groupJson);
                  }).subscribe();
            })
            .doOnError(err -> {
              errData(err, waitFor, groupJson);
              conn.close();
            }).subscribe())
        .doOnError(err -> {
          errData(err, waitFor, groupJson);
        }).subscribe();

    return waitFor.future();
  }

  protected Future<List<String>> checkOnMembers(List<String> selectedList, JsonObject addGroupJson) {
    Promise<List<String>> waitFor = Promise.promise();
    List<String> newSelected = new ArrayList<>();
    Single<SqlConnection> connResult = pool.rxGetConnection();

    for (String user : selectedList) {
      Single.just(user).subscribe(SingleHelper.toObserver(userName -> {
        Tuple parameters = Tuple.of(addGroupJson.getString("groupName"), userName.result());

        connResult.flatMap(conn -> {
              conn.preparedQuery(getMembersByGroup()).rxExecute(parameters)
                  .doOnSuccess(rows -> {
                    if (rows.size() == 0) {
                      newSelected.add(userName.result());
                    }
                    if (userName.result().equals(selectedList.get(selectedList.size() - 1))) {
                      waitFor.complete(newSelected);
                      conn.close();
                    }
                  }).doOnError(Throwable::printStackTrace)
                  .subscribe();
              return Single.just(conn);
            }).doOnError(Throwable::printStackTrace)
            .subscribe();
      }));
    }

    return waitFor.future();
  }

  private void errData(Throwable err, Promise<JsonObject> promise, JsonObject groupJson) {
    if (err != null && err.getMessage() != null) {
      if (!err.getMessage().contains("batch execution")) {
        err.printStackTrace();
        groupJson.put("errorMessage", err.getMessage());
      } else {
        groupJson.put("errorMessage", err.getMessage() + " -- some actions may have succeeded.");
        logger.error(err.getMessage());
      }
      groupJson.put("status", -1);
      promise.tryComplete(groupJson);
    }
  }

  public static void setCreate(DSLContext create) {
    GroupOpenApiSql.create = create;
  }

  public static void setPool(Pool pool) {
    GroupOpenApiSql.pool = pool;
  }

  public static void setQmark(boolean qmark) {
    GroupOpenApiSql.qmark = qmark;
  }

}
