package com.beust.perry

import com.google.inject.Guice
import com.google.inject.Inject
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
class RealTwitterService @Inject constructor(val env: TypedProperties): TwitterService {
    private val log = org.slf4j.LoggerFactory.getLogger(RealTwitterService::class.java)

    private val twitter: Twitter
        get() {
            val cb = ConfigurationBuilder()
            cb.setDebugEnabled(true)
                .setOAuthConsumerKey(env.get(LocalProperty.TWITTER_CONSUMER_KEY))
                .setOAuthConsumerSecret(env.get(LocalProperty.TWITTER_CONSUMER_KEY_SECRET))
                .setOAuthAccessToken(env.get(LocalProperty.TWITTER_ACCESS_TOKEN))
                .setOAuthAccessTokenSecret(env.get(LocalProperty.TWITTER_ACCESS_TOKEN_SECRET))
            return TwitterFactory(cb.build()).instance
        }

    override fun updateStatus(number: Int, title: String, url: String) {
        if (! title.isEmpty()) {
            twitter.updateStatus("$number: '$title'   $url")
        } else {
            log.info("Not posting to Twitter, empty title for summary $number")
        }
    }
}

fun main(args: Array<String>) {
    val injector = Guice.createInjector(PerryModule())
    val twitter = injector.getInstance(TwitterService::class.java)
//    val tweets = twitter.homeTimeline
//    println(tweets)
}
