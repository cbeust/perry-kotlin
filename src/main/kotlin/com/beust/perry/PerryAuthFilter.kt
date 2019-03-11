package com.beust.perry

import io.dropwizard.auth.Authenticator
import io.dropwizard.auth.Authorizer
import io.dropwizard.auth.basic.BasicCredentials
import java.security.Principal
import java.util.*

class User(val n: String) : Principal {
    override fun getName(): String {
        return n
    }
}

class PerryAuthenticator:  Authenticator<BasicCredentials, User> {
    override fun authenticate(credentials: BasicCredentials): Optional<User> {
        val username = credentials.username
        val password = credentials.password
        return Optional.of(User(username))
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
