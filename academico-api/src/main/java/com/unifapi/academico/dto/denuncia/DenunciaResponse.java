package com.unifapi.academico.dto.denuncia;

import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import com.unifapi.academico.domain.enums.MedidaModeracao;
import com.unifapi.academico.domain.enums.StatusDenuncia;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.Denuncia;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record DenunciaResponse(
        UUID id,
        TipoAlvoDenuncia tipoAlvo,
        AlvoDenunciadoResponse alvo,
        /** Quem pode ser sancionado por este conteúdo. */
        UsuarioResumoResponse responsavel,
        UsuarioResumoResponse denunciante,
        CategoriaDenuncia categoria,
        String motivo,
        StatusDenuncia status,
        MedidaModeracao medida,
        /** Preenchido só na fila de pendentes (ver DenunciaService.listarPendentes). */
        HistoricoModeracaoResponse historicoResponsavel,
        LocalDateTime criadoEm,
        LocalDateTime analisadoEm
) {

    public static DenunciaResponse fromEntity(Denuncia denuncia) {
        return fromEntity(denuncia, null);
    }

    public static DenunciaResponse fromEntity(Denuncia denuncia, HistoricoModeracaoResponse historico) {
        Usuario responsavel = denuncia.getResponsavel();

        return new DenunciaResponse(
                denuncia.getId(),
                denuncia.getTipoAlvo(),
                montarAlvo(denuncia),
                responsavel != null ? UsuarioResumoResponse.fromEntity(responsavel) : null,
                UsuarioResumoResponse.fromEntity(denuncia.getDenunciante()),
                denuncia.getCategoria(),
                denuncia.getMotivo(),
                denuncia.getStatus(),
                denuncia.getMedida(),
                historico,
                denuncia.getCriadoEm(),
                denuncia.getAnalisadoEm()
        );
    }

    private static AlvoDenunciadoResponse montarAlvo(Denuncia denuncia) {
        return switch (denuncia.getTipoAlvo()) {
            case AVALIACAO -> AlvoDenunciadoResponse.deAvaliacao(denuncia.getAvaliacao());
            case PROJETO -> AlvoDenunciadoResponse.deProjeto(denuncia.getProjeto());
            case PERFIL -> AlvoDenunciadoResponse.dePerfil(denuncia.getUsuarioDenunciado());
        };
    }
}
