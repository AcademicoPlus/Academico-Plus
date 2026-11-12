package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.dto.projeto.AvisoProjetoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoProjetoServiceTest {

    @Mock private ProjetoCanceladoAvisoRepository avisoRepository;

    @InjectMocks private AvisoProjetoService avisoProjetoService;

    private static ProjetoCanceladoAviso aviso(String tipo, String motivo) {
        return ProjetoCanceladoAviso.builder()
                .id(UUID.randomUUID())
                .projetoNome("Projeto X")
                .projetoData(LocalDate.now())
                .tipo(tipo)
                .motivo(motivo)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    void listarPendentesDevolveTipoEMotivoSemMarcarComoVisto() {
        UUID usuarioId = UUID.randomUUID();
        ProjetoCanceladoAviso cancelado = aviso(ProjetoCanceladoAviso.TIPO_CANCELAMENTO, "Falta de verba");
        ProjetoCanceladoAviso concluido = aviso(ProjetoCanceladoAviso.TIPO_CONCLUSAO, null);
        when(avisoRepository.findByUsuario_IdAndVisualizadoEmIsNullOrderByCriadoEmAsc(usuarioId))
                .thenReturn(List.of(cancelado, concluido));

        List<AvisoProjetoResponse> pendentes = avisoProjetoService.listarPendentes(usuarioId);

        assertThat(pendentes).extracting(AvisoProjetoResponse::tipo).containsExactly("cancelamento", "conclusao");
        assertThat(pendentes.get(0).motivo()).isEqualTo("Falta de verba");
        assertThat(cancelado.getVisualizadoEm()).isNull();
        assertThat(concluido.getVisualizadoEm()).isNull();
    }

    @Test
    void marcarComoVisualizadoRegistraOMomento() {
        UUID usuarioId = UUID.randomUUID();
        ProjetoCanceladoAviso aviso = aviso(ProjetoCanceladoAviso.TIPO_CONCLUSAO, null);
        when(avisoRepository.findByIdAndUsuario_Id(aviso.getId(), usuarioId)).thenReturn(Optional.of(aviso));

        avisoProjetoService.marcarComoVisualizado(aviso.getId(), usuarioId);

        assertThat(aviso.getVisualizadoEm()).isNotNull();
    }

    @Test
    void marcarComoVisualizadoNaoMexeNoAvisoDeOutraPessoa() {
        UUID avisoAlheio = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(avisoRepository.findByIdAndUsuario_Id(avisoAlheio, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> avisoProjetoService.marcarComoVisualizado(avisoAlheio, usuarioId))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
