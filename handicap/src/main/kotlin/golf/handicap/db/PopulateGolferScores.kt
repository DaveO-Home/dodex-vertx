package golf.handicap.db

// import golf.handicap.Course.key

import dmo.fs.db.DbConfiguration
import dmo.fs.utils.ColorUtilConstants
import golf.handicap.*
import golf.handicap.generated.tables.references.COURSE
import golf.handicap.generated.tables.references.GOLFER
import golf.handicap.generated.tables.references.RATINGS
import golf.handicap.generated.tables.references.SCORES
import io.reactivex.rxjava3.core.Single
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.Promise
import io.vertx.rxjava3.sqlclient.Tuple
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.*
import java.sql.Date
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.Year
import java.util.*
import java.util.logging.Logger
import kotlin.Throws
import org.jooq.*
import org.jooq.impl.*
import org.jooq.impl.DSL.*

class PopulateGolferScores : SqlConstants() {
  private val teeDate = SimpleDateFormat("yyyy-MM-dd")
  private val teeYear = SimpleDateFormat("yyyy")
  private val year: String? = null
  private var beginDate: String? = null
  private var endDate: String? = null
  private var maxRows = 20
  var gettingData = false
  var beginGolfDate: java.util.Date? = null
  var endGolfDate: java.util.Date? = null
  var overlapYears = false

