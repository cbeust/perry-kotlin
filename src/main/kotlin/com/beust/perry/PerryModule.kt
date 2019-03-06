package com.beust.perry

import com.beust.koolaid.Database
import com.beust.koolaid.LocalProperties
import com.beust.koolaid.LocalProperty
import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Singleton

class PerryModule : Module {
    override fun configure(binder: Binder) {
        // LocalProperties
        val localProperties = LocalProperties()
        binder.bind(LocalProperties::class.java).toInstance(localProperties)

        // Pick the right DAO
        // If $DATABASE_URL is set or if "postgres" was set as the database in local.properties,
        // use ViewDaoPostgres, else use ViewDaoInMemory
        val isPostgres = System.getenv("DATABASE_URL") != null
                || Database.POSTGRESQL.value == localProperties.get(LocalProperty.DATABASE)
        val isMySql = System.getenv("DATABASE_URL") != null
                || Database.MY_SQL.value == localProperties.get(LocalProperty.DATABASE)
        val daoClass =
//                if (isPostgres) ViewsDaoPostgres::class.java
                if (isMySql) CyclesDaoMysql::class.java
                else CyclesDaoInMemory::class.java

        binder.bind(CyclesDao::class.java).to(daoClass).`in`(Singleton::class.java)

    }
}