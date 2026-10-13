package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.enums.MedidaModeracao;

import java.time.LocalDateTime;

/**
 * Passado de moderação do responsável pelo conteúdo denunciado, entregue
 * junto da denúncia pendente. É o que torna a escada operável: sem ver que
 * alguém já foi advertido duas vezes, o admin não tem como escalonar.
 *
 * {@code medidaSugerida} é sugestão, não automação — quem decide é o admin.
 */
public record HistoricoModeracaoResponse(
        long advertencias,
        long suspensoes,
        boolean suspensoAgora,
        LocalDateTime suspensoAte,
        MedidaModeracao medidaSugerida,
        String justificativaSugestao
) {
}
