package com.beust.perry

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class UsersDaoExposed: UsersDao {
    override fun setPassword(login: String, password: String) {
        transaction {
            // Will throw if the user doesn't exist
            findUser(login)
            val hashedPassword = Passwords.hashPassword(password)
            Users.update({ Users.login eq login }) {
                it[Users.salt] = hashedPassword.salt
                it[Users.password] = hashedPassword.hashedPassword
                it[Users.authToken] = null
            }
        }
    }

    /**
     * The auth_token columns contains the last three tokens used, separated by spaces, which allows
     * users to log in from multiple browsers/computers.
     */
    override fun updateAuthToken(login: String, authToken: String) {
        val shortAuthToken = Passwords.rewriteAuthToken(authToken)

        val user = findUser(login)
        val at = user.authToken
        val authTokens =
            if (at != null) ArrayList(at.split(" "))
            else arrayListOf()
        authTokens.add(0, shortAuthToken)
        val newAuthTokens = LinkedHashSet(authTokens).take(3).joinToString(" ")
        transaction {
            Users.update({ Users.login eq login }) {
                it[Users.authToken] = newAuthTokens
            }
        }
    }

    @Throws(UserNotFoundException::class)
    override fun findUser(login: String): User {
        val result = transaction {
            val row = Users.select { Users.login eq login }.firstOrNull()
            if (row != null) {
                User(login, row[Users.name], row[Users.level], row[Users.email], row[Users.authToken],
                        row[Users.salt], row[Users.password])
            } else {
                throw UserNotFoundException("User not found: $login")
            }
        }
        return result
    }

    override fun findByAuthToken(authToken: String): User? {
        val shortAuthToken = Passwords.rewriteAuthToken(authToken)
        val result = transaction {
            val row = Users.select { Users.authToken like "%$shortAuthToken%"}.firstOrNull()
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
