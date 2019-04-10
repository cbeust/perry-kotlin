package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.google.inject.Binder
import com.google.inject.Module
import java.time.LocalDateTime
import kotlin.to as _

class PerryModule : Module {
    val isProduction = System.getenv("IS_HEROKU") != null

    override fun configure(binder: Binder) {
        // TypedProperties
        val vars =
            if (isProduction) HerokuVars()
            else DevVars()
        val typedProperties = TypedProperties(vars.map)
        binder.bind(TypedProperties::class.java).toInstance(typedProperties)

        if (isProduction) {
            binder.bind(TwitterService::class.java).to(RealTwitterService::class.java)
            binder.bind(EmailSender::class.java).to(ProductionEmailSender::class.java)
        } else {
            binder.bind(TwitterService::class.java).to(FakeTwitterService::class.java)
            binder.bind(EmailSender::class.java).to(FakeEmailSender::class.java)
        }

        binder.bind(MetricRegistry::class.java).toInstance(MetricRegistry())
        binder.bind(CoverCacheMetric::class.java).toInstance(CoverCacheMetric(LocalDateTime.now()))
    }
}