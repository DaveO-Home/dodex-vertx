/*
 * HandicapConstants.java
 *
 * Created on June 8, 2005, 10:30 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package golf.handicap

/**
 *
 * @author daveo
 */
object HandicapConstants {
    const val GET_COURSES = 1
    const val SET_COURSES = 2
    const val GET_GOLFER = 3
    const val SET_GOLFER = 4
    const val GET_SCORES = 5
    const val SET_SCORE = 6
    const val SET_HANDICAP = 7
    const val GET_HANDICAP = 8
    const val defaultDriver = "org.sqlite.JDBC.ClientDriver" //"org.gjt.mm.mysql.Driver";
    const val defaultURL = "jdbc:sqlite:handicap.db?foreign_keys=on;"
    const val defaultUID = ""
    const val defaultPWD = "" 
    // const static final states: Arrary<String> = arrayOf<String> ("","AK", "AL", "AR", "AZ", "CA", "CO", "CT", "DE", "FL", 
    //                           "GA", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", 
    //                           "MD", "ME", "MI", "MN", "MO", "MS", "MT" ,"NC", "ND", "NE", 
    //                           "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "RI", 
    //                           "SC", "SD", "TN", "TX", "UT", "VA", "VT", "WA", "WI", "WV", "WY");
    
    /*
    const val defaultDriver = "org.apache.derby.jdbc.ClientDriver" //"org.gjt.mm.mysql.Driver";
    const val defaultURL = "jdbc:derby://localhost:1527/handicap"
    const val defaultUID = "handicap"
    const val defaultPWD = "handicap" 

    public static final String defaultDriver = "com.mysql.jdbc.Driver"; //"org.gjt.mm.mysql.Driver";
    public static final String defaultURL = "jdbc:mysql://localhost/handicap?//user=root=";
    
    public static final String defaultURL = "jdbc:pointbase://localhost:9092/handicap";
    public static final String defaultDriver = "com.pointbase.jdbc.jdbcUniversalDriver";
    public static final String defaultUID = "Handicap";
    public static final String defaultPWD = "Handicap";
    */
}