package golf.handicap.routes

import dmo.fs.dbh.emf.DodexEntityManager
import golf.handicap.Golfer
import golf.handicap.Score
import golf.handicap.hibernate.Handicap
import golf.handicap.hibernate.db.PopulateCourse
import golf.handicap.hibernate.db.PopulateGolfer
import golf.handicap.hibernate.db.PopulateGolferScores
import golf.handicap.hibernate.db.PopulateScore
import golf.handicap.vertx.HandicapGrpcServer
import handicap.grpc.*
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.grpc.server.Service
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.*

class GrpcHibernateService : HandicapIndexGrpcService(), Service {
    private val logger = LoggerFactory.getLogger(GrpcHibernateService::class.java.name)
    private var sessionFactory: SessionFactory

    init {
        DodexEntityManager()
        sessionFactory = DodexEntityManager.getEmf()
    }

    override fun getGolfer(request: HandicapSetup) : Future<HandicapData?> {
        val requestJson = JsonObject(request.json)
        val promise = Promise.promise<HandicapData?>()
        val handicapGolfer = requestJson.mapTo(Golfer::class.java)
        var responseObject = JsonObject()

        try {
            val currentGolfer: Golfer =
                PopulateGolfer().getGolfer(handicapGolfer, request.cmd, sessionFactory)
            responseObject = JsonObject.mapFrom(currentGolfer)
            requestJson.remove("status")
            requestJson.put("status", currentGolfer.status)

            if (true == HandicapGrpcServer.enableHandicapAdmin) {
                responseObject
                    .put("admin", HandicapGrpcServer.handicapAdminPin)
                    .put("adminstatus", 10)
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }

        promise.complete(HandicapData.newBuilder()
            .setMessage("success")
            .setCmd(request.cmd)
            .setJson(responseObject.toString())
            .build())

        return promise.future()
    }

    override fun addRating(request: Command) : Future<HandicapData?> {
        val json = request.getJson()
        val promise = Promise.promise<HandicapData?>()
        val populateCourse = PopulateCourse()
        val ratingMap: HashMap<String, Any> = populateCourse.getRatingMap(json)
        val handicapData: HandicapData? = populateCourse.getCourseWithTee(ratingMap, sessionFactory)

        promise.complete(handicapData)

        return promise.future()
    }

    override fun addScore(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val requestJson = JsonObject(request.getJson())
        val golferScore: Score = requestJson.mapTo(Score::class.java)
        val populateScore = PopulateScore()
        val golfer = Objects.requireNonNull<Golfer?>(golferScore.golfer)
        val handicap = Handicap()

        populateScore.setScore(golferScore, sessionFactory)

        val latestTee: MutableMap<String, Any> = handicap.getHandicap(golfer!!, sessionFactory)

        val newHandicap: Float = latestTee["handicap"] as Float
        val slope: Float = latestTee["slope"] as Float
        val rating: Float = latestTee["rating"] as Float
        val par: Int = latestTee["par"] as Int
        golferScore.handicap = newHandicap
        val courseHandicap: Float = newHandicap * slope / 113 + (rating - par)
        golferScore.netScore = golferScore.grossScore.toFloat() - courseHandicap
        golferScore.golfer!!.handicap = newHandicap

        val jsonData: String = populateScore.setScore(golferScore, sessionFactory)

        promise.complete(
            HandicapData.newBuilder()
                .setMessage("Success")
                .setCmd(request.cmd)
                .setJson(jsonData)
                .build()
        )

        return promise.future()
    }

    override fun golferScores(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val populateScores = PopulateGolferScores()
        val requestJson = JsonObject(request.getJson())
        val golfer: Golfer = requestJson.mapTo(Golfer::class.java)

        if (request.cmd == 10) {
            val names: Array<String?> =
                request.getKey().split("&#44;".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            golfer.lastName = names[0]
            golfer.firstName = if (names.size > 1) names[1]!!.trim { it <= ' ' } else ""
            golfer.pin = ""
        }

        val scoresMap: Map<String, Any?> = populateScores.getGolferScores(golfer, 365, sessionFactory)
        promise.complete(
            HandicapData.newBuilder()
                .setMessage("Success")
                .setCmd(request.cmd)
                .setJson(scoresMap["array"].toString())
                .build()
        )

        return promise.future()
    }

    override fun removeScore(request: Command): Future<HandicapData?> {
        val promise = Promise.promise<HandicapData?>()
        val populateGolferScores = PopulateGolferScores()
        val requestJson: JsonObject = JsonObject(request.getJson())
        val golfer: Golfer = requestJson.mapTo(Golfer::class.java)

        if (golfer.pin != null) {
            val used: String = populateGolferScores.removeLastScore(request.getKey(), sessionFactory)

            val handicap = Handicap()
            val latestTee: MutableMap<String, Any> = handicap.getHandicap(golfer, sessionFactory)


            val handicapValue = latestTee["handicap"].toString().toFloat()
            golfer.handicap = handicapValue
            golfer.status = used.toInt()

            val golferJson: String? = JsonObject.mapFrom(golfer).toString()

            promise.complete(HandicapData.newBuilder()
                .setMessage("Success")
                .setCmd(request.cmd)
                .setJson(golferJson)
                .build()
            )
        } else {
            promise.complete(null)
        }

        return promise.future()
    }

    override fun listCourses(
        request: Command
    ): Future<ListCoursesResponse?> {
        val promise = Promise.promise<ListCoursesResponse?>()
        val populateCourse = PopulateCourse()
        val coursesBuilder: ListCoursesResponse.Builder =
            populateCourse.listCourses(request.getKey(), sessionFactory)

        promise.complete(coursesBuilder.build())

        return promise.future()
    }

    override fun listGolfers(
        request: Command
    ): Future<ListPublicGolfers?> {
        val promise = Promise.promise<ListPublicGolfers?>()
        val populateGolfer = PopulateGolfer()

        val golfersBuilder: ListPublicGolfers.Builder = populateGolfer.getGolfers(sessionFactory)
        val listPublicGolfers = golfersBuilder.build()

        promise.complete(listPublicGolfers)

        return promise.future()
    }
}