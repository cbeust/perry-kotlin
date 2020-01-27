package com.beust.perry

open class Vars {
    val map = hashMapOf<String, String>()
}

class DevVars: Vars() {
    init {
        val lp = LocalProperties()
        LocalProperty.values().map { it.toString() }.forEach {
            map[it] = lp.get(it)
        }

        map[LocalProperty.JDBC_USERNAME.toString()] = System.getenv("JDBC_USERNAME")
        map[LocalProperty.JDBC_PASSWORD.toString()] = System.getenv("JDBC_PASSWORD")

        map[LocalProperty.DATABASE.toString()] = "postgresql"
        map[LocalProperty.HOST.toString()] = Urls.HOST

        listOf(LocalProperty.EMAIL_USERNAME, LocalProperty.EMAIL_PASSWORD, LocalProperty.TWITTER_ACCESS_TOKEN,
                LocalProperty.TWITTER_ACCESS_TOKEN_SECRET, LocalProperty.TWITTER_CONSUMER_KEY,
                LocalProperty.TWITTER_CONSUMER_KEY_SECRET).map { it.toString() }.forEach {
            map[it] = System.getenv(it)
        }

    }

}
class HerokuVars: Vars() {
    init {
        map[LocalProperty.JDBC_DATABASE_URL.toString()] = System.getenv("JDBC_DATABASE_URL")
    }
}

class DockerVars: Vars() {
    init {
        map[LocalProperty.JDBC_DATABASE_URL.toString()] = System.getenv("JDBC_DATABASE_URL_DOCKER")
    }
}
