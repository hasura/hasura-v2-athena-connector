package io.hasura.utils

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class TestDatabaseConfigProvider {

    @ConfigProperty(name = "testing.database.url.h2")
    lateinit var h2: String

    @ConfigProperty(name = "testing.database.url.postgres")
    lateinit var postgres: String

    // Note about MySQL and MariaDB in classpath at same time:
    // https://mariadb.com/kb/en/about-mariadb-connector-j/#having-mariadb-and-mysql-drivers-in-the-same-classpath
    //
    // MariaDB Connector/J permits connection URLs beginning with both jdbc:mariadb and jdbc:mysql.
    //
    // However, if you also have MySQL's JDBC driver in your CLASSPATH, then this could cause
    // issues.
    // To permit having MariaDB Connector/J and MySQL's JDBC driver in your CLASSPATH at the same
    // time,
    // MariaDB Connector/J 1.5.9 and later do not accept connection URLs beginning with jdbc:mysql
    // if
    // the disableMariaDbDriver option is set in the connection URL.
    @ConfigProperty(name = "testing.database.url.mysql")
    lateinit var mysql: String

    @ConfigProperty(name = "testing.database.url.oracle")
    lateinit var oracle: String

    @ConfigProperty(name = "testing.database.url.mongo")
    lateinit var mongo: String

    @ConfigProperty(name = "testing.database.url.mongo_documentdb")
    lateinit var mongoDocumentDB: String
}
