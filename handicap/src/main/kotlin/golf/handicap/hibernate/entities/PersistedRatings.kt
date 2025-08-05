package golf.handicap.hibernate.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime

@Cacheable(true)
@Entity(name = "PersistedRatings")
@Table(name = "ratings")
@Access(AccessType.FIELD)
class PersistedRatings : Serializable {
    @EmbeddedId
    var ratingsId: RatingsId? = null

    @Basic(optional = false)
    @Column(name = "COURSE_SEQ", nullable = false, insertable = false, updatable = false)
    var course_seq: Int? = null

    @Basic(optional = false)
    @Column(name = "TEE", nullable = false, insertable = false, updatable = false)
    var tee: Int? = null

    @Basic(optional = false)
    @Column(name = "TEE_COLOR", nullable = false)
    var teeColor: String? = null

    @Basic(optional = false)
    @Column(name = "TEE_RATING", nullable = false)
    var teeRating: Float? = null

    @Basic(optional = false)
    @Column(name = "TEE_SLOPE", nullable = false)
    var teeSlope: Int? = null

    @Basic(optional = false)
    @Column(name = "TEE_PAR", nullable = false)
    var teePar: Int? = null

    @Embeddable
    public class RatingsId : Serializable {
        @Basic(optional = false)
        @Column(name = "COURSE_SEQ", nullable = false, updatable = false)
        private var courseSeq: Int? = 0

        @Basic(optional = false)
        @Column(name = "TEE", nullable = false, updatable = false)
        var tee: Int? = 0

        constructor()

        override fun hashCode(): Int {
            return (courseSeq!! / tee!!) * "&".hashCode() * "$".hashCode() * LocalDateTime.now().second
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other !is RatingsId) return false
            return false
        }

        constructor(courseSeq: Int?, tee: Int?) {
            this.courseSeq = courseSeq
            this.tee = tee
        }
    }

    companion object {
    }
}
