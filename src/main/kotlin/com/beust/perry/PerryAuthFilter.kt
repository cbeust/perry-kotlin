package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.auth.Authenticator
import io.dropwizard.auth.Authorizer
import io.dropwizard.auth.basic.BasicCredentials
import org.apache.commons.logging.LogFactory
import java.security.Principal
import java.util.*

class User(val n: String) : Principal {
    override fun getName(): String {
        return n
    }
}

class PerryContext {
    var user: User? = null
}

class PerryAuthenticator @Inject constructor(private val context: PerryContext)
        :  Authenticator<BasicCredentials, User> {
    val log = LogFactory.getLog(PerryAuthenticator::class.java)

    override fun authenticate(credentials: BasicCredentials): Optional<User> {
        val username = credentials.username
        val password = credentials.password
        log.info("username: $username")
        if (username == "logout") {
            context.user = null
            return Optional.empty()
        } else {
            User(username).let {
                context.user = it
                return Optional.of(it)
            }
        }
    }
}

//@PreMatching
//@Priority(Priorities.AUTHENTICATION)
//class PerryAuthFilter : BasicCredentialAuthFilter<User>() {
//    override fun filter(context: ContainerRequestContext) {
//        val headers = context.headers
//        val username = headers["username"]?.firstOrNull()
//        if (username != null) {
//            val principal = authenticator.authenticate(username)
//            if (! principal.isPresent) {
////                context.securityContext.userPrincipal = principal.get()
//                throw WebApplicationException(Response.Status.UNAUTHORIZED)
//            }
//        } else {
//            throw WebApplicationException(Response.Status.UNAUTHORIZED)
//        }
//    }
//}

class PerryAuthorizer: Authorizer<User> {
    override fun authorize(principal: User?, role: String?): Boolean {
        return true
    }
}
