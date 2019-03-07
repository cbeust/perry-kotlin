package com.beust.perry

import com.beust.koolaid.Database
import com.beust.koolaid.LocalProperties
import com.beust.koolaid.LocalProperty
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton
import kotlin.to as _

class PerryModule : Module {
    override fun configure(binder: Binder) {
        // LocalProperties
        val localProperties = LocalProperties()
        binder.bind(LocalProperties::class.java).toInstance(localProperties)

        // DAO's
        fun initJdbc(className: String) {
            val user = localProperties.getRequired(LocalProperty.DATABASE_USER)
            val password = localProperties.getRequired(LocalProperty.DATABASE_PASSWORD)
            val url = localProperties.getRequired(LocalProperty.DATABASE_URL)
            org.jetbrains.exposed.sql.Database.connect(url,
                    driver = className, user = user, password = password)
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