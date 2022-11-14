@file:JvmName("PopulateGolfer")

package golf.handicap.db

import dmo.fs.db.DbConfiguration
import dmo.fs.utils.ColorUtilConstants
import golf.handicap.*
import golf.handicap.Golfer
import golf.handicap.generated.tables.references.GOLFER
import handicap.grpc.*
import io.grpc.stub.StreamObserver
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.rxjava3.sqlclient.Tuple
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Logger
import kotlin.Throws
import org.jooq.impl.DSL.deleteFrom
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.insertInto
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.table
import org.jooq.impl.DSL.update

class PopulateGolfer() : SqlConstants() {
  companion object {
    private val LOGGER = Logger.getLogger(PopulateGolfer::class.java.name)

    @Throws(SQLException::class)
    @JvmStatic
    public fun buildSql() {

      val regEx = "\\$\\d".toRegex()
      GETGOLFER = if (qmark) setupGetGolfer().replace(regEx, "?") else setupGetGolfer()

      GETGOLFERBYNAME =
          if (qmark) setupGetGolferByName().replace(regEx, "?") else setupGetGolferByName()
      GETGOLFERBYNAMES =
          if (qmark) setupGetGolferByNames().replace(regEx, "?") else setupGetGolferByNames()
      INSERTGOLFER = if (qmark) setupInsertGolfer().replace(regEx, "?") else setupInsertGolfer()
      UPDATEGOLFER = if (qmark) setupUpdateGolfer().replace(regEx, "?") else setupUpdateGolfer()
      UPDATEGOLFERNAME =
          if (qmark) setupUpdateGolferName().replace(regEx, "?") else setupUpdateGolferName()
      UPDATEGOLFERHANDICAP =
          if (qmark) setupUpdateGolferHandicap().replace(regEx, "?")
          else setupUpdateGolferHandicap()
      DELETEGOLFER = if (qmark) setupDeleteGolfer().replace(regEx, "?") else setupDeleteGolfer()
      GETPUBLICGOLFERS =
          if (qmark) setupGetPublicGolfers().replace(regEx, "?")
          else setupGetPublicGolfers().replace("\"", "")
    }

    init {}

    @JvmStatic
    fun setupGetGolfer(): String {
      return create!!.renderNamedParams(
          select(
                  field("PIN"),
                  field("FIRST_NAME"),
                  field("LAST_NAME"),
                  field("HANDICAP"),
                  field("COUNTRY"),
                  field("STATE"),
                  field("OVERLAP_YEARS"),
                  field("PUBLIC"),
                  field("LAST_LOGIN")
              )
              .from(table("GOLFER"))
              .where(field("PIN").eq("$"))
      )
    }
    @JvmStatic
    fun setupGetGolferByName(): String {
      return create!!.renderNamedParams(
          select(
                  field("PIN"),
                  field("FIRST_NAME"),
                  field("LAST_NAME"),
                  field("HANDICAP"),
                  field("COUNTRY"),
                  field("STATE"),
                  field("OVERLAP_YEARS"),
                  field("PUBLIC"),
                  field("LAST_LOGIN")
              )
              .from(table("GOLFER"))
              .where(field("LAST_NAME").eq("$"))
      )
    }
    @JvmStatic
    fun setupGetGolferByNames(): String {
      return create!!.renderNamedParams(
          select(
                  field("PIN"),
                  field("FIRST_NAME"),
                  field("LAST_NAME"),
                  field("HANDICAP"),
                  field("COUNTRY"),
                  field("STATE"),
                  field("OVERLAP_YEARS"),
                  field("PUBLIC"),
                  field("LAST_LOGIN")
              )
              .from(table("GOLFER"))
              .where(field("LAST_NAME").eq("$"))
              .and(field("FIRST_NAME").eq("$"))
      )
    }
    @JvmStatic
    fun setupGetPublicGolfers(): String {
      return create!!.renderNamedParams(
          select(GOLFER.LAST_NAME, GOLFER.FIRST_NAME)
              .from(GOLFER)
              .where(GOLFER.PUBLIC.eq(0))
              .orderBy(GOLFER.LAST_NAME)
      )
    }
    @JvmStatic
    fun setupInsertGolfer(): String {
      return create!!.renderNamedParams(
          insertInto(table("GOLFER"))
              .columns(
                  field("FIRST_NAME"),
                  field("LAST_NAME"),
                  field("PIN"),
                  field("COUNTRY"),
                  field("STATE"),
                  field("OVERLAP_YEARS"),
                  field("PUBLIC"),
                  field("LAST_LOGIN")
              )
              .values("$", "$", "$", "S", "$", "$", "$", "$")
              .returning(field("PIN"))
      )
    }
    @JvmStatic
    fun setupUpdateGolferName(): String {
      return create!!.renderNamedParams(
          update(table("GOLFER"))
              .set(field("FIRST_NAME"), "$")
              .set(field("LAST_NAME"), "$")
              .where(field("pin").eq("$"))
      )
    }
    @JvmStatic
    fun setupUpdateGolfer(): String {
      return create!!.renderNamedParams(
          update(table("GOLFER"))
              .set(field("COUNTRY"), "$")
              .set(field("STATE"), "$")
              .set(field("OVERLAP_YEARS"), "$")
              .set(field("PUBLIC"), "$")
              .set(field("LAST_LOGIN"), "$")
              .where(field("pin").eq("$"))
      )
    }
    @JvmStatic
    fun setupUpdateGolferHandicap(): String {
      return create!!.renderNamedParams(
          update(table("GOLFER")).set(field("HANDICAP"), "$").where(field("pin").eq("$"))
      )
    }
    @JvmStatic
    fun setupDeleteGolfer(): String {
      return create!!.renderNamedParams(deleteFrom(table("GOLFER")).where(field("pin").eq("$")))
    }
  }

