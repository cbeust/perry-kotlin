package com.beust.perry

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UsersDaoExposed: UsersDao {
    override fun updateAuthToken(username: String, authToken: String) {
        transaction {
            Users.update({ Users.login eq username}) {
                it[Users.authToken] = authToken
            }
        }
    }

    override fun findUser(loginName: String): User? {
        val result =
            transaction {
                val row = Users.select { Users.login eq loginName }.firstOrNull()
                if (row != null) {
                    User(loginName, row[Users.name], row[Users.level], row[Users.email])
                } else {
                    null
                }
            }
        return result
    }
}
