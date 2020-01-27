package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.google.inject.Binder
import com.google.inject.BindingAnnotation
import com.google.inject.Module
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@BindingAnnotation
annotation class Host

class PerryModule : Module {
    private val log = LoggerFactory.getLogger(PerryModule::class.java)

    private val isHeroku = System.getenv("IS_HEROKU") != null
    private val isKubernetes = System.getenv("IS_KUBERNETES") != null
    private val isDocker = System.getenv("IS_DOCKER") != null

    val isProduction = isHeroku || isKubernetes

    override fun configure(binder: Binder) {
        log.warn("@@@@@@@@@@@@@@@@ DOCKER: " + System.getenv("IS_DOCKER"))
        // TypedProperties
        val vars =
            if (isHeroku) HerokuVars()
            else if (isKubernetes || isDocker) DockerVars()
            else DevVars()
        val typedProperties = TypedProperties(vars.map)

        with(binder) {
            bind(TypedProperties::class.java).toInstance(typedProperties)

            if (isProduction) {
                bind(TwitterService::class.java).to(RealTwitterService::class.java)
                bind(EmailSender::class.java).to(ProductionEmailSender::class.java)
                bind<String>(String::class.java).annotatedWith(Host::class.java).toInstance(Urls.HOST)
            } else {
                bind(TwitterService::class.java).to(FakeTwitterService::class.java)
                bind(EmailSender::class.java).to(FakeEmailSender::class.java)
                bind(String::class.java).annotatedWith(Host::class.java).toInstance("http://localhost:9000")
            }
            bind(MetricRegistry::class.java).toInstance(MetricRegistry())
            bind(CoverCacheMetric::class.java).toInstance(CoverCacheMetric(LocalDateTime.now()))
        }
    }
}