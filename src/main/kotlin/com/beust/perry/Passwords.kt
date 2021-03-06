package com.beust.perry

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object Passwords {
    class HashedPassword(val salt: ByteArray, val hashedPassword: ByteArray)

    /**
     * @return a pair of <salt, hashed password> for the given password.
     */
    fun hashPassword(password: String): HashedPassword {
        SecureRandom().let { random ->
            val salt = ByteArray(16)
            random.nextBytes(salt)
            return hashPassword(password, salt)
        }
    }

    fun verifyPassword(password: String, salt: ByteArray, expected: ByteArray) : Boolean
        = expected.contentEquals(hashPassword(password, salt).hashedPassword)

    private fun hashPassword(password: String, salt: ByteArray): HashedPassword {
        MessageDigest.getInstance("SHA-512").let { md ->
            md.update(salt)
            val result = md.digest(password.toByteArray(StandardCharsets.UTF_8))
            return HashedPassword(salt, result)
        }
    }

    fun rewriteAuthToken(authToken: String) = authToken.take(16)
}

fun main(args: Array<String>) {
    val pair = Passwords.hashPassword("cedric")
    println(Passwords.verifyPassword("cedric", pair.salt, pair.hashedPassword))
    println(Passwords.verifyPassword("cedric2", pair.salt, pair.hashedPassword))
}
