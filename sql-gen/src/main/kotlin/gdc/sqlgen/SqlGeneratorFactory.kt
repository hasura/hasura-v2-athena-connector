package gdc.sqlgen

import gdc.sqlgen.custom.*
import gdc.sqlgen.generic.*
import org.jooq.SQLDialect

object SqlGeneratorFactory {
    fun getSqlGenerator(dialect: SQLDialect): BaseQueryGenerator {
        return when (dialect) {
            SQLDialect.DEFAULT -> AthenaGenerator
            else -> throw Error("Unsupported database")
        }
    }

    fun getMutationTranslator(dialect: SQLDialect): BaseMutationTranslator {
        return when (dialect) {
            else -> throw NotImplementedError("Mutation not supported for this data source")
        }
    }
}
