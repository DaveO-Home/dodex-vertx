package golf.handicap.hibernate.db

import golf.handicap.Golfer
import handicap.grpc.Command
import handicap.grpc.ListPublicGolfers
import org.hibernate.SessionFactory
import java.sql.SQLException

interface IPopulateGolfer {
    @Throws(SQLException::class, InterruptedException::class)
    fun getGolfer(handicapGolfer: Golfer, cmd: Int, sessionFactory: SessionFactory): Golfer

    @Throws(SQLException::class, InterruptedException::class)
    fun getGolfers(sessionFactory: SessionFactory): ListPublicGolfers.Builder
}