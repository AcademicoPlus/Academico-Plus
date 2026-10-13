package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.enums.MedidaModeracao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Decisão do admin sobre uma denúncia pendente. A medida é sempre escolhida
 * por ele — o serviço apenas sugere com base no histórico do responsável.
 */
public record ResolverDenunciaRequest(

        @NotNull(message = "A medida é obrigatória")
        MedidaModeracao medida,

        /** Obrigatório só para SUSPENSAO; ignorado nas demais medidas. */
        @Min(value = 1, message = "A suspensão deve durar ao menos 1 dia")
        @Max(value = 365, message = "A suspensão não pode passar de 365 dias")
        Integer diasSuspensao,

        /**
         * Texto que o usuário sancionado lê no próximo login. Obrigatório quando
         * a medida pune alguém — ninguém deve ser advertido sem saber por quê.
         */
        @Size(max = 1000, message = "A justificativa não pode passar de 1000 caracteres")
        String justificativa
) {
}
