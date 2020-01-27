package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.google.inject.Binder
import com.google.inject.BindingAnnotation
import com.google.inject.Module
import java.time.LocalDateTime

@BindingAnnotation
annotation class Host

class PerryModule(private val typedProperties: ITypedProperties) : Module {

    override fun configure(binder: Binder) {
        with(binder) {
            bind(ITypedProperties::class.java).toInstance(typedProperties)

            if (ITypedProperties.isProduction) {
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