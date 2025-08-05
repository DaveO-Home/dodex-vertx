package golf.handicap.hibernate.entities

import jakarta.persistence.*
import java.io.Serializable

@Cacheable(true)
@Entity(name = "PersistedCourse")
@Table(name = "course")
class PersistedCourse : Serializable {
    @Id
    @Column(name = "COURSE_SEQ", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "courseSeq", sequenceName = "COURSE_SEQ", allocationSize = 50)
    var courseSeq: Int = 0

    @Basic(optional = false)
    @Column(name = "COURSE_NAME", nullable = false)
    var courseName: String? = null

    @Basic(optional = false)
    @Column(name = "COURSE_COUNTRY", nullable = false)
    var courseCountry: String? = null

    @Basic(optional = false)
    @Column(name = "COURSE_STATE", nullable = false)
    var courseState: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval=false, targetEntity=PersistedRatings::class, fetch=FetchType.EAGER)
    @JoinColumn(
        name = "COURSE_SEQ",
        nullable = false,
        insertable = false,
        updatable = false,
        referencedColumnName = "COURSE_SEQ"
    )

    var ratings: Set<PersistedRatings> = setOf()
}
