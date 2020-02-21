package com.beust.perry

import com.beust.perry.exposed.*
import com.beust.perry.inmemory.BooksDaoInMemory
import com.beust.perry.inmemory.CyclesDaoInMemory
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton

class DatabaseModule(private val tp: IConfig,
        private val dbProvider: DbProvider = DbProviderLocal(tp)): Module {
    override fun configure(binder: Binder) {
        // DAO's
        fun initJdbc(className: String) {
            val dbUrl = dbProvider.databaseUrl
            val user = dbProvider.username
            val password = dbProvider.password
            org.jetbrains.exposed.sql.Database.connect(dbUrl, driver = className,
                            user = user, password = password)
            DbMigration().run()

//            val cn = connection.connector
//            try {
//                cn.invoke()
//            } catch(t: Throwable) {
//                t.printStackTrace()
//                throw RuntimeException(t)
//            }
        }

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

        when(dbProvider.database) {
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