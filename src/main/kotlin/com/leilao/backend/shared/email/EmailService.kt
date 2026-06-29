package com.leilao.backend.shared.email

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val fromAddress: String
) {

    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendPasswordResetCode(email: String, code: String) {
        val message = SimpleMailMessage()
        message.from = fromAddress
        message.setTo(email)
        message.subject = "Bid Toys - Código de recuperação de senha"
        message.text = """
            Olá!

            Seu código para redefinir a senha da Bid Toys é: $code

            Válido por 15 minutos. Não compartilhe com ninguém.

            Se você não solicitou a recuperação de senha, ignore este e-mail.
        """.trimIndent()
        mailSender.send(message)
        log.info("[Email] Código de reset enviado para {}", email)
    }
}
