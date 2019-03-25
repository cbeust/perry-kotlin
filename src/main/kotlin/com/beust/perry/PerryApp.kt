package com.beust.perry

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.servlets.*
import com.google.inject.servlet.GuiceFilter
import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.views.ViewBundle
import org.eclipse.jetty.servlet.FilterHolder
import java.util.*
import javax.servlet.*
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider




class PerryApp : Application<DemoConfig>() {
    private lateinit var guiceBundle: GuiceBundle<DemoConfig>
    private val module = PerryModule()
    private var bootstrap: Bootstrap<DemoConfig>? = null

    override fun initialize(configuration: Bootstrap<DemoConfig>) {
        bootstrap = configuration
        configuration.addBundle(AssetsBundle("/assets", "/static", "index.html", "static"));
        configuration.addBundle(ViewBundle())
    }

    override fun run(config: DemoConfig, env: Environment) {
        val dp = config.dbProvider
        val provider =
            if (dp != null) {
                when (config.dbProvider) {
                    "local" -> DbProviderLocal()
                    "production" -> DbProviderHeroku()
                    "localToProduction" -> DbProviderLocalToProduction()
                    else -> throw IllegalArgumentException("UNKNOWN DB PROVIDER: ${config.dbProvider}")
                }
            } else {
                if (module.isProduction()) DbProviderHeroku() else DbProviderLocal()
            }

        guiceBundle = GuiceBundle.newBuilder<DemoConfig>()
                .addModule(module)
                .addModule(DatabaseModule(provider))
                .setConfigClass(DemoConfig::class.java)
                .build()
        bootstrap!!.addBundle(guiceBundle)

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

        val metricRegistry = MetricRegistry()
        env.servlets().apply {
            addServlet("admin", AdminServlet()).apply {
                addMapping("/admin")
            }
            addServlet("metrics", MetricsServlet(metricRegistry)).addMapping("/admin/metrics")
            addServlet("healthcheck", HealthCheckServlet()).addMapping("/admin/healthcheck")
            addServlet("ping", PingServlet()).addMapping("/admin/ping")
            addServlet("pprof", CpuProfileServlet()).addMapping("/admin/pprof")
        }
        metricRegistry.apply {
            register("coverCount", injector.getInstance(CoverCountMetric::class.java))
            register("coverSize", injector.getInstance(CoverSizeMetric::class.java))
        }
        env.applicationContext.apply {
            setAttribute(MetricsServlet.METRICS_REGISTRY, env.metrics())
            setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, env.healthChecks())
        }

        class AdminServletFilter: Filter {
            override fun init(config: FilterConfig) {}
            override fun destroy() {}

            override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
                val pc = guiceBundle.injector.getInstance(PerryContext::class.java)
                if (pc.user != null && pc?.user?.level == 0) {
                    chain.doFilter(request, response)
                } else {
                    (response as HttpServletResponse).sendError(
                            HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                }
            }
        }

        env.applicationContext.addFilter(FilterHolder(GuiceFilter()), "/*",
                EnumSet.of(DispatcherType.REQUEST))

        env.applicationContext.addFilter(FilterHolder(
                AdminServletFilter()), "/admin/*", EnumSet.of(DispatcherType.REQUEST))

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
        if (module.isProduction()) {
            env.jersey().register(MyExceptionMapper())
        }

//        env.jersey().register(AuthDynamicFeature(PerryAuthFilter()))

//                PerryAuthFilterBuilder()
//                        .setAuthenticator(PerryAuthenticator())
//                        .setAuthorizer(PerryAuthorizer())
//                        .buildAuthFilter()
//        ))

        env.healthChecks().register("template", DemoCheck(config.version))
    }

}