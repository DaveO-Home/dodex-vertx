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
import io.vertx.reactivex.core.http.ServerWebSocket;
import io.vertx.reactivex.jdbcclient.JDBCPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.reactivex.sqlclient.Tuple;

public abstract class DbCubridOverride extends DbDefinitionBase {
    private static Logger logger = LoggerFactory.getLogger(DbCubridOverride.class.getName());

    @Override
    public Future<MessageUser> addUser(ServerWebSocket ws, MessageUser messageUser)
            throws InterruptedException, SQLException {
        Promise<MessageUser> promise = Promise.promise();
        Timestamp current = new Timestamp(new Date().getTime());
        Object lastLogin = current;

        pool.getConnection(c -> {
            Tuple parameters = Tuple.of(messageUser.getName(), messageUser.getPassword(), messageUser.getIp(),
                    lastLogin);
            SqlConnection conn = c.result();
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
        });

        return promise.future();
    }

    @Override
    public Future<Long> deleteUser(ServerWebSocket ws, MessageUser messageUser)
            throws InterruptedException, SQLException {
        Promise<Long> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();
            String query = create.query(getDeleteUser(), messageUser.getName(), messageUser.getPassword()).toString();

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
                logger.error(String.format("%sError deleting user: %s%s", ColorUtilConstants.RED, err,
                        ColorUtilConstants.RESET));
                ws.writeTextMessage(err.toString());
                conn.close();
            }).subscribe(rows -> {
                //
            }, Throwable::printStackTrace);
        });
        return promise.future();
    }

    @Override
    public Future<Long> getUserIdByName(String name) throws InterruptedException {
        Promise<Long> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();
            String query = create.query(getUserByName(), name).toString();
            conn.query(query).execute(ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rows = ar.result();
                    Long id = 0L;
                    for (Row row : rows) {
                        id = row.getLong(0);
                    }
                    conn.close();
                    promise.complete(id);
                } else {
                    logger.error(String.format("%sError finding user by name: %s - %s%s", ColorUtilConstants.RED, name,
                            ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });
        return promise.future();
    }

    @Override
    public Future<MessageUser> selectUser(MessageUser messageUser, ServerWebSocket ws)
            throws InterruptedException, SQLException {
        MessageUser resultUser = createMessageUser();
        Promise<MessageUser> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            conn.query(create.query(getUserById(), messageUser.getName(), messageUser.getPassword()).toString())
                .execute(ar -> {
                    if (ar.succeeded()) {
                        Future<Integer> future1 = null;
                        RowSet<Row> rows = ar.result();

                        if (rows.size() == 0) {
                            try {
                                Future<MessageUser> future2 = addUser(ws, messageUser);

                                future2.onComplete(handler -> {
                                    MessageUser result = future2.result();
                                    resultUser.setId(result.getId());
                                    resultUser.setName(result.getName());
                                    resultUser.setPassword(result.getPassword());
                                    resultUser.setIp(result.getIp());
                                    resultUser.setLastLogin(result.getLastLogin() == null ? new Date().getTime()
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
                                // logger.info(String.format("%sLogin Time Changed: %s%s",
                                // ColorUtilConstants.BLUE,
                                // resultUser.getName(), ColorUtilConstants.RESET));
                            });
                        }
                    } else {
                        logger.error(String.format("%sError selecting user: %s%s", ColorUtilConstants.RED,
                                ar.cause().getMessage(), ColorUtilConstants.RESET));
                        conn.close();
                    }
                });
        });
        return promise.future();
    }

    @Override
    public Future<StringBuilder> buildUsersJson(MessageUser messageUser) throws InterruptedException, SQLException {
        Promise<StringBuilder> promise = Promise.promise();

        pool.getConnection(c -> {
            SqlConnection conn = c.result();

            conn.query(create.query(getAllUsers(), messageUser.getName()).toString()).execute(ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rows = ar.result();
                    JsonArray ja = new JsonArray();

                    for (Row row : rows) {
                        ja.add(new JsonObject().put("name", row.getString(1)));
                    }
                    conn.close();
                    promise.complete(new StringBuilder(ja.toString()));
                } else {
                    logger.error(String.format("%sError build user json: %s%s", ColorUtilConstants.RED,
                            ar.cause().getMessage(), ColorUtilConstants.RESET));
                    conn.close();
                }
            });
        });
        return promise.future();
    }

    @Override
    public Future<Map<String, Integer>> processUserMessages(ServerWebSocket ws, MessageUser messageUser)
            throws Exception {
        RemoveUndelivered removeUndelivered = new RemoveUndeliveredCubrid();
        RemoveMessage removeMessage = new RemoveMessageCubrid();
        CompletePromise completePromise = new CompletePromise();

        removeUndelivered.setUserId(messageUser.getId());

        /*
         * Get all undelivered messages for current user
         */
        Future<Void> future = Future.future(promise -> {
            completePromise.setPromise(promise);
            pool.rxGetConnection()
                .flatMapCompletable(
                    conn -> conn.rxBegin()
                        .flatMapCompletable(tx -> conn
                            .query(create.query(getUserUndelivered(), messageUser.getId()).toString())
                            .rxExecute().doOnSuccess(rows -> {
                                for (Row row : rows) {
                                    OffsetDateTime postDate = null;

                                    postDate = row.getOffsetDateTime("POST_DATE");

                                    long epochMilli = postDate.toInstant().toEpochMilli();
                                    Date date = new Date(epochMilli);

                                    DateFormat formatDate = DateFormat
                                            .getDateInstance(DateFormat.DEFAULT, Locale.getDefault());

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
                                    tx.completion(x -> {
                                        if (x.failed()) {
                                            tx.rollback();
                                            logger.error(String.format(
                                                "%sMessages Transaction Error: %s%s",
                                                ColorUtilConstants.RED, x.cause(),
                                                ColorUtilConstants.RESET));
                                        }
                                        conn.close();
                                    });
                                })))
                    .doOnError(err -> {
                        logger.info(String.format("%sDatabase for Messages Error: %s%s",
                                ColorUtilConstants.RED, err.getMessage(), ColorUtilConstants.RESET));
                        err.printStackTrace();
                        conn.close();
                    }))
                .subscribe();
        });

        future.compose(v -> Future.<Void>future(promise -> {
            removeUndelivered.setPromise(promise);
            try {
                removeUndelivered.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (removeUndelivered.getCount() > 0) {
                logger.info(
                    String.join(ColorUtilConstants.BLUE_BOLD_BRIGHT, Integer.toString(removeUndelivered.getCount()),
                            " Messages Delivered", " to ", messageUser.getName(), ColorUtilConstants.RESET));
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
                pool.getConnection(c -> {
                    SqlConnection conn = c.result();
                    String query = create.query(getRemoveUndelivered(), userId, messageId).toString();

                    conn.query(query).execute(ar -> {
                        if (ar.succeeded()) {
                            RowSet<Row> rows = ar.result();
                            for (Row row : rows) {
                                logger.info(row.toJson().toString());
                            }
                            count += rows.rowCount() == 0 ? 1 : rows.rowCount();
                        } else {
                            logger.error(String.format("Deleting Undelivered: %s", ar.cause().getMessage()));
                        }
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
                    });
                });
            }
        }
    }

    class RemoveMessageCubrid extends RemoveMessage {
        @Override
        public void run() throws Exception {
            completePromise.setPromise(promise);

            for (Long messageId : messageIds) {
                pool.getConnection(c -> {
                    String sql = null;

                    SqlConnection conn = c.result();

                    sql = create.query(getRemoveMessage(), messageId, messageId).toString();

                    conn.query(sql).execute(ar -> {
                        if (ar.succeeded()) {
                            RowSet<Row> rows = ar.result();

                            count += rows.rowCount() == 0 ? 1 : rows.rowCount();
                            if (messageIds.size() == count) {
                                try {
                                    conn.close();
                                    completePromise.run();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            logger.error(String.format("%sDeleting Message: %s%s", ColorUtilConstants.RED,
                                ar.cause().getMessage(), ColorUtilConstants.RESET));
                        }
                    });
                });
            }
        }
    }

    @Override
    public Future<Long> addMessage(ServerWebSocket ws, MessageUser messageUser, String message)
            throws InterruptedException, SQLException {
        Promise<Long> promise = Promise.promise();

        Timestamp current = new Timestamp(new Date().getTime());

        Object postDate = current;

        pool.getConnection(ar -> {
            if (ar.succeeded()) {
                SqlConnection conn = ar.result();

                String query = create.query(getAddMessage(), message, messageUser.getName(), postDate).toString();

                conn.query(query).rxExecute().doOnSuccess(rows -> {
                    Long id = 0L;
                    for (Row row : rows) {
                        id = row.getLong(0);
                    }
                    id = rows.property(JDBCPool.GENERATED_KEYS).getLong(0);
                    conn.close();
                    promise.complete(id);
                }).doOnError(err -> {
                    logger.error(String.format("%sError adding messaage: %s%s", ColorUtilConstants.RED, err,
                            ColorUtilConstants.RESET));
                    ws.writeTextMessage(err.toString());
                    conn.close();
                }).subscribe(rows -> {
                    //
                }, err -> {
                    if (err != null && err.getMessage() != null) {
                        err.printStackTrace();
                    }
                });
            }

            if (ar.failed()) {
                logger.error(String.format("%sFailed Adding Message: - %s%s", ColorUtilConstants.RED,
                        ar.cause().getMessage(), ColorUtilConstants.RESET));
            }
        });

        return promise.future();
    }
}