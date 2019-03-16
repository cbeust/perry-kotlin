package com.beust.perry

import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun main(args: Array<String>) {
    SendEmail.sendEmail("cedric@beust.com", "Subject here", "Message here")
}

internal object SendEmail {
    const val SMTP = "smtp.gmail.com"

    fun sendEmail(to: String, subject: String, message: String) {
        val properties = Properties().also {
            it["mail.smtp.auth"] = true
            it["mail.smtp.starttls.enable"] = "true"
            it["mail.smtp.host"] = SMTP
            it["mail.smtp.port"] = "587"
            it["mail.smtp.ssl.trust"] = SMTP
        }

        val user = ""
        val password = ""

        // Get the default Session object.
        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication
                    = PasswordAuthentication(user, password)
        })

        try {
            with(MimeMessage(session)) {
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                this.subject = subject
                setText(message)
                Transport.send(this)
            }
        } catch (mex: MessagingException) {
            mex.printStackTrace()
        }

    }
}
