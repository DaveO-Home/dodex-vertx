/*
 * This file is generated by jOOQ.
 */
package golf.handicap.generated.tables.records


import golf.handicap.generated.tables.Golfer

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record9
import org.jooq.Row9
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class GolferRecord() : UpdatableRecordImpl<GolferRecord>(Golfer.GOLFER), Record9<String?, String?, String?, Float?, String?, String?, Boolean?, Boolean?, Long?> {

    open var pin: String?
        set(value): Unit = set(0, value)
        get(): String? = get(0) as String?

    open var firstName: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var lastName: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var handicap: Float?
        set(value): Unit = set(3, value)
        get(): Float? = get(3) as Float?

    open var country: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    open var state: String?
        set(value): Unit = set(5, value)
        get(): String? = get(5) as String?

    open var overlapYears: Boolean?
        set(value): Unit = set(6, value)
        get(): Boolean? = get(6) as Boolean?

    open var `public`: Boolean?
        set(value): Unit = set(7, value)
        get(): Boolean? = get(7) as Boolean?

    open var lastLogin: Long?
        set(value): Unit = set(8, value)
        get(): Long? = get(8) as Long?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<String?> = super.key() as Record1<String?>

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row9<String?, String?, String?, Float?, String?, String?, Boolean?, Boolean?, Long?> = super.fieldsRow() as Row9<String?, String?, String?, Float?, String?, String?, Boolean?, Boolean?, Long?>
    override fun valuesRow(): Row9<String?, String?, String?, Float?, String?, String?, Boolean?, Boolean?, Long?> = super.valuesRow() as Row9<String?, String?, String?, Float?, String?, String?, Boolean?, Boolean?, Long?>
    override fun field1(): Field<String?> = Golfer.GOLFER.PIN
    override fun field2(): Field<String?> = Golfer.GOLFER.FIRST_NAME
    override fun field3(): Field<String?> = Golfer.GOLFER.LAST_NAME
    override fun field4(): Field<Float?> = Golfer.GOLFER.HANDICAP
    override fun field5(): Field<String?> = Golfer.GOLFER.COUNTRY
    override fun field6(): Field<String?> = Golfer.GOLFER.STATE
    override fun field7(): Field<Boolean?> = Golfer.GOLFER.OVERLAP_YEARS
    override fun field8(): Field<Boolean?> = Golfer.GOLFER.PUBLIC
    override fun field9(): Field<Long?> = Golfer.GOLFER.LAST_LOGIN
    override fun component1(): String? = pin
    override fun component2(): String? = firstName
    override fun component3(): String? = lastName
    override fun component4(): Float? = handicap
    override fun component5(): String? = country
    override fun component6(): String? = state
    override fun component7(): Boolean? = overlapYears
    override fun component8(): Boolean? = `public`
    override fun component9(): Long? = lastLogin
    override fun value1(): String? = pin
    override fun value2(): String? = firstName
    override fun value3(): String? = lastName
    override fun value4(): Float? = handicap
    override fun value5(): String? = country
    override fun value6(): String? = state
    override fun value7(): Boolean? = overlapYears
    override fun value8(): Boolean? = `public`
    override fun value9(): Long? = lastLogin

    override fun value1(value: String?): GolferRecord {
        this.pin = value
        return this
    }

    override fun value2(value: String?): GolferRecord {
        this.firstName = value
        return this
    }

    override fun value3(value: String?): GolferRecord {
        this.lastName = value
        return this
    }

    override fun value4(value: Float?): GolferRecord {
        this.handicap = value
        return this
    }

    override fun value5(value: String?): GolferRecord {
        this.country = value
        return this
    }

    override fun value6(value: String?): GolferRecord {
        this.state = value
        return this
    }

    override fun value7(value: Boolean?): GolferRecord {
        this.overlapYears = value
        return this
    }

    override fun value8(value: Boolean?): GolferRecord {
        this.`public` = value
        return this
    }

    override fun value9(value: Long?): GolferRecord {
        this.lastLogin = value
        return this
    }

    override fun values(value1: String?, value2: String?, value3: String?, value4: Float?, value5: String?, value6: String?, value7: Boolean?, value8: Boolean?, value9: Long?): GolferRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        this.value7(value7)
        this.value8(value8)
        this.value9(value9)
        return this
    }

    /**
     * Create a detached, initialised GolferRecord
     */
    constructor(pin: String? = null, firstName: String? = null, lastName: String? = null, handicap: Float? = null, country: String? = null, state: String? = null, overlapYears: Boolean? = null, `public`: Boolean? = null, lastLogin: Long? = null): this() {
        this.pin = pin
        this.firstName = firstName
        this.lastName = lastName
        this.handicap = handicap
        this.country = country
        this.state = state
        this.overlapYears = overlapYears
        this.`public` = `public`
        this.lastLogin = lastLogin
    }
}