  companion object {
    private val LOGGER = Logger.getLogger(PopulateGolferScores::class.java.name)
    //
    private val regEx = "\\$\\d".toRegex()

    @Throws(SQLException::class)
    @JvmStatic
    public fun buildSql() {
      GETSETUSEDUPDATE =
          if (qmark) setupSetUsedUpdate().replace(regEx, "?")
          else setupSetUsedUpdate().replace("\"", "")
      val dialect = create!!.dsl().dialect().toString()
      if ("SQLITE".equals(dialect) || "DEFAULT".equals(dialect)) {
        GETSETUSEDSQLITEUPDATE =
            if (qmark) setupSqliteSetUsedUpdate().replace(regEx, "?")
            else setupSqliteSetUsedUpdate()
      }
      GETRESETUSEDUPDATE =
          if (qmark) setupResetUsedUpdate().replace(regEx, "?")
          else setupResetUsedUpdate().replace("\"", "")
      GETRESETUSEDSQLITEUPDATE =
          if (qmark) setupSqliteResetUsedUpdate().replace(regEx, "?")
          else setupSqliteResetUsedUpdate()
      GETHANDICAPUPDATE =
          if (qmark) setupHandicapUpdate().replace(regEx, "?")
          else setupHandicapUpdate().replace("\"", "")
      GETHANDICAPSQLITEUPDATE =
          if (qmark) setupSqliteHandicapUpdate().replace(regEx, "?")
          else setupSqliteHandicapUpdate()
      GETSCORESUPDATE =
          if (qmark) setupScoreUpdate().replace(regEx, "?")
          else setupScoreUpdate().replace("\"", "")
      GETSCORESSQLITEUPDATE =
          if (qmark) setupSqliteScoreUpdate().replace(regEx, "?")
          else setupSqliteScoreUpdate().replace("\"", "")
      GETGOLFERDATA =
          if (qmark) setupGetGolferData().replace(regEx, "?")
          else setupGetGolferData().replace("\"", "")
      GETGOLFERPUBLICDATA =
          if (qmark) setupGetPublicGolferData().replace(regEx, "?")
          else setupGetPublicGolferData().replace("\"", "")
      GETREMOVESCORE =
          if (qmark) setupRemoveScore().replace(regEx, "?")
          else setupRemoveScore().replace("\"", "")
      GETREMOVESCORESUB =
          if (qmark) setupRemoveScoreSub().replace(regEx, "?")
          else setupRemoveScoreSub().replace("\"", "")
      GETLASTSCORE =
          if (qmark) setupGetLastScore().replace(regEx, "?")
          else setupGetLastScore().replace("\"", "")
      GETGOLFERSCORES =
          if (qmark) setupGetGolferScores().replace(regEx, "?")
          else setupGetGolferScores().replace("\"", "")
    }

    init {}

    @JvmStatic
    private fun setupSetUsedUpdate(): String {
      return create!!.renderNamedParams(
          update(SCORES)
              .set(SCORES.USED, "*")
              .where(SCORES.PIN.eq("$").and(SCORES.COURSE_SEQ.eq(0)).and(SCORES.TEE_TIME.eq("$")))
      )
    }

    @JvmStatic
    private fun setupSqliteSetUsedUpdate(): String {
      return create!!.renderNamedParams(
          update(table("SCORES"))
              .set(field("USED"), '*')
              .where(
                  (field("PIN")
                      .eq("$")
                      .and(field("COURSE_SEQ").eq("$"))
                      .and(field("TEE_TIME").eq("$")))
              )
      )
    }

    @JvmStatic
    private fun setupResetUsedUpdate(): String {
      return create!!.renderNamedParams(
          update(SCORES).setNull(SCORES.USED).where(SCORES.PIN.eq("$").and(SCORES.USED.eq("$")))
      )
    }

    @JvmStatic
    private fun setupSqliteResetUsedUpdate(): String {
      return create!!.renderNamedParams(
          update(table("SCORES"))
              .setNull(field("USED"))
              .where((field("PIN").eq("$").and(field("USED").eq("$"))))
      )
    }

    @JvmStatic
    private fun setupHandicapUpdate(): String {
      return create!!.renderNamedParams(
          update(GOLFER).set(GOLFER.HANDICAP, 0.0.toFloat()).where(GOLFER.PIN.eq("$"))
      )
    }

    @JvmStatic
    private fun setupSqliteHandicapUpdate(): String {
      return create!!.renderNamedParams(
          update(table("GOLFER")).set(field("HANDICAP"), "$").where((field("PIN").eq("$")))
      )
    }

    @JvmStatic
    private fun setupScoreUpdate(): String {
      return create!!.renderNamedParams(
          update(SCORES)
              .set(SCORES.HANDICAP, 0.0.toFloat())
              .set(SCORES.NET_SCORE, 0.0.toFloat())
              .where(SCORES.COURSE_SEQ.eq(0).and(SCORES.PIN.eq("$")).and(SCORES.TEE_TIME.eq("$")))
      )
    }

    @JvmStatic
    private fun setupSqliteScoreUpdate(): String {
      return create!!.renderNamedParams(
          update(table("SCORES"))
              .set(field("HANDICAP"), "$")
              .set(field("NET_SCORE"), "$")
              .where(
                  field("COURSE_SEQ")
                      .eq("$")
                      .and(field("PIN").eq("$").and(field("TEE_TIME").eq("$")))
              )
      )
    }

    @JvmStatic
    private fun setupGetGolferData(): String {
      // val json: String = create!!.selectFrom(GOLFER, RATINGS, COURSE,
      // SCORES).where().fetch().formatJSON();
      return create!!.renderNamedParams(
          select(
                  COURSE.COURSE_SEQ,
                  COURSE.COURSE_NAME,
                  SCORES.PIN,
                  SCORES.GROSS_SCORE,
                  SCORES.NET_SCORE,
                  SCORES.ADJUSTED_SCORE,
                  SCORES.HANDICAP,
                  SCORES.COURSE_TEES,
                  SCORES.TEE_TIME,
                  SCORES.USED
              )
              .from(GOLFER, RATINGS, COURSE, SCORES)
              .where(
                  (GOLFER.PIN.eq(SCORES.PIN))
                      .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                      .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                      .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                      .and(GOLFER.PIN.eq("$"))
                      .and(SCORES.TEE_TIME.between("$").and("$"))
              )
              .orderBy(SCORES.TEE_TIME.desc())
      )
    }

    @JvmStatic
    private fun setupGetPublicGolferData(): String {

      return create!!.renderNamedParams(
          select(
                  COURSE.COURSE_SEQ,
                  COURSE.COURSE_NAME,
                  SCORES.PIN,
                  SCORES.GROSS_SCORE,
                  SCORES.NET_SCORE,
                  SCORES.ADJUSTED_SCORE,
                  SCORES.HANDICAP,
                  SCORES.COURSE_TEES,
                  SCORES.TEE_TIME,
                  SCORES.USED
              )
              .from(GOLFER, RATINGS, COURSE, SCORES)
              .where(
                  (GOLFER.PIN.eq(SCORES.PIN))
                      .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                      .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                      .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                      .and(GOLFER.FIRST_NAME.eq("$"))
                      .and(GOLFER.LAST_NAME.eq("$"))
                      .and(SCORES.TEE_TIME.between("$").and("$"))
              )
              .orderBy(SCORES.TEE_TIME.desc())
      )
    }

    @JvmStatic
    private fun setupRemoveScore(): String {
      return create!!.renderNamedParams(
          delete(SCORES)
              .where(SCORES.PIN.eq("$").and(SCORES.COURSE_SEQ.eq(0)).and(SCORES.TEE_TIME.eq("$")))
      )
    }

    @JvmStatic
    private fun setupRemoveScoreSub(): String {
      return create!!.renderNamedParams(
          delete(SCORES)
              .where(
                  (SCORES.PIN.eq("$")).and(
                      SCORES.TEE_TIME.eq(
                          select(max(SCORES.TEE_TIME)).from(SCORES).where(SCORES.PIN.eq("$"))
                      )
                  )
              )
      )
    }

    @JvmStatic
    private fun setupGetLastScore(): String {
      return create!!.renderNamedParams(
          select(SCORES.USED, SCORES.COURSE_SEQ, SCORES.TEE_TIME)
              .from(SCORES)
              .where(
                  (SCORES.PIN.eq("$")).and(
                      SCORES.TEE_TIME.eq(
                          select(max(SCORES.TEE_TIME)).from(SCORES).where(SCORES.PIN.eq("$"))
                      )
                  )
              )
      )
    }

    @JvmStatic
    private fun setupGetGolferScores(): String {
      return create!!.renderNamedParams(
          select(
                  GOLFER.PIN,
                  RATINGS.TEE_RATING,
                  RATINGS.TEE_SLOPE,
                  SCORES.ADJUSTED_SCORE,
                  SCORES.TEE_TIME,
                  COURSE.COURSE_SEQ,
                  RATINGS.TEE_PAR
              )
              .from(GOLFER, RATINGS, COURSE, SCORES)
              .where(
                  (GOLFER.PIN.eq(SCORES.PIN))
                      .and(COURSE.COURSE_SEQ.eq(SCORES.COURSE_SEQ))
                      .and(COURSE.COURSE_SEQ.eq(RATINGS.COURSE_SEQ))
                      .and(RATINGS.TEE.eq(SCORES.COURSE_TEES))
                      .and(GOLFER.PIN.eq("$"))
                      .and(SCORES.TEE_TIME.between("$").and("$"))
              )
              .orderBy(SCORES.TEE_TIME.desc())
      )
    }
  }

