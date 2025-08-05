package golf.handicap.hibernate.entities

import jakarta.persistence.*
import java.io.Serial
import java.io.Serializable
import java.time.LocalDateTime

@Entity(name = "PersistedGolfer")
@Table(name = "golfer")
class PersistedGolfer : Serializable {

    @EmbeddedId
    var golferId: GolferId? = null

    @Basic(optional = false)
    @Column(name = "PIN", nullable = false, insertable = false, updatable = false, length = 8)
    var pin: String? = null

    @Basic(optional = false)
    @Column(name = "FIRST_NAME", nullable = false)
    var firstName: String? = null

    @Basic(optional = false)
    @Column(name = "LAST_NAME", nullable = false)
    var lastName: String? = null

    @Basic(optional = false)
    @Column(name = "HANDICAP", nullable = false)
    var handicap: Float? = null

    @Basic(optional = false)
    @Column(name = "COUNTRY", nullable = false)
    var country: String? = null

    @Basic(optional = false)
    @Column(name = "STATE", nullable = false)
    var state: String? = null

    @Basic(optional = false)
    @Column(name = "OVERLAP_YEARS", nullable = false)
    var overlapYears: Boolean? = null

    @Basic(optional = false)
    @Column(name = "PUBLIC_DISPLAY", nullable = false)
    var publicDisplay: Boolean? = null

    @Basic(optional = false)
    @Column(name = "LAST_LOGIN")
    var lastLogin: LocalDateTime? = null

    @OneToMany(
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY, targetEntity = PersistedScores::class, orphanRemoval = true
    )
    @JoinColumn(name = "PIN")
    var scores: Set<PersistedScores>? = setOf()

    companion object {
        @Serial
        private const val serialVersionUID = 1L
    }

    @Embeddable
    class GolferId : Serializable {
        @Basic(optional = false)
        @Column(name = "PIN", nullable = false, updatable = false)
        private var pin: String = ""


        constructor()

        override fun hashCode(): Int {
            return (77).toInt() * "&".hashCode() * "$".hashCode() * LocalDateTime.now().second
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other !is GolferId) return false
            return false
        }

        override fun toString(): String {
            return pin
        }

        constructor(pin: String) {
            this.pin = pin
        }
    }
}