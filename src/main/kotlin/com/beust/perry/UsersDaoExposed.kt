package com.beust.perry

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*
import javax.ws.rs.WebApplicationException

class UsersDaoExposed: UsersDao {
    /**
     * The auth_token columns contains the last three tokens used, separated by spaces, which allows
     * users to log in from multiple browsers/computers.
     */
    override fun updateAuthToken(login: String, longAuthToken: String) {
        val authToken = Passwords.rewriteAuthToken(longAuthToken)

        val user = findUser(login)
        if (user != null) {
            val at = user.authToken
            val authTokens =
                if (at != null) ArrayList(at.split(" "))
                else arrayListOf()
            authTokens.add(0, authToken)
            val newAuthTokens = LinkedHashSet(authTokens).take(3).joinToString(" ")
            transaction {
                Users.update({ Users.login eq login }) {
                    it[Users.authToken] = newAuthTokens
                }
            }
        } else {
            throw WebApplicationException("Couldn't find user $login")
        }
    }

    override fun findUser(login: String): User? {
        val result = transaction {
            val row = Users.select { Users.login eq login }.firstOrNull()
            if (row != null) {
                User(login, row[Users.name], row[Users.level], row[Users.email], row[Users.authToken],
                        row[Users.salt], row[Users.password])
            } else {
                null
            }
        }
        return result
    }

    override fun findByAuthToken(longAuthToken: String): User? {
        val authToken = Passwords.rewriteAuthToken(longAuthToken)
        val result = transaction {
            val row = Users.select { Users.authToken like "%$authToken%"}.firstOrNull()
            if (row != null) {
                User(row[Users.login], row[Users.name], row[Users.level], row[Users.email], row[Users.authToken],
                        row[Users.salt], row[Users.password])
            } else {
                null
            }
        }
        return result
    }
}
