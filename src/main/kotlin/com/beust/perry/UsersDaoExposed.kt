package com.beust.perry

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.WebApplicationException

class UsersDaoExposed: UsersDao {
    private val log = LoggerFactory.getLogger(UsersDaoExposed::class.java)

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

    private fun userFromRow(row: ResultRow, login: String = row[Users.login]): User {
        return User(login, row[Users.name], row[Users.email], row[Users.password],
                row[Users.salt], row[Users.authToken], row[Users.level], row[Users.tempLink])
    }

    @Throws(UserNotFoundException::class)
    override fun findUser(login: String): User {
        val result = transaction {
            val row = Users.select { Users.login eq login }.firstOrNull()
            if (row != null) {
                userFromRow(row, login)
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
                userFromRow(row)
            } else {
                null
            }
        }
        return result
    }

    override fun createUser(user: User): Boolean {
        val insert = transaction {
            Users.insert {
                it[login] = user.login
                it[name] = user.fullName
                it[email] = user.email
                it[password] = user.password
                it[salt] = user.salt
                it[authToken] = user.authToken
                it[level] = user.level
                it[tempLink] = user.tempLink
            }
        }
        return true
    }

    override fun verifyAccount(tempLink: String) {
        transaction {
            val row = Users.select { Users.tempLink eq tempLink }.firstOrNull()
            if (row != null) {
                Users.update({ Users.tempLink eq tempLink }) {
                    it[Users.tempLink] = null
                }
                val login = row[Users.login]
                val email = row[Users.email]
                log.info("Successfully verified account $login $email")
            } else {
                throw WebApplicationException("Invalid verification link")
            }
        }
    }
}
