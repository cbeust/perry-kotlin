package com.beust.perry

/**
 * Passed to the DatabaseModule to provide information on how to connect to the database.
 */
interface DbProvider {
    val database: Database
    val databaseUrl: String
    val username: String
    val password: String
}

/**
 * Production DbProvider. Get the database info from the environment.
 */
class DbProviderHeroku : DbProvider {
    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = System.getenv("JDBC_DATABASE_URL")!!
    override val username = System.getenv("JDBC_DATABASE_USERNAME")!!
    override val password = System.getenv("JDBC_DATABASE_PASSWORD")!!
}

/**
 * Development DbProvider. Get the database info from local.properties.
 */
class DbProviderLocal: DbProvider {
    private val tp = ITypedProperties.get()

    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = tp.jdbcUrl
    override val username: String = tp.jdbcUsername
    override val password: String = tp.jdbcPassword
}

/**
 * Connect to the production database from a local environment. Useful to run commands
 * on the production database. The production database info needs to be store in local.properties.
 */
class DbProviderLocalToProduction: DbProvider {
    private val prop = LocalProperties()

    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = prop.get("PRODUCTION_JDBC_URL")
    override val username = prop.get("PRODUCTION_JDBC_USERNAME")
    override val password = prop.get("PRODUCTION_JDBC_PASSWORD")
}
