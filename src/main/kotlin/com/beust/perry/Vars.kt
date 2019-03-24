package com.beust.perry

import org.slf4j.LoggerFactory
import java.net.URI

open class Vars {
    val map = hashMapOf<String, String>()
}

class DevVars: Vars() {
    init {
        val lp = LocalProperties()
        LocalProperty.values().map { it.toString() }.forEach {
            map[it] = lp.get(it)
        }
    }

}
class HerokuVars: Vars() {
    private val log = LoggerFactory.getLogger(HerokuVars::class.java)

    init {
        // Extract username and password from DATABASE_URL
        val dbUrl = System.getenv("DATABASE_URL")
        URI(dbUrl).let { dbUri ->
            dbUri.userInfo.split(":").let { split ->
                val username = split[0]
                val password = split[1]
                val jdbcUrl = System.getenv("JDBC_DATABASE_URL")
                log.info("JDBC_DATABASE_URL = $jdbcUrl")
                map[LocalProperty.JDBC_USERNAME.toString()] = username
                map[LocalProperty.JDBC_PASSWORD.toString()] = password
                map[LocalProperty.JDBC_URL.toString()] = jdbcUrl
            }
        }

        map[LocalProperty.DATABASE.toString()] = "postgresql"
        map[LocalProperty.HOST.toString()] = "http://perry-kotlin.herokuapp.com"

        listOf(LocalProperty.EMAIL_USERNAME, LocalProperty.EMAIL_PASSWORD, LocalProperty.TWITTER_ACCESS_TOKEN,
                LocalProperty.TWITTER_ACCESS_TOKEN_SECRET, LocalProperty.TWITTER_CONSUMER_KEY,
                LocalProperty.TWITTER_CONSUMER_KEY_SECRET).map { it.toString() }.forEach {
            map[it] = System.getenv(it)
        }
    }
}