  @Throws(SQLException::class, InterruptedException::class)
  fun getGolfer(golfer: Golfer, cmd: Int): Future<Golfer> {
    val promise: Promise<Golfer> = Promise.promise()

    pool!!
        .getConnection()
        .doOnSuccess { conn ->
          val parameters: Tuple = Tuple.tuple()
          var sql: String? = GETGOLFER
          val golferPin: String? = golfer.pin
          if (golferPin!!.trim { it <= ' ' } != "") {
            parameters.addString(golfer.pin)
            /*
                If pin is missing try using first/last name with last course/tee/tee-date
            */
          } else if (golfer.lastName != null && golfer.firstName != null) {
            sql = GETGOLFERBYNAMES
            parameters.addString(golfer.lastName)
            parameters.addString(golfer.firstName)
          }

          conn.preparedQuery(sql)
              .rxExecute(parameters)
              .doOnSuccess { rows ->
                golfer.message = "Golfer not found"
                var golferClone: Golfer? = null
                for (row in rows) {
                  golferClone = golfer.clone() as Golfer
                  golfer.pin = row!!.getString(0) // "PIN")
                  golfer.firstName = row.getString(1) // "FIRST_NAME")
                  golfer.lastName = row.getString(2) // "LAST_NAME")
                  golfer.handicap = row.getFloat(3) // "HANDICAP")
                  golfer.country = row.getString(4) // "COUNTRY")
                  golfer.state = row.getString(5) // "STATE")
                  golfer.overlap = row.getInteger(6).equals(1) // "OVERLAP_YEARS")
                  golfer.public = row.getInteger(7).equals(1) // "PUBLIC")

                  if (DbConfiguration.isUsingMariadb()) {
                    val zdt: ZonedDateTime =
                        ZonedDateTime.of(row.getLocalDateTime(8), ZoneId.systemDefault())
                    val zdl: Long = zdt.toInstant().toEpochMilli()
                    golfer.lastLogin = zdl
                  } else {
                    golfer.lastLogin = row.getLong(8) // "LAST_LOGIN")
                  }
                  golfer.message = "Golfer not found"
                }
                conn.close()

                if (rows.size() == 0) {
                  if (golfer.firstName!!.length < 3 || golfer.lastName!!.length < 5) {
                    golfer.status = -1
                    var which: String
                    if (golfer.firstName!!.length < 3) {
                      which = "First"
                    } else {
                      which = "Last"
                    }
                    golfer.message = "${which} name required for new golfer."
                    promise.complete(golfer)
                  } else {
                    val future = addGolfer(golfer)
                    future
                        .onFailure { throwable ->
                          golfer.message = throwable.message
                          golfer.status = -2
                          promise.complete(golfer)
                        }
                        .onSuccess { resultGolfer -> promise.complete(resultGolfer) }
                  }
                } else {
                  val futureUpdate: Future<Golfer> = updateGolfer(golfer, golferClone, cmd)
                  futureUpdate
                      .onFailure { throwable ->
                        golfer.message = throwable.message
                        golfer.status = -3
                        promise.complete(golferClone)
                      }
                      .onSuccess { updateGolfer -> promise.complete(updateGolfer) }
                }
              }
              .doOnError { _ ->
                golfer.status = -1
                golfer.message = "Golfer query failed"
                conn.close()
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError querying Golfer - %s%s %s",
                            ColorUtilConstants.RED,
                            err,
                            ColorUtilConstants.RESET,
                            err.stackTraceToString()
                        )
                    )
                    promise.tryComplete(golfer)
                  }
              )
        }
        .subscribe()

    return promise.future()
  }

  @Throws(SQLException::class, InterruptedException::class)
  fun getGolfers(
      responseObserver: StreamObserver<ListPublicGolfers?>
  ): Future<StreamObserver<ListPublicGolfers?>> {
    val promise: Promise<StreamObserver<ListPublicGolfers?>> = Promise.promise()

    pool!!
        .getConnection()
        .doOnSuccess { conn ->
          val parameters: Tuple = Tuple.tuple()
          val golfersBuilder = ListPublicGolfers.newBuilder()

          val sql = GETPUBLICGOLFERS
          parameters.addInteger(1)

          conn.preparedQuery(sql)
              .rxExecute(parameters)
              .doOnSuccess { rows ->
                var golferBuilder: handicap.grpc.Golfer.Builder?

                for (row in rows) {
                  var concatName = row!!.getString(0) + ", " + row.getString(1)
                  golferBuilder = handicap.grpc.Golfer.newBuilder()

                  golferBuilder.name = concatName
                  golfersBuilder.addGolfer(golferBuilder)
                }
                conn.close()
                responseObserver.onNext(golfersBuilder.build())

                promise.complete(responseObserver)
              }
              .doOnError { _ ->
                conn.close()
                promise.complete(responseObserver)
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Querying Golfer - %s%s %s",
                            ColorUtilConstants.RED,
                            err,
                            ColorUtilConstants.RESET,
                            err.stackTraceToString()
                        )
                    )
                    promise.complete(responseObserver)
                  }
              )
        }
        .subscribe()

    return promise.future()
  }

  @Throws(SQLException::class, InterruptedException::class)
  fun addGolfer(golfer: Golfer): Future<Golfer> {
    val promise: Promise<Golfer> = Promise.promise()
    val localDateTime: LocalDateTime = LocalDateTime.now()

    if (golfer.pin == null || golfer.pin!!.length < 6) {
      golfer.status = -1
      golfer.message = "Valid Golfer Pin must be supplied"
      promise.complete(golfer)
      return promise.future()
    }

    pool!!
        .getConnection()
        .doOnSuccess { conn ->
          val parameters: Tuple = Tuple.tuple()
          val ldt = LocalDateTime.now()
          val milliSeconds =
              ZonedDateTime.of(ldt, ZoneId.systemDefault()).toInstant().toEpochMilli()
          var overlap = 0
          var public = 0
          if (DbConfiguration.isUsingPostgres()) {
            overlap = if (golfer.overlap) 1 else 0
            public = if (golfer.overlap) 1 else 0
          }
          parameters
              .addString(golfer.firstName)
              .addString(golfer.lastName)
              .addString(golfer.pin)
              .addString(golfer.country)
              .addString(golfer.state)
          if (DbConfiguration.isUsingPostgres()) {
            parameters.addInteger(overlap).addInteger(public)
          } else {
            parameters.addBoolean(golfer.overlap).addBoolean(golfer.public)
          }
          if (DbConfiguration.isUsingMariadb()) parameters.addValue(ldt)
          else parameters.addValue(milliSeconds)

          val sql: String? = INSERTGOLFER
          val zdt: ZonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
          golfer.lastLogin = zdt.toInstant().toEpochMilli()

          conn.preparedQuery(sql)
              .rxExecute(parameters)
              .doOnSuccess { _ ->
                conn.close()
                golfer.message = "Golfer added"
                promise.tryComplete(golfer)
              }
              .doOnError { _ ->
                golfer.status = -1
                golfer.message = "Golfer add failed"
                conn.close()
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Adding Golfer - %s%s %s",
                            ColorUtilConstants.RED,
                            err,
                            ColorUtilConstants.RESET,
                            err.stackTraceToString()
                        )
                    )
                    promise.tryComplete(golfer)
                  }
              )
        }
        .subscribe()

    return promise.future()
  }

  fun updateGolfer(golfer: Golfer, golferClone: Golfer?, cmd: Int): Future<Golfer> {
    val promise: Promise<Golfer> = Promise.promise()
    var isLogin = cmd == 3 || golferClone?.pin?.length == 0

    pool!!
        .getConnection()
        .doOnSuccess { conn ->
          val parameters: Tuple = Tuple.tuple()
          val ldt = LocalDateTime.now()
          val milliSeconds =
              ZonedDateTime.of(ldt, ZoneId.systemDefault()).toInstant().toEpochMilli()
          var overlap = if (isLogin) golfer.overlap else golferClone?.overlap
          var public = if (isLogin) golfer.public else golferClone?.public

          parameters
              .addString(if (isLogin) golfer.country else golferClone?.country)
              .addString(if (isLogin) golfer.state else golferClone?.state)
              .addInteger(if (overlap!!) 1 else 0)
              .addInteger(if (public!!) 1 else 0)
          if (DbConfiguration.isUsingMariadb()) {
            parameters.addValue(ldt)
          } else {
            parameters.addLong(milliSeconds)
          }
          parameters.addString(golfer.pin)

          val sql: String? = UPDATEGOLFER

          conn.preparedQuery(sql)
              .rxExecute(parameters)
              .doOnSuccess { _ ->
                conn.close()
                if (!isLogin) {
                  golfer.message = "Golfer updated"
                  golfer.country = golferClone?.country
                  golfer.state = golferClone?.state
                  golfer.overlap = golferClone?.overlap == true
                  golfer.public = golferClone?.public == true
                }
                golfer.lastLogin = milliSeconds
                promise.tryComplete(golfer)
              }
              .doOnError { _ ->
                golfer.status = -1
                golfer.message = "Golfer update failed"
                conn.close()
              }
              .subscribe(
                  {},
                  { err ->
                    LOGGER.severe(
                        String.format(
                            "%sError Updating Golfer - %s%s %s",
                            ColorUtilConstants.RED,
                            err,
                            ColorUtilConstants.RESET,
                            err.stackTraceToString()
                        )
                    )
                    if (!promise.future().isComplete) {
                      promise.complete(golfer)
                    }
                  }
              )
        }
        .subscribe()

    return promise.future()
  }
}
