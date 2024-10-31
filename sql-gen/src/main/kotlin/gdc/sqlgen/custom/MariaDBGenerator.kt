package gdc.sqlgen.custom

import gdc.ir.QueryRequest
import gdc.sqlgen.generic.CorrelatedSubqueryDSL
import gdc.sqlgen.generic.CorrelatedSubqueryGenerator
import org.jooq.Context
import org.jooq.Field
import org.jooq.QueryPart
import org.jooq.Record
import org.jooq.Select
import org.jooq.impl.CustomField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType


object MariaDbDSL : CorrelatedSubqueryDSL {
    override fun jsonArrayAgg(value: Field<*>?, orderBy: QueryPart?, limit: Int?, offset: Int?): CustomField<Record> =
        CustomField.of("json_array_agg", SQLDataType.RECORD) { ctx: Context<*> ->
            ctx.visit(
                DSL.field("(json_arrayagg({0} {1} LIMIT {2} OFFSET {3}))", value, orderBy, limit, offset)
            )
        }
}

object MariaDBGenerator : CorrelatedSubqueryGenerator(MariaDbDSL) {
    override fun mutationQueryRequestToSQL(request: QueryRequest): Select<*> {
        return DSL.select(
            DSL.jsonGetAttribute(
                queryRequestToSQL(request).asField(),
                "rows"
            )
        )
    }

}
