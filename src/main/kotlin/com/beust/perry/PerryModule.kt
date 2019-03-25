package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.google.inject.Binder
import com.google.inject.Module
import kotlin.to as _

class PerryModule : Module {
    fun isProduction() = System.getenv("IS_HEROKU") != null

    override fun configure(binder: Binder) {
        val isProduction = isProduction()

        // TypedProperties
        val vars =
            if (isProduction) HerokuVars()
            else DevVars()
        val typedProperties = TypedProperties(vars.map)
        binder.bind(TypedProperties::class.java).toInstance(typedProperties)

        if (isProduction) {
            binder.bind(TwitterService::class.java).to(RealTwitterService::class.java)
        } else {
            binder.bind(TwitterService::class.java).to(FakeTwitterService::class.java)
        }

        binder.bind(PerryContext::class.java).toInstance(PerryContext())
        binder.bind(MetricRegistry::class.java).toInstance(MetricRegistry())
    }
}