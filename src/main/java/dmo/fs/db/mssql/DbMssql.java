
package dmo.fs.db.mssql;

import dmo.fs.hib.bld.DatabaseBuild;

import java.io.Serializable;

public class DbMssql implements Serializable, DatabaseBuild {

    protected enum CreateTable {
        CREATEUSERS("create table users (" +
            "id INT IDENTITY (1,1) PRIMARY KEY," +
            "name varchar(255) collate LATIN1_GENERAL_100_CI_AS_SC_UTF8 not null," +
            "password varchar(255) not null," +
            "ip varchar(255) not null," +
            "last_login DATETIME not null)"),
        CREATENAMEIDX(
            "CREATE UNIQUE INDEX USERS_NAME_IDX ON USERS (NAME)"),
        CREATEPASSWORDIDX(
            "CREATE UNIQUE INDEX USERS_PASSWORD_IDX ON USERS (PASSWORD)"),
        CREATEMESSAGES("create table messages (" +
            "id INT IDENTITY (1,1) PRIMARY KEY," +
            "message text not null," +
            "from_handle varchar(255) collate LATIN1_GENERAL_100_CI_AS_SC_UTF8 not null," +
            "post_date DATETIME not null)"),
        CREATEUNDELIVERED("create table undelivered (" +
            "user_id int, message_id int," +
            "CONSTRAINT undelivered_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id)," +
            "CONSTRAINT undelivered_message_id_foreign FOREIGN KEY (message_id) REFERENCES messages (id))"),
        CREATEGROUPS("CREATE TABLE groups (" +
            "id INT IDENTITY (1,1) PRIMARY KEY," +
            "NAME VARCHAR(24) NOT NULL," +
            "OWNER INT NOT NULL," +
            "CREATED DATETIME NOT NULL," +
            "UPDATED DATETIME)"),
        CREATEMEMBER("CREATE TABLE member (" +
            "GROUP_ID INT NOT NULL," +
            "USER_ID INT NOT NULL," +
            "PRIMARY KEY (GROUP_ID,USER_ID)," +
            "CONSTRAINT U_fk_MEMBER_GROUP FOREIGN KEY (GROUP_ID) REFERENCES groups (ID)," +
            "CONSTRAINT U_fk_MEMBER_USER FOREIGN KEY (USER_ID) REFERENCES users (ID))"),
        CREATEGOLFER(
          "CREATE TABLE golfer (" +
            "PIN VARCHAR(8) primary key NOT NULL," +
            "FIRST_NAME VARCHAR(32) NOT NULL," +
            "LAST_NAME VARCHAR(32) NOT NULL," +
            "HANDICAP DECIMAL(4,1) DEFAULT 0.0," +
            "COUNTRY CHAR(2) DEFAULT 'US' NOT NULL," +
            "STATE CHAR(2) DEFAULT 'NV' NOT NULL," +
            "OVERLAP_YEARS TINYINT DEFAULT 0," +
            "PUBLIC_DISPLAY TINYINT DEFAULT 0," +
            "LAST_LOGIN DATETIME NOT NULL)"
        ),
        CREATECOURSE(
          "CREATE TABLE course (" +
            "COURSE_SEQ INT primary key IDENTITY (1,1) NOT NULL," +
            "COURSE_NAME VARCHAR(128) NOT NULL," +
            "COURSE_COUNTRY VARCHAR(128) NOT NULL," +
            "COURSE_STATE CHAR(2) NOT NULL )"),
        CREATERATINGS(
          "CREATE TABLE ratings (" +
            "COURSE_SEQ INT NOT NULL," +
            "TEE INT NOT NULL," +
            "TEE_COLOR VARCHAR(16)," +
            "TEE_RATING DECIMAL(4,1) NOT NULL," +
            "TEE_SLOPE INT NOT NULL," +
            "TEE_PAR INT DEFAULT '72' NOT NULL," +
            "PRIMARY KEY (COURSE_SEQ, TEE)," +
            "CONSTRAINT fk_course_ratings " +
            "FOREIGN KEY (COURSE_SEQ) " +
            "REFERENCES course (COURSE_SEQ) " +
            "ON DELETE NO ACTION " +
            "ON UPDATE NO ACTION)"),
        CREATERATINGSIDX(
          "CREATE UNIQUE INDEX COURSE_RATING_IDX ON RATINGS (COURSE_SEQ, TEE ASC)"),
        CREATESCORES(
          "CREATE TABLE scores (" +
            "PIN VARCHAR(8) NOT NULL," +
            "GROSS_SCORE INT NOT NULL," +
            "NET_SCORE DECIMAL(4,1)," +
            "ADJUSTED_SCORE INT NOT NULL," +
            "TEE_TIME DATETIME NOT NULL," +
            "HANDICAP DECIMAL(4,1)," +
            "COURSE_SEQ INT," +
            "COURSE_TEES INT," +
            "USED CHAR(1)," +
            "CONSTRAINT fk_course_scores " +
            "FOREIGN KEY (COURSE_SEQ) " +
            "REFERENCES course (COURSE_SEQ) " +
            "ON DELETE NO ACTION " +
            "ON UPDATE NO ACTION," +
            "CONSTRAINT fk_golfer_scores " +
            "FOREIGN KEY (PIN) " +
            "REFERENCES golfer (PIN) " +
            "ON DELETE NO ACTION " +
            "ON UPDATE NO ACTION)");

        final String sql;

        CreateTable(String sql) {
            this.sql = sql;
        }
    }

    public DbMssql() {
        super();
    }

    public String getCreateTable(String table) {
        return CreateTable.valueOf("CREATE" + table.toUpperCase()).sql;
    }
}
