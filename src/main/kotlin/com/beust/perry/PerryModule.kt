package com.beust.perry

import com.beust.perry.exposed.*
import com.beust.perry.inmemory.BooksDaoInMemory
import com.beust.perry.inmemory.CyclesDaoInMemory
import com.codahale.metrics.MetricRegistry
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton
import kotlin.to as _

class PerryModule : Module {
    override fun configure(binder: Binder) {
        val isProduction = System.getenv("IS_HEROKU") != null
        // TypedProperties
        val vars =
            if (isProduction) HerokuVars()
            else DevVars()
        val typedProperties = TypedProperties(vars.map)
        binder.bind(TypedProperties::class.java).toInstance(typedProperties)

        if (isProduction) {
            binder.bind(TwitterService::class.java).to(RealTwitterService::class.java)
        } else {
            binder.bind(TwitterService::class.java).to(FakeTwitterService::class.java)
        }

        binder.bind(PerryContext::class.java).toInstance(PerryContext())
        binder.bind(MetricRegistry::class.java).toInstance(MetricRegistry())

        // DAO's
        fun initJdbc(className: String) {
            val dbUrl = typedProperties.getRequired(LocalProperty.JDBC_URL)
            val user = typedProperties.getRequired(LocalProperty.JDBC_USERNAME)
            val password = typedProperties.getRequired(LocalProperty.JDBC_PASSWORD)
            org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className,
                        user = user, password = password)
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
            binder.bind(CoversDao::class.java).to(CoversDaoExposed::class.java)
                    .`in`(Singleton::class.java)
        }

        when(typedProperties.database) {
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