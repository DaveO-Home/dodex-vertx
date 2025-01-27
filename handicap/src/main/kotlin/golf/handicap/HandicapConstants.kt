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
}