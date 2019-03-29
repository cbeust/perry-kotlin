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
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
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

        val injector = guiceBundle.injector
        env.jersey().register(CookieAuthFilter(injector.getInstance(UsersDao::class.java)))

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

        class AdminServletFilter(private val usersDao: UsersDao): Filter {
            override fun init(config: FilterConfig) {}
            override fun destroy() {}

            override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
                // Doesn't work on Chrome
//                authenticateFromHeaders(request, response, chain)

                authenticateFromCookies(request, response, chain)
//                authenticateFromContext(request, response, chain)
            }

            private fun authenticateFromCookies(request: ServletRequest, response: ServletResponse,
                    chain: FilterChain) {
                val authToken = (request as HttpServletRequest).cookies.find { it.name == "auth_token" }
                if (authToken != null) {
                    val user = usersDao.findByAuthToken(authToken.value)
                    println("User: $user")
                }
                println("Cookies check")
            }

//            private fun authenticateFromContext(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
//                val pc = guiceBundle.injector.getInstance(PerryContext::class.java)
//                if (pc.user != null && pc?.user?.level == 0) {
//                    chain.doFilter(request, response)
//                } else {
//                    (response as HttpServletResponse).sendError(
//                            HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
//                }
//            }

            private fun authenticateFromHeaders(request: ServletRequest, response: ServletResponse,
                    chain: FilterChain) {
                val r = request as HttpServletRequest
                val auth = r.getHeader("Authorization")
                val resp = response as HttpServletResponse
                if (auth == null) {
                    resp.apply {
                        addHeader("WWW-Authenticate", "Basic BASIC-AUTH-REALM")
                        setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                    }
                } else {
                    if (auth.toLowerCase().startsWith("basic")) {
                        // Authorization: Basic base64credentials
                        val base64Credentials = auth.substring("Basic".length).trim()
                        val credDecoded = Base64.getDecoder().decode(base64Credentials)
                        val credentials = String(credDecoded, StandardCharsets.UTF_8)
                        // credentials = username:password
                        val values = credentials.split(":")
                        val usersDao = guiceBundle.injector.getInstance(UsersDao::class.java)
                        val user = usersDao.findUser(values[0])
                        if (user != null && user.level == 0) {
                            chain.doFilter(request, response)
                        } else {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")
                        }
                    }
                }
            }
        }

        env.applicationContext.addFilter(FilterHolder(GuiceFilter()), "/*",
                EnumSet.of(DispatcherType.REQUEST))

        env.applicationContext.addFilter(FilterHolder(
                CookieAuthFilter(injector.getInstance(UsersDao::class.java))),
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
        if (module.isProduction()) {
            env.jersey().register(MyExceptionMapper())
        }

        env.healthChecks().register("template", DemoCheck(config.version))
    }

}

