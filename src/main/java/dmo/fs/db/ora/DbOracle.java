
package dmo.fs.db.ora;

import dmo.fs.hib.bld.DatabaseBuild;

import java.io.Serializable;

public class DbOracle implements Serializable, DatabaseBuild {

    protected enum CreateTable {
        CREATEUSERS("create table users (" +
            "id INTEGER GENERATED ALWAYS AS IDENTITY INCREMENT BY 1 PRIMARY KEY," +
            "name varchar2(255) not null," +
            "password varchar2(255) not null," +
            "ip varchar2(255) not null," +
            "last_login TIMESTAMP not null)"),
        CREATENAMEIDX(
            "CREATE UNIQUE INDEX USERS_NAME_IDX ON USERS (NAME)"),
        CREATEPASSWORDIDX(
            "CREATE UNIQUE INDEX USERS_PASSWORD_IDX ON USERS (PASSWORD)"),
        CREATEMESSAGES("create table messages (" +
            "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
            "message clob not null," +
            "from_handle varchar2(255) not null," +
            "post_date TIMESTAMP not null)"),
        CREATEUNDELIVERED("create table undelivered (" +
            "user_id integer, message_id integer," +
            "CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id)," +
            "CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id) REFERENCES messages (id))"),
        CREATEGROUPS("CREATE TABLE groups (" +
            "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
            "NAME VARCHAR2(24) NOT NULL," +
            "OWNER INTEGER NOT NULL," +
            "CREATED TIMESTAMP NOT NULL," +
            "UPDATED TIMESTAMP)"),
        CREATEMEMBER("CREATE TABLE member (" +
            "GROUP_ID INTEGER NOT NULL," +
            "USER_ID INTEGER NOT NULL," +
            "PRIMARY KEY (GROUP_ID,USER_ID)," +
            "CONSTRAINT U_fk_MEMBER_GROUP FOREIGN KEY (GROUP_ID) REFERENCES groups (ID)," +
            "CONSTRAINT U_fk_MEMBER_USER FOREIGN KEY (USER_ID) REFERENCES users (ID))"),
        CREATEGOLFER(
          "CREATE TABLE golfer (" +
            "PIN VARCHAR(8) primary key NOT NULL," +
            "FIRST_NAME VARCHAR(32) NOT NULL," +
            "LAST_NAME VARCHAR(32) NOT NULL," +
            "HANDICAP NUMBER(4,1) DEFAULT 0.0," +
            "COUNTRY CHAR(2) DEFAULT 'US' NOT NULL," +
            "STATE CHAR(2) DEFAULT 'NV' NOT NULL," +
            "OVERLAP_YEARS char(1) DEFAULT 'N'," +
            "PUBLIC_DISPLAY char(1) DEFAULT 'N'," +
            "LAST_LOGIN TIMESTAMP NOT NULL)"),
        CREATECOURSE(
          "CREATE TABLE course (" +
            "COURSE_SEQ INTEGER GENERATED ALWAYS AS IDENTITY INCREMENT BY 1 PRIMARY KEY," +
            "COURSE_NAME VARCHAR(128) NOT NULL," +
            "COURSE_COUNTRY VARCHAR(128) NOT NULL," +
            "COURSE_STATE CHAR(2) NOT NULL )"),
        CREATERATINGS(
          "CREATE TABLE ratings (" +
            "COURSE_SEQ INTEGER NOT NULL," +
            "TEE INTEGER NOT NULL," +
            "TEE_COLOR VARCHAR(16)," +
            "TEE_RATING NUMBER(4,1) NOT NULL," +
            "TEE_SLOPE INTEGER NOT NULL," +
            "TEE_PAR INTEGER DEFAULT '72' NOT NULL," +
            "PRIMARY KEY (COURSE_SEQ, TEE)," +
            "CONSTRAINT fk_course_ratings " +
            "FOREIGN KEY (COURSE_SEQ) " +
            "REFERENCES course (COURSE_SEQ))"),
        CREATERATINGSIDX(
          "CREATE UNIQUE INDEX COURSE_RATING_IDX ON RATINGS (COURSE_SEQ, TEE ASC)"),
        CREATESCORES(
          "CREATE TABLE scores (" +
            "PIN VARCHAR(8) NOT NULL," +
            "GROSS_SCORE INTEGER NOT NULL," +
            "NET_SCORE NUMBER(4,1)," +
            "ADJUSTED_SCORE INTEGER NOT NULL," +
            "TEE_TIME TIMESTAMP NOT NULL," +
            "HANDICAP NUMBER(4,1)," +
            "COURSE_SEQ INTEGER," +
            "COURSE_TEES INTEGER," +
            "USED CHAR(1)," +
            "CONSTRAINT fk_course_scores " +
            "FOREIGN KEY (COURSE_SEQ) " +
            "REFERENCES course (COURSE_SEQ), " +
            "CONSTRAINT fk_golfer_scores " +
            "FOREIGN KEY (PIN) " +
            "REFERENCES golfer (PIN))");

        final String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    }

    public DbOracle() {
        super();
    }

    public String getCreateTable(String table) {
        return CreateTable.valueOf("CREATE" + table.toUpperCase()).sql;
    }
}
