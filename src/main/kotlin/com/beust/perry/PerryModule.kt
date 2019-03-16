package com.beust.perry

import com.beust.perry.exposed.BooksDaoExposed
import com.beust.perry.exposed.CyclesDaoExposed
import com.beust.perry.exposed.PendingDaoExposed
import com.beust.perry.exposed.SummariesDaoExposed
import com.beust.perry.inmemory.BooksDaoInMemory
import com.beust.perry.inmemory.CyclesDaoInMemory
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton
import java.net.URI
import kotlin.to as _

class PerryModule : Module {
    override fun configure(binder: Binder) {
        // TypedProperties
        val localProperties = TypedProperties(
                MergedProperties("config.properties", "local.properties").map)
        binder.bind(TypedProperties::class.java).toInstance(localProperties)

        binder.bind(PerryContext::class.java).toInstance(PerryContext())

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
                            Triple(jdbcUrl!!, username, password)
                        }
                    }
                } else {
                    // Local
                    val user = localProperties.get(LocalProperty.DATABASE_USER)
                    val password = localProperties.get(LocalProperty.DATABASE_PASSWORD)
                    val url = localProperties.getRequired(LocalProperty.DATABASE_URL)
                    Triple(url, user, password)
                }

            if (user != null && password != null) {
                org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className,
                        user = user, password = password)
            } else {
                org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className)
            }
        }

//        binder.bind(io.dropwizard.auth.Authenticator::class.java).to(PerryAuthenticator::class.java)

        fun bindExposed() {
            binder.bind(CyclesDao::class.java).to(CyclesDaoExposed::class.java)
                    .`in`(Singleton::class.java)
            binder.bind(BooksDao::class.java).to(BooksDaoExposed::class.java)
                    .`in`(Singleton::class.java)
            binder.bind(SummariesDao::class.java).to(SummariesDaoExposed::class.java)
                    .`in`(Singleton::class.java)
            binder.bind(UsersDao::class.java).to(UsersDaoExposed::class.java)
                    .`in`(Singleton::class.java)
            binder.bind(PendingDao::class.java).to(PendingDaoExposed::class.java)
                    .`in`(Singleton::class.java)
        }

        when(localProperties.database) {
            Database.POSTGRESQL -> {
                initJdbc("org.postgresql.Driver")
                bindExposed()
            }
            Database.MY_SQL -> {
                initJdbc("com.mysql.jdbc.Driver")
                bindExposed()
            }
            else -> {
                binder.bind(CyclesDao::class.java).to(CyclesDaoInMemory::class.java)
                        .`in`(Singleton::class.java)
                binder.bind(BooksDao::class.java).to(BooksDaoInMemory::class.java)
                        .`in`(Singleton::class.java)
            }
        }
    }
}