package com.beust.perry

import java.net.URI

open class Vars {
    companion object {
        val JDBC_USERNAME = "JDBC_USERNAME"
        val JDBC_PASSWORD = "JDBC_PASSWORD"
        val JDBC_URL = "JDBC_URL"
        val EMAIL_USERNAME = "EMAIL_USERNAME"
        val EMAIL_PASSWORD = "EMAIL_PASSWORD"
        val DATABASE = "DATABASE"
        val HOST = "HOST"
    }

    val map = hashMapOf<String, String>()
}

class DevVars: Vars() {
    init {
        val lp = LocalProperties()
        listOf(DATABASE, JDBC_USERNAME,  JDBC_PASSWORD, JDBC_URL, EMAIL_USERNAME, EMAIL_PASSWORD, HOST).forEach {
            map[it] = lp.get(it)
        }
    }

}
class HerokuVars: Vars() {
    init {
        // Extract username and password from DATABASE_URL
        val dbUrl = System.getenv("DATABASE_URL")
        URI(dbUrl).let { dbUri ->
            dbUri.userInfo.split(":").let { split ->
                val username = split[0]
                val password = split[1]
                val jdbcUrl = System.getenv("JDBC_DATABASE_URL")
                map[JDBC_USERNAME] = username
                map[JDBC_PASSWORD] = password
                map[JDBC_URL] = jdbcUrl
            }
        }

        map[EMAIL_USERNAME] = System.getenv("EMAIL_USERNAME")
        map[EMAIL_PASSWORD] = System.getenv("EMAIL_PASSWORD")
        map[DATABASE] = "postgresql"
        map[HOST] = "http://perry-kotlin.herokuapp.com"
    }
}
