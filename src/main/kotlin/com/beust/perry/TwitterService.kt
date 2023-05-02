package com.beust.perry

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

class Result<out E, out V>(val error: E? = null, val value: V? = null) {
    companion object {
        fun <E, V> success(value: V): Result<E, V> = Result(value = value)
        fun <E, V> error(error: E): Result<E, V> = Result(error = error)
    }
}

interface TwitterService {
    fun auth(): Result<String, Boolean>
    fun updateStatus(number: Int, title: String, url: String)
}

class FakeTwitterService: TwitterService {
    private val log = LoggerFactory.getLogger(FakeTwitterService::class.java)

    override fun auth(): Result<String, Boolean> = Result.success(value = true)
    override fun updateStatus(number: Int, title: String, url: String) {
        if (! title.isEmpty()) {
            log.info("Posting to Twitter: $number: '$title'   $url")
        }
    }

}


class RealTwitterService @Inject constructor(private val properties: IConfig): TwitterService {
    private val log = LoggerFactory.getLogger(RealTwitterService::class.java)

    private val twitter: Twitter?
        get() {
            val cb = ConfigurationBuilder().apply {
                setDebugEnabled(true)
                setOAuthConsumerKey(properties.twitterConsumerKey)
                setOAuthConsumerSecret(properties.twitterConsumerKeySecret)
                setOAuthAccessToken(properties.twitterAccessToken)
                setOAuthAccessTokenSecret(properties.twitterAccessTokenSecret)
            }
            return TwitterFactory(cb.build()).instance
        }

    override fun auth(): Result<String, Boolean> {
        return try {
            val factory = twitter
            Result.success(true)
        } catch(ex: Exception) {
            Result.error(ex.message!!)
        }
    }

    override fun updateStatus(number: Int, title: String, url: String) {
        if (twitter == null) {
            log.warn("Twitter service is null, not posting \"$number: $title\"")
            return
        }

        try {
            if (title.isNotEmpty()) {
                val text = "$number: \"$title\"   $url"
                twitter?.updateStatus(text)
                log.info("Posted new status on Twitter: $text")
            } else {
                log.info("Not posting to Twitter, empty title for summary $number")
            }
        } catch(ex: TwitterException) {
            log.error("Couldn't post \"$number: $title\" to Twitter: ${ex.message}")
        }
    }
}
