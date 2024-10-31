package gdc.sqlgen.custom

import gdc.ir.Aggregate
import gdc.sqlgen.generic.TreeQueryDSL
import gdc.sqlgen.generic.TreeQueryGenerator
import org.jooq.Context
import org.jooq.Field
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.SelectField
import org.jooq.impl.CustomField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType


object AthenaDSL : TreeQueryDSL {

    override fun mapFromEntries(fields: Collection<SelectField<*>>): CustomField<Record> =
        CustomField.of("map_from_entries", SQLDataType.RECORD) { ctx: Context<*> ->
            ctx.visit(
                DSL.field("map_from_entries({0})", DSL.array(fields as List<Field<Record>>))
            )
        }


    fun castJson(field: SelectField<*>): CustomField<Any> =
        CustomField.of("cast", SQLDataType.OTHER) { ctx: Context<*> ->
            ctx.visit(
                DSL.field("cast({0} as json)", field)
            )
        }

    fun keyValueRow(key: org.jooq.Param<String>, field: Field<*>): CustomField<Record> =
        CustomField.of("key_value_row", SQLDataType.RECORD) { ctx: Context<*> ->
            ctx.visit(
                DSL.field("({0}, {1})", key, field)
            )
        }

    override fun emptyArray(): CustomField<Array<Any>> =
        CustomField.of("empty_array", SQLDataType.OTHER.arrayDataType) { ctx: Context<*> ->
            ctx.visit(
                DSL.field("array[]")
            )
        }
}

object AthenaGenerator : TreeQueryGenerator(AthenaDSL, SQLDialect.DEFAULT) {

    override val orderByDefault = null

    override fun mkRowsField(ctx: QueryGenCtx): SelectField<*> {
        return DSL.arrayAgg(
            AthenaDSL.mapFromEntries(
                if (ctx.request.query.fields == null) {
                    emptyList()
                } else if (ctx.request.query.fields!!.isEmpty()) {
                    listOf(DSL.inline("rows"), AthenaDSL.emptyObjAgg())
                } else {
                    getColumnFields(ctx.request.query.fields).map { (alias, colField) ->
                        AthenaDSL.keyValueRow(
                            DSL.inline(alias.value),
                            AthenaDSL.castJson(
                                DSL.field(
                                    DSL.name(ctx.currentTableAlias, colField.column)
                                )
                            )
                        )
                    }
                } + getRelationFields(ctx.request.query.fields).map { (alias, relField) ->
                    AthenaDSL.keyValueRow(
                        DSL.inline(alias.value),
                        AthenaDSL.castJson(
                            // Default field value generation logic for relationship fields (if rows/aggregates value is null)
                            DSL.coalesce(
                                // Row we want to select from the query, IE: "artist_base_fields_0.Albums"
                                DSL.field(
                                    DSL.name(ctx.currentTableAlias, alias.value)
                                ),
                                // Default value if the row is null
                                //
                                // What we're doing here is making an inline object with the same structure as the
                                // relationship field, but with all the fields set to their default values.
                                //
                                // TODO: This probably isn't the right place to do this, and it's certainly not DRY
                                AthenaDSL.castJson(
                                    AthenaDSL.mapFromEntries(
                                        listOfNotNull(
                                            // First, the "rows" key
                                            if (relField.query.fields == null) {
                                                null
                                            } else {
                                                AthenaDSL.keyValueRow(
                                                    DSL.inline("rows"),
                                                    AthenaDSL.castJson(
                                                        AthenaDSL.emptyArray()
                                                    )
                                                )
                                            },
                                            // Then, the "aggregates" key
                                            if (relField.query.aggregates.isNullOrEmpty()) {
                                                null
                                            } else {
                                                AthenaDSL.keyValueRow(
                                                    DSL.inline("aggregates"),
                                                    AthenaDSL.castJson(
                                                        AthenaDSL.mapFromEntries(
                                                            relField.query.aggregates!!.map { (alias, agg) ->
                                                                AthenaDSL.keyValueRow(
                                                                    DSL.inline(alias.value),
                                                                    AthenaDSL.castJson(
                                                                        // Get the default value based on the aggregate type
                                                                        when (agg) {
                                                                            is Aggregate.SingleColumn -> DSL.nullCondition()
                                                                            is Aggregate.StarCount -> DSL.zero()
                                                                            is Aggregate.ColumnCount -> DSL.zero()
                                                                        }
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    )
                                                )
                                            }
                                        )

                                    )
                                ) as SelectField<*>
                            )
                        )
                    )
                }
            )
        )
    }

    override fun mkRowsAndAggregates(
        ctx: QueryGenCtx,
        rowFields: SelectField<*>,
        aggregatesFields: SelectField<*>
    ): SelectField<*> {
        return AthenaDSL.castJson(
            AthenaDSL.mapFromEntries(
                listOfNotNull(
                    if (ctx.request.query.fields == null) {
                        null
                    } else {
                        AthenaDSL.keyValueRow(
                            DSL.inline("rows"),
                            AthenaDSL.castJson(
                                if (ctx.request.query.fields!!.isEmpty()) AthenaDSL.emptyObjAgg() else rowFields
                            )
                        )
                    },
                    if (ctx.request.query.aggregates.isNullOrEmpty()) {
                        null
                    } else {
                        AthenaDSL.keyValueRow(
                            DSL.inline("aggregates"),
                            // ('aggregates', cast(map_from_entries(ARRAY[ ... ]) as json))
                            AthenaDSL.castJson(aggregatesFields)
                        )
                    }
                )
            )
        )
    }

    override fun mkAggregatesField(ctx: QueryGenCtx): SelectField<*> {
        return AthenaDSL.mapFromEntries(
            getAggregateFields(ctx.request).map { (alias, aggField) ->
                AthenaDSL.keyValueRow(
                    DSL.inline(alias.value),
                    AthenaDSL.castJson(
                        translateIRAggregateField(
                            aggField
                        )
                    )
                )
            }
        )
    }

}
