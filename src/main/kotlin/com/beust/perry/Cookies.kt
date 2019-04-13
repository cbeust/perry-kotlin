package com.beust.perry

import org.glassfish.jersey.server.ContainerRequest
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie

enum class PerryCookie(val value: String) {
    AUTH_TOKEN("authToken")
}

object Cookies {
    fun findCookie(request: ContainerRequest, pc: PerryCookie)
            = request.cookies[pc.value]

    fun findCookie(request: HttpServletRequest, pc: PerryCookie)
            = request.cookies?.find { it.name == pc.value}

    fun createAuthCookie(authToken: String, durationSeconds: Int = Duration.of(7, ChronoUnit.DAYS).seconds.toInt())
            : NewCookie {
        val cookie = Cookie("authToken", authToken, "/", null, 1)
        return NewCookie(cookie, null, durationSeconds, false)
    }

    fun clearAuthCookie() = createAuthCookie("", 0)
}
