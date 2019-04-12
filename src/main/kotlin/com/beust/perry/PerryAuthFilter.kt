package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.auth.AuthFilter
import org.glassfish.jersey.server.ContainerRequest
import org.slf4j.LoggerFactory
import java.security.Principal
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext

class User(val login: String, val fullName: String, val level: Int, val email: String,
        val authToken: String?, val salt: ByteArray?, val password: ByteArray?) : Principal {
    override fun getName() = login
}

/**
 * A filter used for both the AdminServlet (which requires a regular Filter) and for all
 * requests to the Services class (which requires an AuthFilter, a ContainerRequestFilter.
 * This filter manages cookie-based authentication.
 */
class CookieAuthFilter @Inject constructor(private val usersDao: UsersDao, private val emailService: EmailService)
        : Filter, AuthFilter<HttpServletRequest, User>()
{
    private val log = LoggerFactory.getLogger(CookieAuthFilter::class.java)

    override fun destroy() {}
    override fun init(filterConfig: FilterConfig?) {}

    /**
     * Used for /admin filtering.
     */
    override fun doFilter(req: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val request = req as HttpServletRequest
        val authToken = Cookies.findCookie(request, PerryCookie.AUTH_TOKEN)
        if (authToken != null) {
            val user = usersDao.findByAuthToken(authToken.value)
            if (user == null || user.level != 0) {
                emailService.onUnauthorized("/admin",
                        if (user == null) "No cookies found" else "${user.fullName}'s level is ${user.level}")
                (response as HttpServletResponse).sendError(Response.Status.UNAUTHORIZED.statusCode,
                        "Authentication required")
            }
        }
        if (listOf("50.225.220", "73.15.2").none { req.remoteAddr.startsWith(it) }) {
            emailService.notifyAdmin("Allowed /admin access from " + req.remoteAddr, "")
        }
        chain.doFilter(request, response)
    }

    /**
     * User for all other filtering than /admin.
     */
    override fun filter(requestContext: ContainerRequestContext) {
        val request = requestContext.request as ContainerRequest
        val authToken = Cookies.findCookie(request, PerryCookie.AUTH_TOKEN)
        if (authToken != null) {
            val user = usersDao.findByAuthToken(Passwords.rewriteAuthToken(authToken.value))
            if (user != null) {
                log.info("Identified user ${user.fullName}")
                request.securityContext = object : SecurityContext {
                    override fun isUserInRole(role: String?) = true

                    override fun getAuthenticationScheme() = ""

                    override fun getUserPrincipal() = user

                    override fun isSecure() = true
                }
            }
        }
    }

}
