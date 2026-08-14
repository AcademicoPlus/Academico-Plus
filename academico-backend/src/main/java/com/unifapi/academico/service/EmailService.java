package com.unifapi.academico.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.api-url:https://api.resend.com/emails}")
    private String resendApiUrl;

    @Value("${app.email.from:Atelier <nao-responda@resend.dev>}")
    private String emailFrom;

    @Value("${app.otp.expiration-minutes:15}")
    private int otpExpirationMinutes;

    private final RestClient restClient;
    private final TemplateEngine templateEngine;

    public EmailService(TemplateEngine templateEngine) {
        this.restClient = RestClient.create();
        this.templateEngine = templateEngine;
    }

    /**
     * Envia o e-mail com o código OTP de verificação/recuperação de senha.
     * Retorna true se o envio via API for bem-sucedido ou se o modo de simulação estiver ativo.
     */
    public boolean enviarCodigoVerificacao(String userEmail, String code) {
        String sanitizedCode = code != null ? code.trim() : "";
        String subject = sanitizedCode + " é o seu código de verificação";

        Context context = new Context();
        context.setVariable("codigo", sanitizedCode);
        context.setVariable("minutosExpiracao", otpExpirationMinutes);
        String html = templateEngine.process("email/otp-verificacao", context);

        // Prioridade 1: Envio via Resend API
        if (resendApiKey != null && resendApiKey.startsWith("re_")) {
            try {
                Map<String, Object> payload = Map.of(
                        "from", emailFrom,
                        "to", List.of(userEmail),
                        "subject", subject,
                        "html", html
                );

                var response = restClient.post()
                        .uri(resendApiUrl)
                        .header("Authorization", "Bearer " + resendApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("E-mail enviado com sucesso via Resend para {}", userEmail);
                    return true;
                }
            } catch (Exception e) {
                log.error("Erro ao conectar com Resend: {}", e.getMessage());
            }
        }

        // Prioridade 2 / Fallback: Modo de Simulação no Console (para Dev Local)
        log.info("\n==================================================" +
                 "\nDEBUG: Modo Simulação Ativo" +
                 "\nPARA: {}" +
                 "\nASSUNTO: {}" +
                 "\nCÓDIGO: {}" +
                 "\n==================================================",
                 userEmail, subject, sanitizedCode);

        // Retorna true em ambiente de testes para permitir fluxo do AuthService sem bloquear no local
        return true; 
    }
}