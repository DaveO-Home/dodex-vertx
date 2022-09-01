
package dmo.fs.db;

public abstract class DbNeo4j extends DbNeo4jBase implements DodexNeo4j {

	private enum CreateSchema {
		CREATEINDEXUSER(
			"CREATE index if not exists for (u:User) on (u.name, u.password);"
		),
		CREATEINDEXMESSAGE(	
			"CREATE index if not exists for (m:Message) on (m.name, m.password);"
		),
		CREATECONSTRAINTS(
			"CALL apoc.schema.assert(null,{Message:['name', 'password'], User:['name','password']},False);"
		),
		CHECKCONSTRAINTS(
			"CALL db.constraints() YIELD description WHERE description contains 'User' or description contains 'Message' return count(*) as count;"
		),
		CHECKAPOC(
			"CALL dbms.procedures() YIELD name where name = 'apoc.schema.assert' RETURN count(*) as count;"
		),
		USERNAMECONSTRAINT(
			"CREATE CONSTRAINT ON (user:User) ASSERT user.name IS UNIQUE;"
		),
		USERPASSCONSTRAINT(
			"CREATE CONSTRAINT ON (user:User) ASSERT user.password IS UNIQUE;"
		),
		MESSAGENAMECONSTRAINT(
			"CREATE CONSTRAINT ON (m:Message) ASSERT m.name IS UNIQUE;"
		),
		MESSAGEPASSCONSTRAINT(
			"CREATE CONSTRAINT ON (m:Message) ASSERT m.password IS UNIQUE;"
		);
		
        String sql;

        CreateSchema(String sql) {
            this.sql = sql;
        }
    };

	protected DbNeo4j() {
		super();
	}

	public String getCreateUserIndex() {
		return CreateSchema.valueOf("CREATEINDEXUSER").sql;
	}
	public String getCreateMessageIndex() {
		return CreateSchema.valueOf("CREATEINDEXMESSAGE").sql;
	}
	public String getCreateConstraints() {
		return CreateSchema.valueOf("CREATECONSTRAINTS").sql;
	}
	public String getCheckApoc() {
		return CreateSchema.valueOf("CHECKAPOC").sql;
	}
	public String getCheckConstraints() {
		return CreateSchema.valueOf("CHECKCONSTRAINTS").sql;
	}
	public String getUserNameConstraint() {
		return CreateSchema.valueOf("USERNAMECONSTRAINT").sql;
	}
	public String getUserPasswordConstraint() {
		return CreateSchema.valueOf("USERPASSCONSTRAINT").sql;
	}
	public String getMessageNameConstraint() {
		return CreateSchema.valueOf("MESSAGENAMECONSTRAINT").sql;
	}
	public String getMessagePasswordConstraint() {
		return CreateSchema.valueOf("MESSAGEPASSCONSTRAINT").sql;
	}
}
