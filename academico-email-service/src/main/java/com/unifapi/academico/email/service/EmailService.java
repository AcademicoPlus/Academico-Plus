package com.unifapi.academico.email.service;

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

    @Value("${app.email.from:Academico+ <nao-responda@resend.dev>}")
    private String emailFrom;

    private final RestClient restClient;
    private final TemplateEngine templateEngine;

    public EmailService(TemplateEngine templateEngine) {
        this.restClient = RestClient.create();
        this.templateEngine = templateEngine;
    }

    /**
     * Envia o e-mail com o código OTP de verificação/recuperação de senha.
     * Retorna false só quando a chave do Resend está configurada mas o envio falhou de
     * fato (API fora do ar, resposta não-2xx) — aciona a DLQ do consumer. Sem chave
     * configurada, assume-se ambiente de dev e cai no modo simulação (sempre "sucesso").
     */
    public boolean enviarCodigoVerificacao(String destinatario, String codigo, int minutosExpiracao) {
        String sanitizedCode = codigo != null ? codigo.trim() : "";
        String subject = sanitizedCode + " é o seu código de verificação";

        Context context = new Context();
        context.setVariable("codigo", sanitizedCode);
        context.setVariable("minutosExpiracao", minutosExpiracao);
        String html = templateEngine.process("email/otp-verificacao", context);

        if (resendApiKey == null || !resendApiKey.startsWith("re_")) {
            log.info("\n==================================================" +
                    "\nDEBUG: Modo Simulação Ativo" +
                    "\nPARA: {}" +
                    "\nASSUNTO: {}" +
                    "\nCÓDIGO: {}" +
                    "\n==================================================",
                    destinatario, subject, sanitizedCode);
            return true;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "from", emailFrom,
                    "to", List.of(destinatario),
                    "subject", subject,
                    "html", html);

            var response = restClient.post()
                    .uri(resendApiUrl)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("E-mail enviado com sucesso via Resend para {}", destinatario);
                return true;
            }

            log.error("Resend retornou status não-2xx para {}: {}", destinatario, response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Erro ao conectar com Resend: {}", e.getMessage());
            return false;
        }
    }
}
