package dmo.fs.db.mongodb;

import dmo.fs.db.DbConfiguration;
import dmo.fs.db.GroupOpenApi;
import dmo.fs.db.MessageUser;
import dmo.fs.utils.ColorUtilConstants;
import dmo.fs.utils.DodexUtil;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class GroupOpenApiMongo implements GroupOpenApi {
  private final static Logger logger = LoggerFactory.getLogger(GroupOpenApiMongo.class.getName());

  @Override
  public Future<JsonObject> addGroupAndMembers(JsonObject addGroupJson) throws InterruptedException, SQLException, IOException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());

    Promise<JsonObject> promise = Promise.promise();
    DodexMongo dodexDatabase = DbConfiguration.getDefaultDb();
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

    try {
      addGroup(addGroupJson, mongoClient).onSuccess(groupJson -> {
        String entry0 = selectedUsers.get(0);
        if (groupJson.getInteger("status") == 0 &&
            entry0 != null && !"".equals(entry0)) {
          try {
            addMembers(selectedUsers, groupJson, mongoClient).onSuccess(promise::complete).onFailure(err -> {
              logger.error("Add group/member err: " + err.getMessage());
              addGroupJson.put("status", -1);
              addGroupJson.put("errorMessage", err.getMessage());
              mongoClient.close().doOnComplete(() -> promise.complete(groupJson)).subscribe();
            });
          } catch (InterruptedException | SQLException | IOException err) {
            err.printStackTrace();
            addGroupJson.put("status", -1);
            addGroupJson.put("errorMessage", err.getMessage());
          }
        } else {
          mongoClient.close().doOnComplete(() -> promise.complete(groupJson)).subscribe();
        }
      }).onFailure(err -> {
        logger.error("Add group/member err: " + err.getMessage());
        mongoClient.close().doOnComplete(() -> promise.complete(addGroupJson)).subscribe();
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }

    return promise.future();
  }

  @Override
  public Future<JsonObject> deleteGroupOrMembers(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    Promise<JsonObject> promise = Promise.promise();
    DodexUtil dodexUtil = new DodexUtil();
    Map<String, String> selected = dodexUtil.commandMessage(deleteGroupJson.getString("groupMessage"));
    final List<String> selectedUsers = Arrays.asList(selected.get("selectedUsers").split(","));

    String entry0 = selectedUsers.getFirst();

    if (deleteGroupJson.getInteger("status") == 0 &&
        "".equals(entry0)) {
      deleteGroup(deleteGroupJson, mongoClient)
          .onSuccess(deleteGroupObject -> promise.complete(deleteGroupJson))
          .onFailure(err -> errData(err, promise, deleteGroupJson))
          .onComplete(v -> mongoClient.close().subscribe());
    } else if (deleteGroupJson.getInteger("status") == 0) {
      deleteMembers(selectedUsers, deleteGroupJson, mongoClient)
          .onSuccess(promise::complete)
          .onFailure(err -> errData(err, promise, deleteGroupJson))
          .onComplete(v -> mongoClient.close().subscribe());
    } else {
      mongoClient.close().subscribe();
      promise.complete(deleteGroupJson);
    }

    return promise.future();
  }

  private Future<JsonObject> deleteGroup(JsonObject deleteGroupJson, MongoClient mongoClient) throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();
    deleteGroupJson.put("id", 0);
    checkOnGroupOwner(deleteGroupJson, mongoClient).onSuccess(checkedJson -> {

      if (checkedJson.getBoolean("isValidForOperation")) {
        JsonObject deleteGroup = new JsonObject();
        deleteGroup.put("name", new JsonObject().put("$eq", deleteGroupJson.getString("groupName")));
        Maybe<MongoClientDeleteResult> maybe = mongoClient.removeDocument("group_member", deleteGroup);
        maybe.subscribe(grp -> {
          logger.info("Removed Group(count): {} -- {}", deleteGroupJson.getString("groupName"), grp.getRemovedCount());
          deleteGroupJson.put("status", 0)
              .put("errorMessage", deleteGroupJson.getString("groupName") + " group deleted");
          promise.complete(deleteGroupJson);
        }, err -> {
          errData(err, promise, deleteGroupJson);
        });
      } else {
        promise.complete(deleteGroupJson);
      }
    }).onFailure(err -> errData(err, promise, deleteGroupJson));
    return promise.future();
  }

  private Future<JsonObject> deleteMembers(
      List<String> selectedUsers, JsonObject deleteGroupJson, MongoClient mongoClient) {
    Promise<JsonObject> promise = Promise.promise();

    checkOnGroupOwner(deleteGroupJson, mongoClient).onSuccess(checkedJson -> {
      deleteGroupJson.put("id", 0);
      if (checkedJson.getBoolean("isValidForOperation")) {
        JsonObject groupQuery = new JsonObject()
            .put("name", new JsonObject().put("$eq", deleteGroupJson.getString("groupName")));

        mongoClient.findOne("group_member", groupQuery, new JsonObject()).doOnSuccess(groupObject -> {
          JsonArray members = groupObject.getJsonArray("members");

          JsonArray removeMembers = new JsonArray();
          Cancellable cancel = Multi.createFrom().items(selectedUsers.toArray())
              .onItem().transform(item -> {
                members.getList().remove(item);
                return item;
              })
              .onFailure().recoverWithCompletion()
              .subscribe().with(member -> {
                if (selectedUsers.getLast().equals(member)) {
                  JsonObject updateObject = new JsonObject();
                  updateObject.put("$set", new JsonObject().put("members", members));
                  Maybe<MongoClientUpdateResult> membersUpdated =
                      mongoClient.updateCollection("group_member", groupQuery, updateObject);
                  Disposable update = membersUpdated.doOnSuccess(result -> {
                        getMembersList(deleteGroupJson).onSuccess(membersList -> {
                          deleteGroupJson.put("status", 0)
                              .put("groupMessage", "")
                              .put("errorMessage", "Members deleted: " + selectedUsers.size())
                              .put("members", membersList.getString("members"));
                          promise.complete(deleteGroupJson);
                        }).onFailure(err -> {
                          err.printStackTrace();
                          deleteGroupJson.put("status", -1)
                              .put("errorMessages", "Member list failed")
                              .put("groupMessage", "");
                          promise.complete(deleteGroupJson);
                        });
                      }).doOnError(err -> errData(err, promise, deleteGroupJson))
                      .subscribe(v -> {
                      }, err -> errData(err, promise, deleteGroupJson));
                }
              });
          if (members.getList().isEmpty()) {
            cancel.cancel();
            deleteGroupJson.put("status", 0)
                .put("errorMessage", "Members not found")
                .put("groupMessage", "");
            promise.complete(deleteGroupJson);
          }
        }).subscribe();
      } else {
        promise.complete(deleteGroupJson);
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getMembersList(JsonObject getGroupJson) throws InterruptedException, SQLException, IOException {
    MongoClient mongoClient = MongoClient.createShared(DodexUtil.getVertx(), new JsonObject());
    Promise<JsonObject> promise = Promise.promise();
    DodexMongo dodexMongo = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexMongo.createMessageUser();

    messageUser.setName(getGroupJson.getString("groupOwner"));
    messageUser.setPassword(getGroupJson.getString("ownerId"));

    getGroupJson.put("id", 0);
    try {
      dodexMongo.selectUser(messageUser).future().onItem().invoke(userData -> {
        getGroupJson.put("ownerKey", userData.get_id());

        JsonObject groupQuery = new JsonObject()
            .put("name", new JsonObject().put("$eq", getGroupJson.getString("groupName")));

        mongoClient.findOne("group_member", groupQuery, new JsonObject()).doOnSuccess(groupObject -> {
          JsonArray members = groupObject.getJsonArray("members");
          members.getList().remove(getGroupJson.getString("groupOwner"));

          JsonArray returnMembers = new JsonArray();
          Cancellable cancel = Multi.createFrom().items(members.getList().toArray())
              .onItem().transform(item -> {
                JsonObject entry = new JsonObject().put("name", item);
                returnMembers.add(entry);
                return item;
              })
              .onFailure().recoverWithCompletion()
              .subscribe().with(member -> {
                if (members.getList().getLast().equals(member)) {
                  getGroupJson.put("status", 0)
                      .put("groupMessage", "")
                      .put("members", returnMembers.encode());
                  promise.complete(getGroupJson);
                }
              });
          if (members.getList().isEmpty()) {
            cancel.cancel();
            getGroupJson.put("status", 0)
                .put("errorMessage", "Members not found")
                .put("groupMessage", "");
            promise.complete(getGroupJson);
          }

        }).subscribe(v -> {
        }, err -> {
          errData(err, promise, getGroupJson);
          mongoClient.close().subscribe();
        });

      }).subscribeAsCompletionStage();
    } catch (InterruptedException | SQLException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    return promise.future();
  }

  private Future<JsonObject> addGroup(JsonObject addGroupJson, MongoClient mongoClient)
      throws InterruptedException, SQLException, IOException, ExecutionException {
    Promise<JsonObject> promise = Promise.promise();
    String mongoDate = DbMongoBase.getMongoDate();

    DodexMongo dodexMongo = DbConfiguration.getDefaultDb();
    MessageUser messageUser = dodexMongo.createMessageUser();
    messageUser.setName(addGroupJson.getString("groupOwner"));
    messageUser.setPassword(addGroupJson.getString("ownerId"));

    dodexMongo.selectUser(messageUser).future().onItem().invoke(userData -> {
      addGroupJson.put("ownerKey", userData.get_id());

      mongoClient.find("group_member", new JsonObject().put("name", new JsonObject()
              .put("$eq", addGroupJson.getString("groupName"))))
          .doOnSuccess(group -> {
            if (group.size() == 1) {
              addGroupJson.put("_id", group.getFirst().getJsonObject("_id").getString("$oid"));
            }
          })
          .doAfterSuccess(result -> {
            if (addGroupJson.getString("_id") == null) {
              JsonObject insertGroup = new JsonObject()
                  .put("name", addGroupJson.getString("groupName"))
                  .put("owner", addGroupJson.getString("ownerKey"))
                  .put("created", mongoDate)
                  .put("members", new JsonArray())
                  .put("updated", mongoDate);

              mongoClient.insert("group_member", insertGroup).doOnSuccess(_id -> {

                addGroupJson.put("created", mongoDate)
                    .put("status", 0)
                    .put("id", 0)
                    .put("_id", _id)
                    .put("errorMessage", addGroupJson.getString("groupName") + " group added");

                promise.complete(addGroupJson);

              }).subscribe(rows -> {
              }, err -> {
                logger.error(String.format("%sError Adding group: %s%s", ColorUtilConstants.RED,
                    err, ColorUtilConstants.RESET));
                if (addGroupJson.getInteger("id") == null) {
                  addGroupJson.put("id", -1);
                }
                errData(err, promise, addGroupJson);
              });
            } else {
              promise.complete(addGroupJson);
            }
          }).subscribe();
    }).subscribeAsCompletionStage();
    return promise.future();
  }

  private Future<JsonObject> addMembers(List<String> selectedUsers, JsonObject addGroupJson, MongoClient mongoClient)
      throws InterruptedException, SQLException, IOException {
    Promise<JsonObject> promise = Promise.promise();

    checkOnGroupOwner(addGroupJson, mongoClient).onSuccess(checkedJson -> {
      addGroupJson.put("id", 0);
      if (checkedJson.getBoolean("isValidForOperation")) {
        List<String> allUsers = new ArrayList<>();
        allUsers.add(addGroupJson.getString("groupOwner"));
        allUsers.addAll(selectedUsers);

        checkOnMembers(allUsers, addGroupJson, mongoClient).onSuccess(newUsers -> {
          if (!newUsers.isEmpty()) {
            JsonObject groupQuery = new JsonObject()
                .put("name", new JsonObject().put("$eq", addGroupJson.getString("groupName")));

            mongoClient.findOne("group_member", groupQuery, new JsonObject()).doOnSuccess(groupObject -> {
              if (!groupObject.isEmpty()) {
                JsonArray members = groupObject.getJsonArray("members");

                for (String member : newUsers) {
                  members.add(member);
                }
                groupObject.put("members", members);
                String mongoDate = DbMongoBase.getMongoDate();
                groupObject.put("updated", mongoDate);

                mongoClient.save("group_member", groupObject)
                    .doFinally(() -> {
                      if (addGroupJson.getString("errorMessage") == null) {
                        addGroupJson.put("errorMessage", "Member(s) added")
                            .put("status", 0);
                      }
                      promise.complete(addGroupJson);
                    })
                    .subscribe(v -> {
                    }, err -> {
                      err.printStackTrace();
                      addGroupJson.put("status", -1)
                          .put("errorMessage", err.getMessage());
                      promise.complete(addGroupJson);
                    });
              }
            }).subscribe();
          } else {
            addGroupJson.put("errorMessage", "Some member(s) already added");
            promise.complete(addGroupJson);
          }
        }).onFailure(Throwable::printStackTrace);
      } else {
        addGroupJson.put("errorMessage", checkedJson.getString("errorMessage"));
        promise.complete(addGroupJson);
      }
    });
    return promise.future();
  }

  private Future<JsonObject> checkOnGroupOwner(JsonObject groupJson, MongoClient mongoClient) {
    Promise<JsonObject> waitFor = Promise.promise();

    JsonObject groupQuery = new JsonObject()
        .put("name", new JsonObject().put("$eq", groupJson.getString("groupName")));

    mongoClient.findOne("group_member", groupQuery, new JsonObject()).doOnSuccess(groupObject -> {
          if (!groupObject.isEmpty()) {
            groupJson.put("_id", groupObject.getJsonObject("_id").getString("$oid"))
                .put("groupOwnerId", groupObject.getString("owner"));
          }
          JsonObject userQuery = new JsonObject()
              .put("name", new JsonObject().put("$eq", groupJson.getString("groupOwner")));

          mongoClient.findOne("user_message", userQuery, new JsonObject())
              .doOnSuccess(userObject -> {
                if (!userObject.isEmpty()) {
                  groupJson.put("checkGroupOwnerId", userObject.getJsonObject("_id").getString("$oid"))
                      .put("checkGroupOwner", userObject.getString("name"));
                }
              })
              .doOnError(err -> errData(err, waitFor, groupJson))
              .doFinally(() -> {
                JsonObject config = Vertx.currentContext().config();

                boolean isCheckForOwner =
                    config.getBoolean("dodex.groups.checkForOwner") != null &&
                        config.getBoolean("dodex.groups.checkForOwner");

                groupJson.put("checkForOwner", isCheckForOwner)
                    .put("isValidForOperation", groupJson.getInteger("status") != -1 &&
                        !isCheckForOwner || Objects.equals(
                        groupJson.getString("checkGroupOwnerId"), groupJson.getString("groupOwnerId")));
                if (!groupJson.getBoolean("isValidForOperation")) {
                  groupJson.put("errorMessage", "Contact owner for group administration");
                }
                waitFor.complete(groupJson);
              }).subscribe(v -> {
              }, err -> {
                errData(err, waitFor, groupJson);
                mongoClient.close().subscribe();
              });
        })
        .subscribe(v -> {
        }, err -> {
          errData(err, waitFor, groupJson);
          mongoClient.close().subscribe();
        });

    return waitFor.future();
  }

  protected Future<List<String>> checkOnMembers(
      List<String> selectedList, JsonObject addGroupJson, MongoClient mongoClient) {
    Promise<List<String>> waitFor = Promise.promise();
    List<String> newSelected = new ArrayList<>();

    JsonObject groupQuery = new JsonObject();
    groupQuery.put("name", new JsonObject().put("$eq", addGroupJson.getString("groupName")));

    mongoClient.findOne("group_member", groupQuery, new JsonObject()).doOnSuccess(group -> {
      JsonArray membersArray = group.getJsonArray("members");
      List<String> members = membersArray.getList();
      for (String user : selectedList) {
        if (!members.contains(user)) {
          newSelected.add(user);
        }
      }
      waitFor.complete(newSelected);
    }).subscribe(v -> {
    }, err -> {
      addGroupJson.put("status", -1)
          .put("errorMessage", err.getMessage());
      mongoClient.close().subscribe();
      waitFor.complete(selectedList);
    });

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

  @Override
  public Future<JsonObject> deleteGroup(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException {
    return null;
  }
}
