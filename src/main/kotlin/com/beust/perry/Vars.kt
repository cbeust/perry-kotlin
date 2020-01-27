package com.beust.perry

open class Vars {
    val map = hashMapOf<String, String>()

    fun add(property: LocalProperty, value: String) {
        map[property.toString()] = value
    }
}

/**
 * Development variables are all read from local.properties.
 */
class DevVars: Vars() {
    init {
        val lp = LocalProperties()
        LocalProperty.values().map { it.toString() }.forEach {
            map[it] = lp.get(it)
        }
    }
}

open class EnvVars(vararg properties: LocalProperty): Vars() {
    init {
        properties.map { it.toString() }.forEach {
            map[it] = System.getenv(it)
        }
    }

    fun addEnv(property: LocalProperty, envName: String) {
        map[property.toString()] = System.getenv(envName)!!
    }
}

/**
 * Heroku variables all come from the environment, with a few exceptions.
 */
class HerokuVars: EnvVars(LocalProperty.EMAIL_USERNAME, LocalProperty.EMAIL_PASSWORD,
        LocalProperty.TWITTER_ACCESS_TOKEN, LocalProperty.TWITTER_ACCESS_TOKEN_SECRET,
        LocalProperty.TWITTER_CONSUMER_KEY, LocalProperty.TWITTER_CONSUMER_KEY_SECRET)
{
    init {
        addEnv(LocalProperty.JDBC_USERNAME, "JDBC_DATABASE_USERNAME")
        addEnv(LocalProperty.JDBC_PASSWORD, "JDBC_DATABASE_PASSWORD")
        addEnv(LocalProperty.JDBC_URL, "JDBC_DATABASE_URL")

        add(LocalProperty.DATABASE, "postgresql")
        add(LocalProperty.HOST, Urls.HOST)
    }
}
