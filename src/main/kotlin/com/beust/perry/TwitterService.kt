package com.beust.perry

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

interface TwitterService {
    fun updateStatus(number: Int, title: String, url: String)
}

class FakeTwitterService: TwitterService {
    private val log = org.slf4j.LoggerFactory.getLogger(FakeTwitterService::class.java)

    override fun updateStatus(number: Int, title: String, url: String) {
        if (! title.isEmpty()) {
            log.info("Posting to Twitter: $number: '$title'   $url")
        }
    }

}


class RealTwitterService @Inject constructor(private val properties: ITypedProperties): TwitterService {
    private val log = LoggerFactory.getLogger(RealTwitterService::class.java)

    private val twitter: Twitter
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

    override fun updateStatus(number: Int, title: String, url: String) {
        if (! title.isEmpty()) {
            val text = "$number: \"$title\"   $url"
            twitter.updateStatus(text)
            log.info("Posted new status on Twitter: $text")
        } else {
            log.info("Not posting to Twitter, empty title for summary $number")
        }
    }
}
