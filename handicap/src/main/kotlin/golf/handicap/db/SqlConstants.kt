package golf.handicap.db

import io.vertx.rxjava3.sqlclient.Pool
import org.jooq.DSLContext
import java.sql.SQLException

open class SqlConstants {
    companion object {
        @JvmField var qmark: Boolean = true
        @JvmField var GETGOLFER: String? = null
        @JvmField var GETGOLFERBYNAME: String? = null
        @JvmField var GETGOLFERBYNAMES: String? = null
        @JvmField var INSERTGOLFER: String? = null
        @JvmField var UPDATEGOLFER: String? = null
        @JvmField var UPDATEGOLFERNAME: String? = null
        @JvmField var UPDATEGOLFERHANDICAP: String? = null
        @JvmField var DELETEGOLFER: String? = null
        @JvmField var GETCOURSESBYSTATE: String? = null
        @JvmField var GETCOURSEBYNAME: String? = null
        @JvmField var GETCOURSEBYTEE: String? = null
        @JvmField var GETCOURSEINSERT: String? = null
        @JvmField var GETRATINGINSERT: String? = null
        @JvmField var GETRATINGUPDATE: String? = null
        @JvmField var GETSQLITERATINGUPDATE: String? = null
        @JvmField var GETSCOREINSERT: String? = null
        @JvmField var GETSCOREUPDATE: String? = null
        @JvmField var GETSCOREBYTEETIME: String? = null
        @JvmField var GETGOLFERUPDATECHECKED: String? = null
        @JvmField var GETSETUSEDUPDATE: String? = null
        @JvmField var GETSETUSEDSQLITEUPDATE: String? = null
        @JvmField var GETRESETUSEDUPDATE: String? = null
        @JvmField var GETRESETUSEDSQLITEUPDATE: String? = null
        @JvmField var GETHANDICAPUPDATE: String? = null
        @JvmField var GETHANDICAPSQLITEUPDATE: String? = null
        @JvmField var GETSCORESUPDATE: String? = null
        @JvmField var GETSCORESSQLITEUPDATE: String? = null
        @JvmField var GETGOLFERDATA: String? = null
        @JvmField var GETGOLFERPUBLICDATA: String? = null
        @JvmField var GETREMOVESCORE: String? = null
        @JvmField var GETREMOVESCORESUB: String? = null
        @JvmField var GETLASTSCORE: String? = null
        @JvmField var GETGOLFERSCORES: String? = null
        @JvmField var GETPUBLICGOLFERS: String? = null
        @JvmField var pool: Pool? = null
        @JvmField var create: DSLContext? = null
        @Throws(NullPointerException::class)
        @JvmStatic
        public fun setQMark(qmark: Boolean) {
            this.qmark = qmark
        }

        @Throws(SQLException::class)
        @JvmStatic
        public fun setSqlPool(pool: Pool?) {
            this.pool = pool
        }

        @Throws(SQLException::class, NullPointerException::class)
        @JvmStatic
        public fun setDslContext(create: DSLContext?) {
            this.create = create
        }
    }
}