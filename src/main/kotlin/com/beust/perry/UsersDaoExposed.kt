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

//    @Throws(WebApplicationException::class)
//    private fun findUserOrThrow(login: String): User {
//        val found = findUser(login)
//        if (found.success) return found.result!!
//        else throw WebApplicationException("User not found: $login")
//    }

    override fun setPassword(login: String, password: String): DaoResult<Unit> {
        val result: DaoResult<Unit> = transaction {
            // Throw if not found
            val found = findUser(login)
            if (found.success) {
                val hashedPassword = Passwords.hashPassword(password)
                Users.update({ Users.login eq login }) {
                    it[Users.salt] = hashedPassword.salt
                    it[Users.password] = hashedPassword.hashedPassword
                    it[Users.authToken] = null
                }
                DaoResult(true)
            } else {
                DaoResult(false, message = "User not found: $login")
            }
        }
        return result
    }

    /**
     * The auth_token columns contains the last three tokens used, separated by spaces, which allows
     * users to log in from multiple browsers/computers.
     */
    override fun updateAuthToken(login: String, authToken: String): DaoResult<Unit> {
        val shortAuthToken = Passwords.rewriteAuthToken(authToken)

        val found = findUser(login)
        val result: DaoResult<Unit> = if (found.success) {
            val user = found.result!!
            val at = user.authToken
            val authTokens =
                    if (at != null) ArrayList(at.split(" "))
                    else arrayListOf<String>()
            authTokens.add(0, shortAuthToken)
            val newAuthTokens = LinkedHashSet(authTokens).take(3).joinToString(" ")
            transaction {
                Users.update({ Users.login eq login }) {
                    it[Users.authToken] = newAuthTokens
                }
            }
            DaoResult(true)
        } else {
            DaoResult(false, message = "User not found: $login")
        }
        return result
    }

    private fun userFromRow(row: ResultRow, login: String = row[Users.login]): User {
        return User(login, row[Users.name], row[Users.email], row[Users.password],
                row[Users.salt], row[Users.authToken], row[Users.level], row[Users.tempLink])
    }

    override fun findUser(login: String): DaoResult<User> {
        val result = transaction {
            val row = Users.select { Users.login eq login }.firstOrNull()
            if (row != null) {
                DaoResult(true, userFromRow(row, login))
            } else {
                DaoResult(false, message = "User not found: $login")
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

    @Throws(WebApplicationException::class)
    override fun verifyAccount(tempLink: String): DaoResult<Unit> {
        return transaction {
            val row = Users.select { Users.tempLink eq tempLink }.firstOrNull()
            if (row != null) {
                Users.update({ Users.tempLink eq tempLink }) {
                    it[Users.tempLink] = null
                }
                val login = row[Users.login]
                val email = row[Users.email]
                log.info("Successfully verified account $login $email")
                DaoResult(true)
            } else {
                DaoResult(false, message = "Invalid verification link")
            }
        }
    }
}