  fun setPeriod(year: String) {
    beginDate = "$year-01-01"
    endDate = (year.toInt() + 1).toString() + "-01-01"
    beginGolfDate = teeDate.parse(beginDate, ParsePosition(0))
    endGolfDate = teeDate.parse(endDate, ParsePosition(0))
  }

  fun useCurrentYearOnly(thisYearOnly: Boolean) {
    overlapYears = thisYearOnly
  }

  @Throws(SQLException::class, InterruptedException::class)
  fun setGolferHandicap(golfer: Golfer): Future<Int> {
    val handicapPromise: Promise<Int> = Promise.promise()
    var rowsUpdated = 0
    var sql: String?

    pool!!
        .rxGetConnection()
        .flatMapCompletable { conn ->
          conn.rxBegin().flatMapCompletable { tx ->
            var parameters: Tuple = Tuple.tuple()
            parameters.addFloat(golfer.handicap)
            parameters.addString(golfer.pin)

            if (DbConfiguration.isUsingSqlite3()) {
              sql = GETHANDICAPSQLITEUPDATE
            } else {
              sql = GETHANDICAPUPDATE
            }

            conn.preparedQuery(sql)
                .rxExecute(parameters)
                .doOnSuccess { rows ->
                  rowsUpdated += rows.rowCount()
                  tx.commit()
                }
                .doOnError { err ->
                  tx.rollback()
                  LOGGER.severe(
                      String.format(
                          "%sError Updating Handicap - %s%s",
                          ColorUtilConstants.RED,
                          err.message,
                          ColorUtilConstants.RESET
                      )
                  )

                  handicapPromise.complete(0)
                }
                .flatMap {
                  val score = golfer.score
                  if (score == null) {
                    Single.just(0)
                  } else {
                    parameters = Tuple.tuple()
                    parameters.addFloat(golfer.handicap)
                    parameters.addFloat(score.netScore)
                    parameters.addInteger(score.course!!.key)
                    parameters.addString(golfer.pin)
                    parameters.addString(score.teeTime)

                    if (DbConfiguration.isUsingSqlite3()) {
                      sql = GETSCORESSQLITEUPDATE
                    } else {
                      sql = GETSCORESUPDATE
                    }
                    conn.preparedQuery(sql)
                        .rxExecute(parameters)
                        .doOnSuccess { rows -> rowsUpdated += rows.rowCount() }
                        .doOnError { err ->
                          LOGGER.severe(
                              String.format(
                                  "%sError Updating Scores - %s%s",
                                  ColorUtilConstants.RED,
                                  err.message,
                                  ColorUtilConstants.RESET
                              )
                          )
                          tx.rxRollback()
                        }
                  }
                }
                .doOnError { err ->
                  LOGGER.warning(
                      String.format(
                          "%sError Updating Scores - %s%s",
                          ColorUtilConstants.RED,
                          err.message,
                          ColorUtilConstants.RESET
                      )
                  )
                }
                .flatMapCompletable { _ ->
                  tx.commit()
                  conn.close()
                }
          }
        }
        .subscribe { handicapPromise.complete(rowsUpdated) }

    return handicapPromise.future()
  }

