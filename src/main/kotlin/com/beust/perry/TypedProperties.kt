package com.beust.perry

import org.slf4j.LoggerFactory

/**
 * Encapsulate read access to a  map of properties with optional type constraints on keys
 * and values.
 */
interface ITypedProperties {
    val database: String
    val jdbcUsername: String
    val jdbcPassword: String
    val jdbcUrl: String
    val emailUsername: String
    val emailPassword: String
    val host: String
    val twitterConsumerKey: String
    val twitterConsumerKeySecret: String
    val twitterAccessToken: String
    val twitterAccessTokenSecret: String

    companion object {
        private val log = LoggerFactory.getLogger(ITypedProperties::class.java)

        private val isHeroku = System.getenv("IS_HEROKU") != null
        private val isDocker = System.getenv("IS_DOCKER") != null
        private val isKubernetes = System.getenv("IS_KUBERNETES") != null

        val isProduction = isHeroku || isKubernetes

        fun get(): ITypedProperties {
            // TypedProperties
            val result = when {
                isHeroku -> HerokuTypedProperties()
                isDocker -> DockerTypedProperties()
                isKubernetes -> KubernetesTypedProperties()
                else -> TypedProperties()
            }

            log.info("JDBC URL: " + result.jdbcUrl)
            log.info("Typed properties: $result")
            return result
        }
    }
}

open class TypedProperties: ITypedProperties {
    private val lp = LocalProperties()
    private fun get(property: LocalProperty) = get(property.toString())
    fun env(n: String): String? = System.getenv(n)
    fun env(property: LocalProperty): String? = System.getenv(property.toString())
    fun local(property: String) = lp.get(property)
    fun get(n: String) = env(n) ?: local(n)

    override val database get() = get(LocalProperty.DATABASE)
    override val jdbcUsername get() = get(LocalProperty.JDBC_USERNAME)
    override val jdbcPassword get() = get(LocalProperty.JDBC_PASSWORD)
    override val jdbcUrl get() = get(LocalProperty.JDBC_URL)
    override val emailUsername get() = get(LocalProperty.EMAIL_USERNAME)
    override val emailPassword get() = get(LocalProperty.EMAIL_PASSWORD)
    override val host get() = get(LocalProperty.HOST)
    override val twitterConsumerKey get() = get(LocalProperty.TWITTER_CONSUMER_KEY)
    override val twitterConsumerKeySecret get() = get(LocalProperty.TWITTER_CONSUMER_KEY_SECRET)
    override val twitterAccessToken get() = get(LocalProperty.TWITTER_ACCESS_TOKEN)
    override val twitterAccessTokenSecret get() = get(LocalProperty.TWITTER_ACCESS_TOKEN_SECRET)
}

class DockerTypedProperties: TypedProperties() {
    override val jdbcUrl = local("JDBC_DOCKER_URL")
    override val jdbcUsername = local("JDBC_DOCKER_USERNAME")
    override val jdbcPassword = local("JDBC_DOCKER_PASSWORD")
}

open class EnvTypedProperties: TypedProperties() {
    override val database = "postgresql"
    override val host = Urls.HOST

    override val emailUsername = env(LocalProperty.EMAIL_USERNAME)!!
    override val emailPassword = env(LocalProperty.EMAIL_PASSWORD)!!
    override val twitterConsumerKey = env(LocalProperty.TWITTER_CONSUMER_KEY)!!
    override val twitterConsumerKeySecret = env(LocalProperty.TWITTER_CONSUMER_KEY_SECRET)!!
    override val twitterAccessToken = env(LocalProperty.TWITTER_ACCESS_TOKEN)!!
    override val twitterAccessTokenSecret = env(LocalProperty.TWITTER_ACCESS_TOKEN_SECRET)!!
}

class HerokuTypedProperties: EnvTypedProperties() {
    override val jdbcUrl = env("JDBC_DATABASE_URL")!!
    override val jdbcUsername = env("JDBC_DATABASE_USERNAME")!!
    override val jdbcPassword = env("JDBC_DATABASE_PASSWORD")!!
}

class KubernetesTypedProperties: EnvTypedProperties() {
    override val jdbcUsername = env("JDBC_DOCKER_USERNAME")!!
    override val jdbcPassword = env("JDBC_DOCKER_PASSWORD")!!
    override val jdbcUrl = env("JDBC_DOCKER_URL")!!
}

enum class Database(val value: String) {
    POSTGRESQL("postgresql"), IN_MEMORY("inMemory"), MY_SQL("mysql")
}

enum class LocalProperty(val allowed: Set<String> = hashSetOf()) {
    DATABASE(setOf(Database.POSTGRESQL.value, Database.IN_MEMORY.value, Database.MY_SQL.value)),
    JDBC_USERNAME,
    JDBC_PASSWORD,
    JDBC_URL,
    EMAIL_USERNAME,
    EMAIL_PASSWORD,
    HOST,
    TWITTER_CONSUMER_KEY,
    TWITTER_CONSUMER_KEY_SECRET,
    TWITTER_ACCESS_TOKEN,
    TWITTER_ACCESS_TOKEN_SECRET
    ;
}
