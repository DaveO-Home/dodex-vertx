package golf.handicap.hibernate.entities

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime

@Entity(name = "PersistedScores")
@Table(name = "scores")
@Access(AccessType.FIELD)
class PersistedScores : Serializable {
    @EmbeddedId
    var scoresId: ScoresId? = null

    @Basic(optional = false)
    @Column(name = "PIN", nullable = false, insertable = false, updatable = false, length = 8)
    var pin: String? = null

    @Basic(optional = false)
    @Column(name = "GROSS_SCORE", nullable = false)
    var grossScore: Int? = null

    @Basic(optional = false)
    @Column(name = "NET_SCORE", nullable = true)
    var netScore: Float? = null

    @Basic(optional = false)
    @Column(name = "ADJUSTED_SCORE", nullable = false)
    var adjustedScore: Int? = null

    @Basic(optional = false)
    @Column(name = "TEE_TIME", nullable = false, insertable = false, updatable = false)
    var teeTime: LocalDateTime? = null

    @Basic(optional = false)
    @Column(name = "HANDICAP", nullable = true)
    var handicap: Float? = null

    @Basic(optional = false)
    @Column(name = "COURSE_SEQ", nullable = false, insertable = false, updatable = false)
    var courseSeq: Int? = 0

    @Basic(optional = false)
    @Column(name = "COURSE_TEES", nullable = false)
    var courseTees: Int? = null

    @Basic(optional = false)
    @Column(name = "USED", nullable = false)
    var used: Char? = null

    @OneToOne(cascade = [CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH], fetch = FetchType.LAZY)
    @JoinColumn(name = "COURSE_SEQ", insertable = false, updatable = false)
    var course: PersistedCourse? = null

    @Embeddable
    class ScoresId : Serializable {
        @Basic(optional = false)
        @Column(name = "PIN", nullable = false, updatable = false)
        var pin: String? = null

        @Basic(optional = false)
        @Column(name = "COURSE_SEQ", nullable = false, updatable = false)
        var courseSeq: Int? = 0

        @Basic(optional = false)
        @Column(name = "TEE_TIME", nullable = false, updatable = false)
        var teeTime: LocalDateTime? = null

        constructor()

        override fun hashCode(): Int {
            return (LocalDateTime.now().second / courseSeq!!) * "&".hashCode() * "$".hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other !is ScoresId) return false
            return false
        }

        constructor(pin: String?, courseSeq: Int?, teeTime: LocalDateTime) {
            this.pin = pin
            this.courseSeq = courseSeq
            this.teeTime = teeTime
        }
    }

    companion object {
    }
}