  @Throws(Exception::class)
  fun getGolferScores(golfer: Golfer, rows: Int): Future<Map<String, Any?>>? {
    val oldRows = this.maxRows
    var golferScores: Promise<Map<String, Any?>>? = Promise.promise()
    this.maxRows = rows
    gettingData = true
    getGolferScores(golfer)!!.onSuccess { data ->
      gettingData = false
      this.maxRows = oldRows
      golferScores!!.complete(data)
    }
    return golferScores!!.future()
  }

  @Throws(Exception::class)
  fun getGolferScores(golfer: Golfer): Future<Map<String, Any?>>? {
    val scoresPromise: Promise<Map<String, Any?>> = Promise.promise()
    val golferPin = golfer.pin
    val previousYear = Year.now().getValue() - 1
    overlapYears = golfer.overlap

    beginDate = if (overlapYears) "01-01-$previousYear" else beginDate
    var sql: String?

    pool!!
        .rxGetConnection()
        .doOnSuccess { conn ->
          val parameters: Tuple = Tuple.tuple()
          val maximumRows = " limit $maxRows"

          if (gettingData) {
            if (golferPin == null || golferPin.length == 0) {
              parameters.addString(golfer.firstName)
              parameters.addString(golfer.lastName)
              sql = GETGOLFERPUBLICDATA + maximumRows
            } else {
              parameters.addString(golferPin)
              sql = GETGOLFERDATA + maximumRows
            }
          } else {
            parameters.addString(golferPin)
            sql = GETGOLFERSCORES + maximumRows
          }
          parameters.addString(beginDate)
          parameters.addString(teeDate.format(endGolfDate))

          conn.preparedQuery(sql)
              .rxExecute(parameters)
              .doOnSuccess { rows ->
                val columns = rows.columnsNames().size
                var x = 0
                var y: Int
                val tableArray: JsonArray = JsonArray()

                for (row in rows) {
                  y = 0
                  val rowObject = JsonObject()
                  while (y < columns) {
                    var name = row.getColumnName(y)
                    name = if (DbConfiguration.isUsingPostgres()) name.uppercase() else name
                    if ("NET_SCORE".equals(name) || "HANDICAP".equals(name)) {
                      rowObject.put(
                          name,
                          BigDecimal(row.getValue(y++).toString()).setScale(1, RoundingMode.UP)
                      )
                    } else if ("TEE_TIME".equals(name) && gettingData) {
                      rowObject.put(name, row.getValue(y++).toString().substring(0, 10))
                    } else {
                      rowObject.put(name, row.getValue(y++))
                    }
                  }
                  x++
                  tableArray.add(rowObject)
                }

                val tableMap: MutableMap<String, Any> = HashMap()
                if (tableArray.size() != 0) {
                  tableMap.put("array", tableArray)
                }
                scoresPromise.complete(tableMap)
              }
              .doOnError { _ ->
              }
              .subscribe({}, { err ->
                LOGGER.severe(
                    String.format(
                        "%sError Getting Score(s) Data - %s%s \n%s",
                        ColorUtilConstants.RED,
                        err.message,
                        ColorUtilConstants.RESET,
                        err.stackTraceToString()
                    )
                )
              })
        }
        .subscribe()

    return scoresPromise.future()
  }

