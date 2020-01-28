package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.servlets.*
import com.google.inject.servlet.GuiceFilter
import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.views.ViewBundle
import org.eclipse.jetty.servlet.FilterHolder
import java.util.*
import javax.servlet.DispatcherType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

class PerryApp : Application<DropWizardConfig>() {
    private lateinit var guiceBundle: GuiceBundle<DropWizardConfig>
    private val config = IConfig.get()
    private val module = PerryModule(config)
    private var bootstrap: Bootstrap<DropWizardConfig>? = null

    override fun initialize(configuration: Bootstrap<DropWizardConfig>) {
        bootstrap = configuration
        configuration.addBundle(AssetsBundle("/static", "/static", "index.html", "static"));
        configuration.addBundle(ViewBundle())
    }

    override fun run(dropWizardConfig: DropWizardConfig, env: Environment) {
        val dp = dropWizardConfig.dbProvider
        val provider =
            if (dp != null) {
                when (dropWizardConfig.dbProvider) {
                    "local" -> DbProviderLocal(config)
                    "production" -> DbProviderHeroku()
                    "localToProduction" -> DbProviderLocalToProduction()
                    else -> throw IllegalArgumentException("UNKNOWN DB PROVIDER: ${dropWizardConfig.dbProvider}")
                }
            } else {
                if (IConfig.isProduction) DbProviderHeroku() else DbProviderLocal(config)
            }

        guiceBundle = GuiceBundle.newBuilder<DropWizardConfig>()
                .addModule(module)
                .addModule(DatabaseModule(config, provider))
                .setConfigClass(DropWizardConfig::class.java)
                .build()
        bootstrap!!.addBundle(guiceBundle)

        listOf(PerryService::class.java).forEach {
            env.jersey().register(it)
        }

        env.jersey().register(AuthValueFactoryProvider.Binder(User::class.java))

        val injector = guiceBundle.injector
        val cookieAuthFilter = CookieAuthFilter(
                injector.getInstance(UsersDao::class.java),
                injector.getInstance(EmailService::class.java))
        env.jersey().register(cookieAuthFilter)

        val perryMetrics = injector.getInstance(PerryMetrics::class.java)
        val metricRegistry = injector.getInstance(MetricRegistry::class.java)
        perryMetrics.registerMetrics()
        env.servlets().apply {
            addServlet("admin", AdminServlet()).apply {
                addMapping("/admin")
            }
            addServlet("metrics", MetricsServlet(metricRegistry)).addMapping("/admin/metrics")
            addServlet("healthcheck", HealthCheckServlet()).addMapping("/admin/healthcheck")
            addServlet("ping", PingServlet()).addMapping("/admin/ping")
            addServlet("pprof", CpuProfileServlet()).addMapping("/admin/pprof")
        }
        env.applicationContext.apply {
            setAttribute(MetricsServlet.METRICS_REGISTRY, env.metrics())
            setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, env.healthChecks())
        }

        env.applicationContext.addFilter(FilterHolder(GuiceFilter()), "/*",
                EnumSet.of(DispatcherType.REQUEST))

        env.applicationContext.addFilter(FilterHolder(
                cookieAuthFilter),
                "/admin/*",
                EnumSet.of(DispatcherType.REQUEST))

        @Provider
        class MyExceptionMapper : ExceptionMapper<Exception> {
            override fun toResponse(ex: Exception): Response {
                val emailService = injector.getInstance(EmailService::class.java)

                val body = StringBuilder(ex.message + " " + ex.javaClass)
                var email = false
                ex.stackTrace.forEach {
                    if (it.className.contains("beust")) email = true
                    body.append("\n").append(it)
                }
                if (email) {
                    emailService.sendEmail("cedric@beust.com", "New exception on http://perryrhodan.us ${ex.message}",
                            body.toString())
                }

                return Response.status(500)
                        .entity("Something went wrong, the owners have been notified")
                        .type(MediaType.TEXT_PLAIN)
                        .build()
            }
        }
        if (IConfig.isProduction) {
            env.jersey().register(MyExceptionMapper())
        }

        env.healthChecks().register("template", DemoCheck(dropWizardConfig.version))
    }

}

