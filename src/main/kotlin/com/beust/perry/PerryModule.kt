package com.beust.perry

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton
import java.net.URI
import kotlin.to as _

class PerryModule : Module {
    override fun configure(binder: Binder) {
        // TypedProperties
        val localProperties = TypedProperties(
                MergedProperties("config.properties" /*, "local.properties"*/).map)
        binder.bind(TypedProperties::class.java).toInstance(localProperties)

        // DAO's
        fun initJdbc(className: String) {
            // postgres://{user}:{password}@{hostname}:{port}/{database-name}
            val envDbUrl = System.getenv("DATABASE_URL")
            val (dbUrl, user, password) =
                    if (envDbUrl != null) {
                        // Heroku, extract username and password from DATABASE_URL
                        URI(envDbUrl).let { dbUri ->
                            dbUri.userInfo.split(":").let { split ->
                                val username = split[0]
                                val password = split[1]
                                val jdbcUrl = System.getenv("JDBC_DATABASE_URL")
                                Triple(jdbcUrl, username, password)
                            }
                        }
                    } else {
                        // Local
                        val envUsername = localProperties.get(LocalProperty.DATABASE_USER)
                        val envPassword = localProperties.get(LocalProperty.DATABASE_PASSWORD)
                        val database = localProperties.get(LocalProperty.DATABASE)
                        val dbUrl = "jdbc:$database:demo"
                        Triple(dbUrl, envUsername, envPassword)
                    }

//        val user = localProperties.getOrNull(LocalProperty.DATABASE_USER)
//        val password = localProperties.getOrNull(LocalProperty.DATABASE_PASSWORD)
//        val url = localProperties.getRequired(LocalProperty.DATABASE_URL)
            if (user != null && password != null) {
                org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className,
                        user = user, password = password)
            } else {
                org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className)
            }
        }

        when(localProperties.database) {
            Database.POSTGRESQL -> {
                initJdbc("org.postgresql.Driver")
                binder.bind(CyclesDao::class.java).to(CyclesDaoExposed::class.java)
                        .`in`(Singleton::class.java)
            }
            Database.MY_SQL -> {
                initJdbc("com.mysql.jdbc.Driver")
                binder.bind(CyclesDao::class.java).to(CyclesDaoExposed::class.java)
                        .`in`(Singleton::class.java)
            }
            else -> {
                binder.bind(CyclesDao::class.java).to(CyclesDaoInMemory::class.java)
                        .`in`(Singleton::class.java)
            }
        }
    }
}