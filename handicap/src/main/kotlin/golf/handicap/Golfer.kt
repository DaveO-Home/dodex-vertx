package golf.handicap

class Golfer : Cloneable {
    companion object {}
    var firstName: String? = null
    var lastName: String? = null
    var pin: String? = null
    var country: String? = null
    var state: String? = null
    var handicap = 0.0.toFloat()
    var score: Score? = null
    var overlap: Boolean = true
    var public: Boolean = false
    var status: Int = 0
    var lastLogin: Long? = null
    var course: String? = null
    var tee: Int = 0
    var teeDate: Long? = null
    var message: String? = null

    public override fun clone(): Any {
        return super.clone()
    }
    override fun toString(): String {
        return "Golfer=$firstName $lastName Pin=$pin Handicap=$handicap Country=$country State=$state Overlap=$overlap Public=$public"
    }
}
