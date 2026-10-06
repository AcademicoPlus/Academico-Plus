package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.dto.usuario.NivelAcessoResponse;
import com.unifapi.academico.repository.NivelAcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link NivelAcessoService}.
 *
 * <p>Discrepância em relação ao briefing original: {@link NivelAcessoService} expõe
 * apenas o método de leitura {@code listarTodos()}. Não há métodos de
 * criação/atualização/remoção nesta classe, por isso apenas o cenário de leitura
 * é testado. Além disso, a ordenação por nome é feita no próprio service, em
 * memória, via {@code Comparator.comparing(NivelAcessoResponse::nome)} — o
 * repositório é chamado com um simples {@code findAll()}, sem order-by. Por isso
 * o teste alimenta o mock com uma lista fora de ordem e verifica que o resultado
 * sai ordenado.</p>
 */
@ExtendWith(MockitoExtension.class)
class NivelAcessoServiceTest {

    @Mock
    private NivelAcessoRepository nivelAcessoRepository;

    private NivelAcessoService service;

    @BeforeEach
    void setUp() {
        service = new NivelAcessoService(nivelAcessoRepository);
    }

    @Test
    void deveListarTodosOsNiveisOrdenadosPorNomeEmMemoriaAPartirDeFindAllSemOrderBy() {
        NivelAcesso professor = nivelAcesso("PROFESSOR");
        NivelAcesso administrador = nivelAcesso("ADMINISTRADOR");
        NivelAcesso aluno = nivelAcesso("ALUNO");
        // Propositalmente fora de ordem: o repositório não garante ordenação (findAll simples).
        when(nivelAcessoRepository.findAll()).thenReturn(List.of(professor, administrador, aluno));

        List<NivelAcessoResponse> resultado = service.listarTodos();

        assertThat(resultado).extracting(NivelAcessoResponse::nome)
                .containsExactly("ADMINISTRADOR", "ALUNO", "PROFESSOR");
        verify(nivelAcessoRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaNiveisCadastrados() {
        when(nivelAcessoRepository.findAll()).thenReturn(List.of());

        List<NivelAcessoResponse> resultado = service.listarTodos();

        assertThat(resultado).isEmpty();
    }
}
