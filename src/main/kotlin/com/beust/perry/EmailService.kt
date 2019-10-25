package com.beust.perry

import com.google.inject.Inject
import org.slf4j.LoggerFactory
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.ws.rs.WebApplicationException


interface EmailSender {
    fun send(message: Message)
}

class ProductionEmailSender : EmailSender {
    override fun send(message: Message) {
        Transport.send(message)
    }
}

class FakeEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(FakeEmailSender::class.java)

    override fun send(message: Message) {
        log.info("Would send email: ${message.subject}")
    }
}

class EmailService @Inject constructor (private val properties: TypedProperties, private val emailSender: EmailSender) {
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
                setContent(message, "text/html")
                emailSender.send(this)
            }
        } catch (mex: MessagingException) {
            throw WebApplicationException(mex.message, mex)
        }
    }

    fun notifyAdmin(subject: String, body: String) = sendEmail("cedric@beust.com", subject, body)

    fun onUnauthorized(subject: String, body: String)
            = notifyAdmin("Unauthorized access: $subject", body)
}
