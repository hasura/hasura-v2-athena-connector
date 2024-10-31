package gdc.sqlgen.extensions

import gdc.ir.FullyQualifiedTableName
import gdc.ir.QueryRequest
import gdc.sqlgen.generic.BaseQueryGenerator
import gdc.ir.Target

fun QueryRequest.containsAggregates() =
    !this.query.aggregates.isNullOrEmpty()

fun QueryRequest.containsFields() =
    !this.query.fields.isNullOrEmpty()

fun QueryRequest.isAggOnlyRequest() =
   this.containsAggregates() && !this.containsFields()

fun QueryRequest.isFieldOnlyRequest() =
    this.containsFields() && !this.containsAggregates()

fun QueryRequest.containsFieldsAndAggregates() =
    this.containsFields() && this.containsAggregates()

fun QueryRequest.isForeachRequest() =
    this.getName() == FullyQualifiedTableName(BaseQueryGenerator.FOREACH_ROWS)

fun QueryRequest.isInterpolatedQuery() =
    this.target is Target.InterpolatedTarget

fun QueryRequest.isFunctionRequest() =
    this.target is Target.FunctionTarget
