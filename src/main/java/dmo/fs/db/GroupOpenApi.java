package dmo.fs.db;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.sql.SQLException;

public interface GroupOpenApi {

  Future<JsonObject> addGroupAndMembers(JsonObject addGroupJson)
      throws InterruptedException, SQLException, IOException;

  Future<JsonObject> deleteGroupOrMembers(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException;

  Future<JsonObject> deleteGroup(JsonObject deleteGroupJson)
      throws InterruptedException, SQLException, IOException;

  Future<JsonObject> getMembersList(JsonObject getGroupJson)
      throws InterruptedException, SQLException, IOException;
}
