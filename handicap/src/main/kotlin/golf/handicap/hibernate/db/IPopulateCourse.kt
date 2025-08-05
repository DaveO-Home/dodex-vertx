package golf.handicap.hibernate.db

//import io.smallrye.mutiny.Uni
import handicap.grpc.HandicapData
import handicap.grpc.ListCoursesResponse
import org.hibernate.SessionFactory
import java.sql.SQLException

interface IPopulateCourse {

    @Throws(SQLException::class, InterruptedException::class)
    fun getCourseWithTee(
        courseMap: java.util.HashMap<String, Any>,
        sessionFactory: SessionFactory
    ): HandicapData?

    fun listCourses(
        state: String, sessionFactory: SessionFactory
    ): ListCoursesResponse.Builder

    fun getCourses(
        state: String, sessionFactory: SessionFactory
    ): String
}