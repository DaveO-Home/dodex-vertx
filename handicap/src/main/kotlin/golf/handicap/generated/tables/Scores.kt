/*
 * This file is generated by jOOQ.
 */
package golf.handicap.generated.tables


import golf.handicap.generated.DefaultSchema
import golf.handicap.generated.keys.SCORES__FK_SCORES_PK_COURSE
import golf.handicap.generated.keys.SCORES__FK_SCORES_PK_GOLFER
import golf.handicap.generated.tables.records.ScoresRecord

import java.util.function.Function

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row9
import org.jooq.Schema
import org.jooq.SelectField
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Scores(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, ScoresRecord>?,
    aliased: Table<ScoresRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<ScoresRecord>(
    alias,
    DefaultSchema.DEFAULT_SCHEMA,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object {

        /**
         * The reference instance of <code>SCORES</code>
         */
        val SCORES: Scores = Scores()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<ScoresRecord> = ScoresRecord::class.java

    /**
     * The column <code>SCORES.PIN</code>.
     */
    val PIN: TableField<ScoresRecord, String?> = createField(DSL.name("PIN"), SQLDataType.CHAR(8).nullable(false), this, "")

    /**
     * The column <code>SCORES.GROSS_SCORE</code>.
     */
    val GROSS_SCORE: TableField<ScoresRecord, Int?> = createField(DSL.name("GROSS_SCORE"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>SCORES.NET_SCORE</code>.
     */
    val NET_SCORE: TableField<ScoresRecord, Float?> = createField(DSL.name("NET_SCORE"), SQLDataType.REAL, this, "")

    /**
     * The column <code>SCORES.ADJUSTED_SCORE</code>.
     */
    val ADJUSTED_SCORE: TableField<ScoresRecord, Int?> = createField(DSL.name("ADJUSTED_SCORE"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>SCORES.TEE_TIME</code>.
     */
    val TEE_TIME: TableField<ScoresRecord, String?> = createField(DSL.name("TEE_TIME"), SQLDataType.CLOB.nullable(false), this, "")

    /**
     * The column <code>SCORES.HANDICAP</code>.
     */
    val HANDICAP: TableField<ScoresRecord, Float?> = createField(DSL.name("HANDICAP"), SQLDataType.REAL, this, "")

    /**
     * The column <code>SCORES.COURSE_SEQ</code>.
     */
    val COURSE_SEQ: TableField<ScoresRecord, Int?> = createField(DSL.name("COURSE_SEQ"), SQLDataType.INTEGER, this, "")

    /**
     * The column <code>SCORES.COURSE_TEES</code>.
     */
    val COURSE_TEES: TableField<ScoresRecord, Int?> = createField(DSL.name("COURSE_TEES"), SQLDataType.INTEGER, this, "")

    /**
     * The column <code>SCORES.USED</code>.
     */
    val USED: TableField<ScoresRecord, String?> = createField(DSL.name("USED"), SQLDataType.CHAR(1), this, "")

    private constructor(alias: Name, aliased: Table<ScoresRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<ScoresRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>SCORES</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>SCORES</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>SCORES</code> table reference
     */
    constructor(): this(DSL.name("SCORES"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, ScoresRecord>): this(Internal.createPathAlias(child, key), child, key, SCORES, null)
    override fun getSchema(): Schema? = if (aliased()) null else DefaultSchema.DEFAULT_SCHEMA
    override fun getReferences(): List<ForeignKey<ScoresRecord, *>> = listOf(SCORES__FK_SCORES_PK_GOLFER, SCORES__FK_SCORES_PK_COURSE)

    private lateinit var _golfer: Golfer
    private lateinit var _course: Course

    /**
     * Get the implicit join path to the <code>GOLFER</code> table.
     */
    fun golfer(): Golfer {
        if (!this::_golfer.isInitialized)
            _golfer = Golfer(this, SCORES__FK_SCORES_PK_GOLFER)

        return _golfer;
    }

    val golfer: Golfer
        get(): Golfer = golfer()

    /**
     * Get the implicit join path to the <code>COURSE</code> table.
     */
    fun course(): Course {
        if (!this::_course.isInitialized)
            _course = Course(this, SCORES__FK_SCORES_PK_COURSE)

        return _course;
    }

    val course: Course
        get(): Course = course()
    override fun `as`(alias: String): Scores = Scores(DSL.name(alias), this)
    override fun `as`(alias: Name): Scores = Scores(alias, this)
    override fun `as`(alias: Table<*>): Scores = Scores(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    override fun rename(name: String): Scores = Scores(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): Scores = Scores(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): Scores = Scores(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row9<String?, Int?, Float?, Int?, String?, Float?, Int?, Int?, String?> = super.fieldsRow() as Row9<String?, Int?, Float?, Int?, String?, Float?, Int?, Int?, String?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (String?, Int?, Float?, Int?, String?, Float?, Int?, Int?, String?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (String?, Int?, Float?, Int?, String?, Float?, Int?, Int?, String?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}