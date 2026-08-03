package com.unifapi.academico.scheduler;

import com.unifapi.academico.repository.TokenRevogadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final TokenRevogadoRepository tokenRevogadoRepository;

    // Executa diariamente às 03:00 da manhã (Expressão CRON: segundo minuto hora dia mes dia-da-semana)
    @Scheduled(cron = "0 0 3 * * ?")
    public void limparTokensExpirados() {
        log.info("Iniciando limpeza de tokens revogados e expirados...");
        tokenRevogadoRepository.deletarExpiradosAte(LocalDateTime.now());
        log.info("Limpeza de tokens concluída.");
    }
}