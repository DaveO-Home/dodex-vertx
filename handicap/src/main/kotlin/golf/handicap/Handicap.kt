/*
 * Handicap.java
 *
 * Created on January 26, 2005, 1:07 PM
 */
package golf.handicap

// import golf.handicap.PopulateGolferScores.getGolferScores
// import golf.handicap.PopulateGolferScores.setGolferHandicap
// import golf.handicap.PopulateGolferScores.clearUsed
// import golf.handicap.PopulateGolferScores.setUsed
import golf.handicap.Handicap.Used
import golf.handicap.Golfer
import golf.handicap.db.PopulateGolferScores
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Logger
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.rxjava3.core.Promise
import io.vertx.rxjava3.core.eventbus.Message
import io.vertx.kotlin.coroutines.awaitResult
import kotlin.jvm.internal.iterator
import dmo.fs.utils.ColorUtilConstants
import com.google.protobuf.AnyOrBuilder


class Handicap
{
  companion object {
    private val LOGGER = Logger.getLogger(Handicap::class.java.name)
  }
    var dateTime: Date? = null
    var diffkeys = Array<String>(20, {" "})
    var index = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 9, 10)
    var diffScores = FloatArray(20) { -100f }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    var multiple = .96.toFloat()
    var used: MutableMap<String, Used> = HashMap<String, Used>()
    var teeTime: String? = null
    var key: String = String()
    var usedClass: Used? = null
    
    fun getHandicap(golfer: Golfer): Future<MutableMap<String, Any>> {
        val handicapPromise = Promise.promise<MutableMap<String, Any>>()
        val averageSlope = 113
        val golferScores = PopulateGolferScores()

        var numberOfScores = -1
        var adjusted: Float
        var rating: Float
        var slope: Float
        var difference: Float
        var handicapDifferential: Float
        var handicap: BigDecimal
        var handicapData: Future<Map<String, Any?>>?
        val latestTee: MutableMap<String, Any> = HashMap()

        handicapData = golferScores.getGolferScores(golfer)
        
        handicapData!!.onSuccess{ map ->
        if(map.size == 0) {
          latestTee.put("handicap", 0.0f)
          handicapPromise.complete(latestTee)
        } 
        else {
        val dataArray: JsonArray = map.get("array") as JsonArray
        val objects = dataArray.iterator()
            
        var i = 0
        do {
            val jsonObject: JsonObject = objects.next() as JsonObject

            if(jsonObject.getInteger("ADJUSTED_SCORE") != null) {
              adjusted = jsonObject.getInteger("ADJUSTED_SCORE").toFloat()
              rating = jsonObject.getDouble("TEE_RATING").toFloat()
              slope = jsonObject.getInteger("TEE_SLOPE").toFloat()
              difference = adjusted - rating
              handicapDifferential = averageSlope * difference / slope
              // Used to calculate Net Score
              if(i == 0) {
                latestTee.put("adjusted", adjusted)
                latestTee.put("rating", rating)
                latestTee.put("slope", slope)
                latestTee.put("par", jsonObject.getInteger("TEE_PAR"))
              }

              var foundZero = false
              for (x in 0..19) {
                  if (diffScores[x] == -100.0f) {
                      diffScores[x] = handicapDifferential
                      foundZero = true
                      break
                  }
              }

              if (foundZero) ; else if (handicapDifferential < diffScores[19]) {
                    diffScores[19] = handicapDifferential
              }

              Arrays.sort(diffScores)

              val localDateTime = LocalDateTime.parse(jsonObject.getString("TEE_TIME"))
              key = java.lang.Float.toString(handicapDifferential) + '\t' + formatter.format(
                  localDateTime
              )
              key = if (key.indexOf('.') == 1) "0$key" else key
              diffkeys[i] = key
              used[key] = Used(
                  jsonObject.getString("PIN"),
                  jsonObject.getInteger("COURSE_SEQ"),
                  jsonObject.getString("TEE_TIME")
              )

              if (numberOfScores < 19) numberOfScores++
              i++
            }
        } while(objects.hasNext())

        Arrays.sort(diffkeys)
        handicap = calculateHandicap(numberOfScores)
        golfer.handicap = handicap.toFloat()
        golferScores.setGolferHandicap(golfer).onSuccess{_ ->
        var rowsUpdated = 0
        usedClass = used[diffkeys[19]]

        golferScores.clearUsed(usedClass!!.pin).onSuccess{count ->
          rowsUpdated += count  

          var idx = 19
          while (idx > -1 && diffkeys[idx] != "") {
            usedClass = used[diffkeys[idx]]
            if (usedClass != null && usedClass!!.used) { 
              golferScores.setUsed(usedClass!!.pin, usedClass!!.course!!, usedClass!!.teeTime)
                .onSuccess{usedCount -> 
                  rowsUpdated += usedCount
                }
                .onFailure{err -> 
                  LOGGER.severe(
                    String.format(
                        "%sError Setting Used - %s%s",
                        ColorUtilConstants.RED,
                        err.message,
                        ColorUtilConstants.RESET
                    )
                  )
                }
            }
            idx--
          }
        }
        .onFailure{err -> 
          LOGGER.severe(
            String.format(
                "%sError Clearing Used - %s%s",
                ColorUtilConstants.RED,
                err.message,
                ColorUtilConstants.RESET
            )
          )
        }
      }
      latestTee.put("handicap", handicap.toFloat())
      handicapPromise.complete(latestTee)
      }
    }

      return  handicapPromise.future()
    }

    private fun calculateHandicap(numberOfScores: Int): BigDecimal {
        if (diffScores[15] == -100.0f) return BigDecimal(0.0)
        var total = 0.0.toFloat()
        var i = 0
        val j = index[numberOfScores]

        while (i < j) {
            total = total + diffScores[19 - numberOfScores + i]
            setUsed(diffkeys[19 - numberOfScores + i]) 
            i++
        }
        total = total / index[numberOfScores] * multiple
        return BigDecimal(total.toString()).setScale(1, RoundingMode.DOWN)
    }

    private fun setUsed(diffkey: String) {
        usedClass = used[diffkey]
        if (usedClass != null) {
            usedClass!!.used = true
            used[diffkey] = usedClass as Used
        }
    }

    inner class Used(pin: String?, course: Int?, teeTime: String) {
        var pin: String?
        var course: Int?
        var teeTime: String
        var used = false
        override fun toString(): String {
            return StringBuffer().append(pin).append(" ").append(course).append(" ")
                .append(teeTime).append(" ").append(used)
                .toString()
        }

        init {
            this.pin = pin
            this.course = course
            this.teeTime = teeTime
        }
    }
}