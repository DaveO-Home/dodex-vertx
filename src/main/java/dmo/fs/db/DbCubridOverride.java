package dmo.fs.db;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dmo.fs.utils.ColorUtilConstants;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.core.http.ServerWebSocket;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;

public abstract class DbCubridOverride extends DbDefinitionBase {
  private static Logger logger = LoggerFactory.getLogger(DbCubridOverride.class.getName());

  @Override
  public Future<MessageUser> addUser(ServerWebSocket ws, MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<MessageUser> promise = Promise.promise();
    Timestamp current = new Timestamp(new Date().getTime());
    Object lastLogin = current;

    pool.rxGetConnection().doOnSuccess(conn -> {
      Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword(),
          messageUser.getIp(), lastLogin);
      // SqlConnection conn = c.result();
      String sql = getInsertUser();

      conn.preparedQuery(sql).rxExecute(parameters).doOnSuccess(rows -> {
        messageUser.setId(0L);
        for (Row row : rows) {
          messageUser.setId(row.getLong(0));
        }

        // messageUser.setId(rows.property(JDBCPool.GENERATED_KEYS).getLong(0));
        messageUser.setLastLogin(current);

        conn.close();
        promise.tryComplete(messageUser);
      }).doOnError(err -> {
        logger.error(String.format("%sError adding user: %s%s", ColorUtilConstants.RED, err,
            ColorUtilConstants.RESET));
      }).subscribe(rows -> {
        //
      }, err -> {
        logger.error(String.format("%sError Adding user: %s%s", ColorUtilConstants.RED, err,
            ColorUtilConstants.RESET));
        err.printStackTrace();
      });
    }).subscribe();

