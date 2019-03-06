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
        when(localProperties.database) {
            Database.POSTGRESQL -> {
            }
            Database.MY_SQL -> {
                val user = localProperties.get(LocalProperty.DATABASE_USER)!!
                val password = localProperties.get(LocalProperty.DATABASE_PASSWORD)!!
                val url = localProperties.get(LocalProperty.DATABASE_URL)!!
                org.jetbrains.exposed.sql.Database.connect(
                        url,
                        driver = "com.mysql.jdbc.Driver",
                        user = user, password = password)

                binder.bind(CyclesDao::class.java).to(CyclesDaoMysql::class.java)
                        .`in`(Singleton::class.java)
            }
            else -> {
                binder.bind(CyclesDao::class.java).to(CyclesDaoInMemory::class.java)
                        .`in`(Singleton::class.java)
            }
        }
    }
}