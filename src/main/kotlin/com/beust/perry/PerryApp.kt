package com.beust.perry

import com.codahale.metrics.Gauge
import com.codahale.metrics.servlets.AdminServlet
import com.codahale.metrics.servlets.HealthCheckServlet
import com.codahale.metrics.servlets.MetricsServlet
import com.google.inject.Inject
import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.views.ViewBundle
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


class PerryApp : Application<DemoConfig>() {
    private lateinit var guiceBundle: GuiceBundle<DemoConfig>

    override fun initialize(configuration: Bootstrap<DemoConfig>) {
        configuration.addBundle(AssetsBundle("/assets", "/static", "index.html", "static"));
        guiceBundle = GuiceBundle.newBuilder<DemoConfig>()
                .addModule(PerryModule())
                .setConfigClass(DemoConfig::class.java)
                .build()
        configuration.addBundle(ViewBundle())

        configuration.addBundle(guiceBundle)
    }

    class CoverCountMetric @Inject constructor(val coversDao: CoversDao): Gauge<Int> {
        override fun getValue(): Int {
            return coversDao.count
        }
    }

    class CoverSizeMetric @Inject constructor(val coversDao: CoversDao): Gauge<String> {
        override fun getValue(): String {
            var count = 0.0
            transaction {
                CoversTable.slice(CoversTable.image).selectAll().forEach {
                    count += it[CoversTable.image].size
                }
            }
            return (count.toFloat() / 1_000_000).toString() + " MB"
        }
    }

    override fun run(config: DemoConfig, env: Environment) {
        listOf(PerryService::class.java).forEach {
            env.jersey().register(it)
        }

        env.jersey().register(AuthValueFactoryProvider.Binder(User::class.java))

        env.jersey().register(AuthDynamicFeature(BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(guiceBundle.injector.getInstance(PerryAuthenticator::class.java))
                .setAuthorizer(PerryAuthorizer())
                .setRealm("BASIC-AUTH-REALM")
                .buildAuthFilter()))

        env.metrics().apply {
            register("coverCount", guiceBundle.injector.getInstance(CoverCountMetric::class.java))
            register("coverSize", guiceBundle.injector.getInstance(CoverSizeMetric::class.java))
        }
        env.applicationContext.apply {
            setAttribute(MetricsServlet.METRICS_REGISTRY, env.metrics())
            setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, env.healthChecks())
        }
        env.servlets().apply {
            addServlet("admin", AdminServlet()).addMapping("/admin")
            addServlet("metrics", MetricsServlet()).addMapping("/admin/metrics")
        }

//        env.jersey().register(AuthDynamicFeature(
//                PerryAuthFilterBuilder()
//                        .setAuthenticator(PerryAuthenticator())
//                        .setAuthorizer(PerryAuthorizer())
//                        .buildAuthFilter()
//        ))

            env.healthChecks().register("template", DemoCheck(config.version))
        }
    }
}