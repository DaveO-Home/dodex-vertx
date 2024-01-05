
package dmo.fs.db.mongodb;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbMongo extends DbMongoBase implements DodexMongo {
  private final static Logger logger = LoggerFactory.getLogger(DbMongo.class.getName());

  private enum CreateSchema {
    CREATEUSERMESSAGE(getUserMessageJsonObject()),
    CREATEMESSAGEUSER(getMessageUserJsonObject()),
    CREATEGROUPUSER(getGroupMemberJsonObject()),
    CREATEINDEXUSERID(
        new JsonObject().put("user_id", 1)
    ),
    CREATEINDEXGROUPNAME(
        new JsonObject().put("name", 1)
    ),
    CREATEINDEXNAMEPASSWORD(
        new JsonObject().put("name", 1).put("password", 2)
    );
    final JsonObject jsonObject;

    CreateSchema(JsonObject jsonObject) {
      this.jsonObject = jsonObject;
    }
  }

  protected DbMongo() {
    super();
  }

  public JsonObject getUserMessage() {
    return CreateSchema.valueOf("CREATEUSERMESSAGE").jsonObject;
  }

  public JsonObject getNamePasswordIndex() {
    return CreateSchema.valueOf("CREATEINDEXNAMEPASSWORD").jsonObject;
  }

  public JsonObject getUserIdIndex() {
    return CreateSchema.valueOf("CREATEINDEXUSERID").jsonObject;
  }
  public JsonObject getGroupNameIndex() {
    return CreateSchema.valueOf("CREATEINDEXGROUPNAME").jsonObject;
  }

  public JsonObject getMessageUser() {
    return CreateSchema.valueOf("CREATEMESSAGEUSER").jsonObject;
  }

  public JsonObject getGroupMember() {
    return CreateSchema.valueOf("CREATEGROUPUSER").jsonObject;
  }

  private static JsonObject getUserMessageJsonObject() {
    JsonObject validator = new JsonObject();
    JsonObject schema = new JsonObject();
    JsonObject properties = new JsonObject();
    JsonObject property = new JsonObject();

    property.put("bsonType", "string").put("description", "generated 'name' of required user");
    properties.put("name", property);
    property = new JsonObject().put("bsonType", "string").put("description", "generated required 'password'");
    properties.put("password", property);
    property = new JsonObject().put("bsonType", "string").put("description", "required remote 'ip'");
    properties.put("ip", property);
    property = new JsonObject().put("bsonType", "date").put("description", "required last login date");
    properties.put("last_login", property);
    schema.put("$jsonSchema", new JsonObject().put("bsonType", "object")
        .put("title", "Users Object Validation")
        .put("required", new JsonArray().add("name").add("password").add("ip").add("last_login"))
        .put("properties", properties));
    return validator.put("validator", schema);
  }

  private static JsonObject getMessageUserJsonObject() {
    JsonObject validator = new JsonObject();
    JsonObject schema = new JsonObject();
    JsonObject properties = new JsonObject();


    JsonObject property = new JsonObject().put("bsonType", "string").put("description", "generated 'name' of required user");
    properties.put("name", property);
    property = new JsonObject().put("bsonType", "string").put("description", "generated required 'password'");
    properties.put("password", property);
    property = new JsonObject().put("bsonType", "string").put("description", "generated 'userid' of required message");
    properties.put("user_id", property);
    property = new JsonObject().put("bsonType", "string").put("description", "generated 'from handle' of required message");
    properties.put("from_handle", property);
    property = new JsonObject().put("bsonType", "date").put("description", "generated 'message' of required message");
    properties.put("message", property);
    property = new JsonObject().put("bsonType", "string").put("description", "required posted date"); // seems "date" fails with bulkWrite
    properties.put("post_date", property);
    schema.put("$jsonSchema", new JsonObject().put("bsonType", "object")
        .put("title", "Messages Object Validation")
        .put("required", new JsonArray().add("name").add("password").add("user_id").add("from_handle").add("message").add("post_date"))
        .put("properties", properties));
    return validator.put("validator", schema);
  }

  private static JsonObject getGroupMemberJsonObject() {
    JsonObject validator = new JsonObject();
    JsonObject schema = new JsonObject();
    JsonObject properties = new JsonObject();

    JsonObject property = new JsonObject().put("bsonType", "string").put("description", "required unique group name");
    properties.put("name", property);
    property = new JsonObject().put("bsonType", "string").put("description", "required Owner of group");
    properties.put("owner", property);
    property = new JsonObject().put("bsonType", "string").put("description", "0 to N members(user_messages(_ids))");
    properties.put("created", property);
    property = new JsonObject().put("bsonType", "string").put("description", "last updated date");
    properties.put("updated", property);
    schema.put("$jsonSchema", new JsonObject().put("bsonType", "object")
        .put("title", "Groups Object Validation")
        .put("required", new JsonArray().add("name").add("owner").add("created").add("members"))
        .put("properties", properties));

    return validator.put("validator", schema);
  }
}