  @Throws(Exception::class)
  fun setUsed(pin: String?, course: Int, teeTime: String): Future<Int> {
    val usedPromise: Promise<Int> = Promise.promise()
    var count: Int = 0

    pool!!
        .rxGetConnection()
        .doOnSuccess { conn ->
          conn.rxBegin()
              .flatMap { tx ->
                var sql: String?
                val parameters: Tuple = Tuple.tuple()
                parameters.addString("*")
                parameters.addString(pin)
                parameters.addInteger(course)
                parameters.addString(teeTime)

                if (DbConfiguration.isUsingSqlite3()) {
                  sql = GETSETUSEDSQLITEUPDATE
                } else {
                  sql = GETSETUSEDUPDATE
                }

                conn.preparedQuery(sql)
                    .rxExecute(parameters)
                    .doOnSuccess { rows ->
                      count += rows.rowCount()
                      tx.commit()
                      conn.close()
                      usedPromise.complete(count)
                    }
                    .doOnError { _ ->
                      usedPromise.complete(count)
                      conn.close()
                      LOGGER.warning(
                          String.format(
                              "Used Parameters/Sql: %s %s",
                              parameters.deepToString(),
                              sql
                          )
                      )
                    }
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Updating Used - %s%s %s",
                            ColorUtilConstants.RED,
                            err.message,
                            ColorUtilConstants.RESET,
                            err.stackTraceToString()
                        )
                    )
                  }
              )
        }
        .subscribe()

    return usedPromise.future()
  }

  @Throws(Exception::class)
  fun clearUsed(pin: String?): Future<Int> {
    val usedPromise: Promise<Int> = Promise.promise()
    var count: Int = 0
    pool!!
        .rxGetConnection()
        .doOnSuccess { conn ->
          conn.rxBegin()
              .flatMap { tx ->
                var sql: String?
                val parameters: Tuple = Tuple.tuple()
                parameters.addString(null)

                if (DbConfiguration.isUsingSqlite3()) {
                  sql = GETRESETUSEDSQLITEUPDATE
                } else {
                  sql = GETRESETUSEDUPDATE
                }
                parameters.addString(pin)
                parameters.addString("*")

                conn.preparedQuery(sql)
                    .rxExecute(parameters)
                    .doOnSuccess { rows ->
                      count += rows.rowCount()
                      tx.commit()
                      conn.close()
                      usedPromise.complete(count)
                    }
                    .doOnError { _ ->
                      usedPromise.complete(count)
                      conn.close()
                    }
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Cleaning Used - %s%s",
                            ColorUtilConstants.RED,
                            err.message,
                            ColorUtilConstants.RESET
                        )
                    )
                  }
              )
        }
        .subscribe()
    return usedPromise.future()
  }

  @Throws(Exception::class)
  fun removeLastScore(golferPIN: String?): Future<String> {
    val usedPromise: Promise<String> = Promise.promise()
    var count: Int = 0

    pool!!
        .rxGetConnection()
        .doOnSuccess { conn ->
          conn.rxBegin()
              .flatMap { tx ->
                val parameters: Tuple = Tuple.tuple()
                parameters.addString(golferPIN)
                parameters.addString(golferPIN)
                var used: String? = ""

                conn.preparedQuery(GETLASTSCORE)
                    .rxExecute(parameters)
                    .doOnSuccess { lastRows ->
                      for (row in lastRows) {
                        used = row.getString(0)
                      }
                      if (used == null) {
                        used = "N"
                      }

                      conn.preparedQuery(GETREMOVESCORESUB)
                          .rxExecute(parameters)
                          .doOnSuccess { rows ->
                            count += rows.rowCount()
                            tx.commit()
                            conn.close()
                            usedPromise.complete(used)
                          }
                          .doOnError { _ ->
                            usedPromise.complete(used)
                            conn.close()
                          }
                          .subscribe(
                              {},
                              { err ->
                                LOGGER.severe(
                                    String.format(
                                        "%sError removing last - %s%s",
                                        ColorUtilConstants.RED,
                                        err.message,
                                        ColorUtilConstants.RESET
                                    )
                                )
                              }
                          )
                    }
                    .doOnError { _ ->
                      usedPromise.complete(used)
                      conn.close()
                    }
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError getting Used - %s%s",
                            ColorUtilConstants.RED,
                            err.message,
                            ColorUtilConstants.RESET
                        )
                    )
                  }
              )
        }
        .subscribe()

    return usedPromise.future()
  }

  init {
    setPeriod(teeYear.format(java.util.Date()))
  }
}