package com.beust.perry

import com.codahale.metrics.servlets.*
import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.views.ViewBundle
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


class PerryApp : Application<DemoConfig>() {
    private lateinit var guiceBundle: GuiceBundle<DemoConfig>
    private val module = PerryModule()

    override fun initialize(configuration: Bootstrap<DemoConfig>) {
        configuration.addBundle(AssetsBundle("/assets", "/static", "index.html", "static"));
        guiceBundle = GuiceBundle.newBuilder<DemoConfig>()
                .addModule(module)
                .setConfigClass(DemoConfig::class.java)
                .build()
        configuration.addBundle(ViewBundle())

        configuration.addBundle(guiceBundle)
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

        val injector = guiceBundle.injector

        env.metrics().apply {
            register("coverCount", injector.getInstance(CoverCountMetric::class.java))
            register("coverSize", injector.getInstance(CoverSizeMetric::class.java))
        }
        env.applicationContext.apply {
            setAttribute(MetricsServlet.METRICS_REGISTRY, env.metrics())
            setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, env.healthChecks())
        }
        env.servlets().apply {
            addServlet("admin", AdminServlet()).addMapping("/admin")
            addServlet("metrics", MetricsServlet()).addMapping("/admin/metrics")
            addServlet("healthcheck", HealthCheckServlet()).addMapping("/admin/healthcheck")
            addServlet("ping", PingServlet()).addMapping("/admin/ping")
            addServlet("pprof", CpuProfileServlet()).addMapping("/admin/pprof")
        }

        @Provider
        class MyExceptionMapper : ExceptionMapper<Exception> {
            override fun toResponse(ex: Exception): Response {
                val emailService = injector.getInstance(EmailService::class.java)

                val body = StringBuilder(ex.message + " " + ex.javaClass)
                ex.stackTrace.forEach {
                    body.append("\n").append(it)
                }
                emailService.sendEmail("cedric@beust.com", "New exception on http://perryrhodan.us ${ex.message}",
                        body.toString())

                return Response.status(500)
                        .entity("Something went wrong, the owners have been notified")
                        .type(MediaType.TEXT_PLAIN)
                        .build()
            }
        }
        if (module.isProduction()) {
            env.jersey().register(MyExceptionMapper())
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