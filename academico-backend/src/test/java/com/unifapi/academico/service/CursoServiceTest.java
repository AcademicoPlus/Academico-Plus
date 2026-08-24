package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.dto.usuario.CursoResponse;
import com.unifapi.academico.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.unifapi.academico.support.EntityFixtures.curso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link CursoService}.
 *
 * <p>Discrepância em relação ao briefing original: {@link CursoService} expõe
 * apenas o método de leitura {@code listarAtivos()}. Não há métodos de
 * criação/atualização/remoção nesta classe (o CRUD de cursos, se existir,
 * não está implementado aqui), por isso apenas o cenário de leitura é testado.</p>
 */
@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    private CursoService service;

    @BeforeEach
    void setUp() {
        service = new CursoService(cursoRepository);
    }

    @Test
    void deveListarApenasCursosAtivosOrdenadosPorNomeUsandoOMetodoDoRepositorio() {
        Curso cursoA = curso("Análise e Desenvolvimento de Sistemas");
        Curso cursoB = curso("Ciência da Computação");
        when(cursoRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(cursoA, cursoB));

        List<CursoResponse> resultado = service.listarAtivos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(CursoResponse::nome)
                .containsExactly("Análise e Desenvolvimento de Sistemas", "Ciência da Computação");
        // O próprio nome do método do repositório já garante o filtro "AtivoTrue" e a
        // ordenação "OrderByNomeAsc"; o teste comprova que é exatamente esse método que é usado.
        verify(cursoRepository).findByAtivoTrueOrderByNomeAsc();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaCursosAtivos() {
        when(cursoRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of());

        List<CursoResponse> resultado = service.listarAtivos();

        assertThat(resultado).isEmpty();
    }
}
