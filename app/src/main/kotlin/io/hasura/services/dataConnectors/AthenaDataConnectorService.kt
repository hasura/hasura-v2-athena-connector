package io.hasura.services.dataConnectors

import gdc.ir.ColumnNullability
import gdc.ir.ConfigSchema
import gdc.ir.DataSchemaCapabilities
import gdc.ir.DetailLevel
import gdc.ir.ReleaseName
import gdc.ir.Schema
import gdc.ir.SchemaRequest
import io.hasura.controllers.DatasourceName
import io.hasura.interfaces.SchemaCrawlerOptionsGenerator
import io.hasura.models.DatasourceConnectionInfo
import io.hasura.models.DatasourceKind
import io.hasura.services.DataConnectorCacheManager
import io.hasura.services.SchemaCrawlerGDCSchemaProducer
import io.hasura.services.dataSourceService.AgroalDataSourceService
import io.hasura.utils.mkSchemaCrawlerCatalog
import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.jboss.logging.Logger
import org.jooq.SQLDialect
import schemacrawler.schemacrawler.LimitOptionsBuilder
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import javax.sql.DataSource

const val ATHENA_DC_NAME = "athena"

@Singleton
@Named(ATHENA_DC_NAME)
class AthenaDataConnectorService @Inject constructor(
    dataSourceService: AgroalDataSourceService,
    cacheManager: DataConnectorCacheManager,
    private val tracer: Tracer,
    private val logger: Logger
) : BaseDataConnectorService(dataSourceService, cacheManager, tracer) {

    override val name: DatasourceName = ATHENA_DC_NAME
    override val kind: DatasourceKind = DatasourceKind.ATHENA

    val schemaCrawlerOptionsGenerator = SchemaCrawlerOptionsGenerator { dsConnInfo, dataSource, schemaRequest ->
        val catalog = dataSource.connection.use { conn -> conn.catalog }

        requireNotNull(dsConnInfo.config["schema"]) { "'schema' config property is required for Athena" }
        val schemaName = dsConnInfo.config["schema"] as String
        val fullyQualifyAllNames = when (dsConnInfo.config["fully_qualify_all_names"]) {
            false -> false
            else -> true
        }

        val catalogAndSchema = listOf(catalog, schemaName)
            .joinToString(".")
            .replace("\"", "")

        fun includeSchema(schema: String): Boolean {
            val sanitizedSchema = schema.replace("\"", "")
            logger.info("Checking schema $sanitizedSchema against target: $catalogAndSchema")
            return sanitizedSchema == catalogAndSchema
        }

        fun includeTable(table: String): Boolean {
            val onlyTables = schemaRequest.filters?.only_tables
            if (onlyTables != null) {
                val tableName = SchemaCrawlerGDCSchemaProducer.processTableName(fullyQualifyAllNames, table)
                return onlyTables.contains(tableName)
            } else {
                return true
            }
        }
        fun includeColumn(column: String) = true

        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
            .withLimitOptions(
                LimitOptionsBuilder.builder()
                    .tableTypes(null as String?) // all table types
                    .includeSchemas(::includeSchema)
                    .includeTables(::includeTable)
                    .includeColumns(::includeColumn)
                    .toOptions()
            ).withLoadOptions(
                // These options manually configured because this class is used by both
                // Athena and Trino, and Trino will throw errors for some of the features due to lack of support
                LoadOptionsBuilder.builder()
                    .withSchemaInfoLevelBuilder(
                        SchemaInfoLevelBuilder.builder()
                            .setRetrieveIndexes(false)
                            .setRetrieveTables(true)
                            .setRetrieveTableColumns(schemaRequest.detail_level == DetailLevel.EVERYTHING)
                            .setRetrieveColumnDataTypes(schemaRequest.detail_level == DetailLevel.EVERYTHING)
                    ).toOptions()
            )
    }

    override var capabilitiesResponse = super.capabilitiesResponse.copy(
        display_name = "Amazon Athena",
        release_name = ReleaseName.Beta,
        capabilities = (super.capabilitiesResponse.capabilities).copy(
            data_schema = DataSchemaCapabilities(
                supports_primary_keys = false,
                supports_foreign_keys = false,
                column_nullability = ColumnNullability.only_nullable
            ),
            mutations = null,
            datasets = null
        ),
        config_schemas = ConfigSchema(
            config_schema = mapOf(
                "type" to "object",
                "nullable" to false,
                "properties" to super.capabilitiesResponse.config_schemas.config_schema["properties"] as Map<*, *> + mapOf(
                    "schema" to mapOf(
                        "type" to "string",
                        "nullable" to false,
                        "title" to "Database Schema",
                        "description" to "Name of the database schema"
                    )
                )
            )
        )
    )

    override fun executeGetSchema(dataSource: DataSource, connInfo: DatasourceConnectionInfo, schemaRequest: SchemaRequest): Schema {
        val schemaCrawlerCatalog = mkSchemaCrawlerCatalog(connInfo, dataSource, schemaRequest, schemaCrawlerOptionsGenerator)
        return SchemaCrawlerGDCSchemaProducer(
            connInfo.config,
            this.capabilitiesResponse.capabilities,
            schemaRequest.detail_level
        ).makeGDCSchemaForCatalog(schemaCrawlerCatalog)
    }

    override val jooqDialect = SQLDialect.DEFAULT
    override val jooqSettings = commonDSLContextSettings
}
