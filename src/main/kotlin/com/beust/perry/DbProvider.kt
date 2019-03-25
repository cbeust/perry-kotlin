package com.beust.perry

interface DbProvider {
    val database: Database
    val databaseUrl: String
    val username: String
    val password: String
}

class DbProviderHeroku : DbProvider {
    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = System.getenv("JDBC_DATABASE_URL")!!
    override val username = System.getenv("JDBC_DATABASE_USERNAME")!!
    override val password = System.getenv("JDBC_DATABASE_PASSWORD")!!
}

class DbProviderLocal: DbProvider {
    private val prop = LocalProperties()

    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = prop.get(LocalProperty.JDBC_URL.toString())
    override val username: String = prop.get(LocalProperty.JDBC_USERNAME.toString())
    override val password: String = prop.get(LocalProperty.JDBC_PASSWORD.toString())
}

class DbProviderLocalToProduction: DbProvider {
    private val prop = LocalProperties()

    override val database: Database = Database.POSTGRESQL
    override val databaseUrl = prop.get("PRODUCTION_JDBC_URL")
    override val username = prop.get("PRODUCTION_JDBC_USERNAME")
    override val password = prop.get("PRODUCTION_JDBC_PASSWORD")
}
