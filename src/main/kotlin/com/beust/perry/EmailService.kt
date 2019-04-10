package com.beust.perry

import com.google.inject.Inject
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException
import org.slf4j.LoggerFactory
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.ws.rs.WebApplicationException



class EmailService @Inject constructor (private val properties: TypedProperties) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    private val SMTP = "smtp.gmail.com"

    fun sendEmail(to: String, subject: String, message: String) {
        val mailProperties = Properties().also {
            it["mail.smtp.auth"] = true
            it["mail.smtp.starttls.enable"] = "true"
            it["mail.smtp.host"] = SMTP
            it["mail.smtp.port"] = "587"
            it["mail.smtp.ssl.trust"] = SMTP
        }

        val user = properties.getRequired(LocalProperty.EMAIL_USERNAME)
        val password = properties.getRequired(LocalProperty.EMAIL_PASSWORD)

        // Get the default Session object.
        val session = Session.getInstance(mailProperties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication
                    = PasswordAuthentication(user, password)
        })

        try {
            with(MimeMessage(session)) {
                addRecipient(Message.RecipientType.TO, InternetAddress(to))
                this.subject = subject
                setText(message)
                if (Vars.isProduction()) {
                    Transport.send(this)
                } else {
                    log.info("Would send email \"$subject\"")
                }
            }
        } catch (mex: MessagingException) {
            throw WebApplicationException(mex.message, mex)
        }
    }

    fun notifyAdmin(subject: String, body: String) = sendEmail("cedric@beust.com", subject, body)

    fun onUnauthorized(subject: String, body: String)
            = notifyAdmin("Unauthorized access: $subject", body)
}
