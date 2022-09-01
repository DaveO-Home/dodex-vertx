
package dmo.fs.spa.db;

public abstract class DbNeo4j extends DbNeo4jBase implements SpaNeo4j {

	private enum CreateSchema {
		CREATECONSTRAINTS(
			"CALL apoc.schema.assert(null,{Login:['name']},False);"
		),
		CHECKCONSTRAINTS(
			"CALL db.constraints() YIELD description WHERE description contains 'Login' return count(*) as count;"
		),
		CHECKAPOC(
			"CALL dbms.procedures() YIELD name where name = 'apoc.schema.assert' RETURN count(*) as count;"
		),
		LOGINNAMECONSTRAINT(
			"CREATE CONSTRAINT ON (login:Login) ASSERT login.name IS UNIQUE;"
		);
		
        String sql;

        CreateSchema(String sql) {
            this.sql = sql;
        }
    };

	protected DbNeo4j() {
		super();
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
	public String getLoginNameConstraint() {
		return CreateSchema.valueOf("LOGINNAMECONSTRAINT").sql;
	}
}