    return promise.future();
  }

  @Override
  public Future<Long> deleteUser(ServerWebSocket ws, MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<Long> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> {
      // SqlConnection conn = c.result();
      String query =
          create.query(getDeleteUser(), messageUser.getName(), messageUser.getPassword())
              .toString();

      conn.query(query).rxExecute().doOnSuccess(rows -> {
        Long id = 0L;
        for (Row row : rows) {
          id = row.getLong(0);
        }
        Long count = Long.valueOf(Integer.toString(rows.rowCount()));
        messageUser.setId(id > 0L ? id : count);
        conn.close();
        promise.complete(count);
      }).doOnError(err -> {
        logger.error(String.format("%sError deleting user: %s%s", ColorUtilConstants.RED,
            err, ColorUtilConstants.RESET));
        ws.writeTextMessage(err.toString());
        conn.close();
      }).subscribe(rows -> {
        //
      }, Throwable::printStackTrace);
    }).subscribe();
    return promise.future();
  }

  @Override
  public Future<Long> getUserIdByName(String name) throws InterruptedException {
    Promise<Long> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> {
      // SqlConnection conn = c.result();
      String query = create.query(getUserByName(), name).toString();
      conn.query(query).rxExecute().doOnSuccess(rows -> {
        // if (ar.succeeded()) {
        // RowSet<Row> rows = ar.result();
        Long id = 0L;
        for (Row row : rows) {
          id = row.getLong(0);
        }
        conn.close();
        promise.complete(id);
        // } else {

        // }
      }).doOnError(err -> {
        logger.error(String.format("%sError finding user by name: %s - %s%s",
            ColorUtilConstants.RED, name, err.getCause().getMessage(),
            ColorUtilConstants.RESET));
        conn.close();
      }).subscribe();
    }).subscribe();
    return promise.future();
  }

  @Override
  public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
      throws InterruptedException, SQLException {
    MessageUser resultUser = createMessageUser();
    Promise<MessageUser> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> {
      conn.query(create.query(getUserById(), messageUser.getName(), messageUser.getPassword())
          .toString()).rxExecute().doOnSuccess(rows -> {
        // if (ar.succeeded()) {
        Future<Integer> future1 = null;
        // RowSet<Row> rows = ar.result();

        if (rows.size() == 0) {
          try {
            Future<MessageUser> future2 = addUser(ws, messageUser);

            future2.onComplete(handler -> {
              MessageUser result = future2.result();
              resultUser.setId(result.getId());
              resultUser.setName(result.getName());
              resultUser.setPassword(result.getPassword());
              resultUser.setIp(result.getIp());
              resultUser.setLastLogin(
                  result.getLastLogin() == null ? new Date().getTime()
                      : result.getLastLogin());
              promise.complete(resultUser);
            });
          } catch (InterruptedException | SQLException e) {
            e.printStackTrace();
          } catch (Exception ex) {
            logger.error(String.format("%s%s%s", ColorUtilConstants.RED,
                ex.getCause().getMessage(), ColorUtilConstants.RESET));
          }
        } else {
          for (Row row : rows) {
            resultUser.setId(row.getLong(0));
            resultUser.setName(row.getString(1));
            resultUser.setPassword(row.getString(2));
            resultUser.setIp(row.getString(3));
            resultUser.setLastLogin(row.getOffsetTime("LAST_LOGIN"));
          }
        }

        if (rows.size() > 0) {
          try {
            conn.close();
            future1 = updateUser(ws, resultUser);
            promise.complete(resultUser);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        if (rows.size() > 0) {
          future1.onComplete(v -> {
            //
          });
        }
      }).doOnError(err -> {
        logger.error(String.format("%sError selecting user: %s%s",
            ColorUtilConstants.RED, err.getCause().getMessage(),
            ColorUtilConstants.RESET));
        conn.close();
      }).subscribe();
    }).subscribe();
    return promise.future();
  }

  @Override
  public Future<StringBuilder> buildUsersJson(MessageUser messageUser)
      throws InterruptedException, SQLException {
    Promise<StringBuilder> promise = Promise.promise();

    pool.rxGetConnection().doOnSuccess(conn -> {
      conn.query(create.query(getAllUsers(), messageUser.getName()).toString()).rxExecute()
          .doOnSuccess(rows -> {
            JsonArray ja = new JsonArray();

            for (Row row : rows) {
              ja.add(new JsonObject().put("name", row.getString(1)));
            }
            conn.close();
            promise.complete(new StringBuilder(ja.toString()));
          }).doOnError(err -> {
            logger.error(String.format("%sError build user json: %s%s",
                ColorUtilConstants.RED, err.getCause().getMessage(),
                ColorUtilConstants.RESET));
            conn.close();
          }).subscribe();
    }).subscribe();
    return promise.future();
  }

  @Override
  public Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws,
                                                          MessageUser messageUser) throws Exception {
    RemoveUndelivered removeUndelivered = new RemoveUndeliveredCubrid();
    RemoveMessage removeMessage = new RemoveMessageCubrid();
    CompletePromise completePromise = new CompletePromise();

    removeUndelivered.setUserId(messageUser.getId());

    /*
     * Get all undelivered messages for current user
     */
    Future<Void> future = Future.future(promise -> {
      completePromise.setPromise(promise);
      pool.rxGetConnection().flatMapCompletable(conn -> conn.rxBegin()
          .flatMapCompletable(tx -> conn.query(
                  create.query(getUserUndelivered(), messageUser.getId()).toString())
              .rxExecute().doOnSuccess(rows -> {
                for (Row row : rows) {
                  OffsetDateTime postDate = null;

                  postDate = row.getOffsetDateTime("POST_DATE");

                  long epochMilli = postDate.toInstant().toEpochMilli();
                  Date date = new Date(epochMilli);

                  DateFormat formatDate = DateFormat.getDateInstance(
                      DateFormat.DEFAULT, Locale.getDefault());

                  String handle = row.getString(4);
                  String message = row.getString(2);

                  // Send messages back to client
                  ws.writeTextMessage(
                      handle + formatDate.format(date) + " " + message);
                  removeUndelivered.getMessageIds().add(row.getLong(1));
                  removeMessage.getMessageIds().add(row.getLong(1));
                }
              }).doOnError(err -> {
                logger.info(String.format("%sRetriveing Messages Error: %s%s",
                    ColorUtilConstants.RED, err.getMessage(),
                    ColorUtilConstants.RESET));
                err.printStackTrace();
              }).flatMapCompletable(res -> tx.rxCommit().doFinally(completePromise)
                  .doOnSubscribe(onSubscribe -> {
                    tx.completion().doOnError(err -> {
                      // if (x.failed()) {
                      tx.rollback();
                      logger.error(String.format(
                          "%sMessages Transaction Error: %s%s",
                          ColorUtilConstants.RED, err.getCause(),
                          ColorUtilConstants.RESET));
                      // }
                      conn.close();
                    }).subscribe();
                  })))
          .doOnError(err -> {
            logger.info(String.format("%sDatabase for Messages Error: %s%s",
                ColorUtilConstants.RED, err.getMessage(),
                ColorUtilConstants.RESET));
            err.printStackTrace();
            conn.close();
          })).subscribe();
    });

    future.compose(v -> Future.<Void>future(promise -> {
      removeUndelivered.setPromise(promise);
      try {
        removeUndelivered.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (removeUndelivered.getCount() > 0) {
        logger.info(String.join(ColorUtilConstants.BLUE_BOLD_BRIGHT,
            Integer.toString(removeUndelivered.getCount()), " Messages Delivered",
            " to ", messageUser.getName(), ColorUtilConstants.RESET));
      }
    })).compose(v -> Future.<Void>future(promise -> {
      removeMessage.setPromise(promise);
      try {
        removeMessage.run();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }));

    return removeUndelivered.getPromise2().future();
  }

  class RemoveUndeliveredCubrid extends RemoveUndelivered {
    @Override
    public void run() throws Exception {
      completePromise.setPromise(promise);

      for (Long messageId : messageIds) {
        pool.rxGetConnection().doOnSuccess(conn -> {
          String query =
              create.query(getRemoveUndelivered(), userId, messageId).toString();

          conn.query(query).rxExecute().doOnSuccess(rows -> {
            for (Row row : rows) {
              logger.info(row.toJson().toString());
            }
            count += rows.rowCount() == 0 ? 1 : rows.rowCount();

            if (messageIds.size() == count) {
              try {
                conn.close();
                completePromise.run();
              } catch (Exception e) {
                e.printStackTrace();
              }
              counts.put("messages", count);
              promise2.complete(counts);
            }
          }).doOnError(err -> {
            logger.error(String.format("Deleting Undelivered: %s",
                err.getCause().getMessage()));
            conn.close();
          }).subscribe();
        }).subscribe();
      }
    }
  }

  class RemoveMessageCubrid extends RemoveMessage {
    @Override
    public void run() throws Exception {
      completePromise.setPromise(promise);

      for (Long messageId : messageIds) {
        pool.rxGetConnection().doOnSuccess(conn -> {
          String sql = null;

          sql = create.query(getRemoveMessage(), messageId, messageId).toString();

          conn.query(sql).rxExecute().doOnSuccess(rows -> {
            count += rows.rowCount() == 0 ? 1 : rows.rowCount();
            if (messageIds.size() == count) {
              try {
                conn.close();
                completePromise.run();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }).doOnError(err -> {
            logger.error(
                String.format("%sDeleting Message: %s%s", ColorUtilConstants.RED,
                    err.getCause().getMessage(), ColorUtilConstants.RESET));
          }).subscribe();
        }).subscribe();
      }
    }
  }

  @Override
  public Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message)
      throws InterruptedException, SQLException {
    Promise<Long> promise = Promise.promise();

    Timestamp current = new Timestamp(new Date().getTime());

    Object postDate = current;

    pool.rxGetConnection().doOnSuccess(conn -> {
      String query = create.query(getAddMessage(), message, messageUser.getName(), postDate)
          .toString();
      conn.query(query).rxExecute().doOnSuccess(rows -> {
        String query2 =
            create.query(getMessageIdByHandleDate(), messageUser.getName(), postDate)
                .toString();
        conn.query(query2).rxExecute().doOnSuccess(msg -> {
          Long id = 0L;
          for (Row row : msg) {
            id = row.getLong(0);
          }
          conn.close();
          promise.complete(id);
        }).subscribe();
      }).doOnError(err -> {
        logger.error(String.format("%sError adding messaage: %s%s", ColorUtilConstants.RED,
            err, ColorUtilConstants.RESET));
        err.printStackTrace();
        ws.writeTextMessage(err.toString());
        conn.close();
      }).subscribe(rows -> {
        //
      }, err -> {
        if (err != null && err.getMessage() != null) {
          err.printStackTrace();
        }
      });
    }).doOnError(Throwable::printStackTrace).subscribe();

    return promise.future();
  }
}
