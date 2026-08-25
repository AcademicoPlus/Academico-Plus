package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.domain.models.UsuarioHabilidade;
import com.unifapi.academico.dto.habilidade.AdicionarHabilidadeUsuarioRequest;
import com.unifapi.academico.dto.habilidade.UsuarioHabilidadeResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.UsuarioHabilidadeRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.habilidade;
import static com.unifapi.academico.support.EntityFixtures.nivelAcesso;
import static com.unifapi.academico.support.EntityFixtures.usuario;
import static com.unifapi.academico.support.EntityFixtures.usuarioHabilidade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link UsuarioHabilidadeService}.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioHabilidadeServiceTest {

    @Mock
    private UsuarioHabilidadeRepository usuarioHabilidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HabilidadeService habilidadeService;

    private UsuarioHabilidadeService service;

    private NivelAcesso nivelAcesso;
    private Usuario usuario;
    private Habilidade habilidade;

    @BeforeEach
    void setUp() {
        service = new UsuarioHabilidadeService(usuarioHabilidadeRepository, usuarioRepository, habilidadeService);

        nivelAcesso = nivelAcesso("ALUNO");
        usuario = usuario("aluno@teste.com", nivelAcesso);
        habilidade = habilidade("Java", "Backend");
    }

    // ============================================
    // listar()
    // ============================================

    @Test
    void deveListarHabilidadesDoUsuarioOrdenadasPorNome() {
        UsuarioHabilidade vinculo = usuarioHabilidade(usuario, habilidade, NivelHabilidade.INTERMEDIARIO);
        when(usuarioHabilidadeRepository.findByUsuario_IdOrderByHabilidade_NomeAsc(usuario.getId()))
                .thenReturn(List.of(vinculo));

        List<UsuarioHabilidadeResponse> resultado = service.listar(usuario.getId());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nivel()).isEqualTo(NivelHabilidade.INTERMEDIARIO);
    }

    // ============================================
    // adicionar()
    // ============================================

    @Test
    void deveLancarExcecaoAoAdicionarHabilidadeParaUsuarioInexistente() {
        UUID usuarioInexistenteId = UUID.randomUUID();
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(habilidade.getId(), null);
        when(usuarioRepository.findById(usuarioInexistenteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adicionar(usuarioInexistenteId, request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(habilidadeService, never()).obterEntidade(any());
        verify(usuarioHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAdicionarHabilidadeInexistente() {
        UUID habilidadeInexistenteId = UUID.randomUUID();
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(habilidadeInexistenteId, null);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(habilidadeService.obterEntidade(habilidadeInexistenteId))
                .thenThrow(new RecursoNaoEncontradoException("Habilidade não encontrada com o ID informado."));

        assertThatThrownBy(() -> service.adicionar(usuario.getId(), request))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(usuarioHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoAdicionarHabilidadeJaPresenteNoPerfil() {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(habilidade.getId(), null);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.adicionar(usuario.getId(), request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está no seu perfil");

        verify(usuarioHabilidadeRepository, never()).save(any());
        verify(habilidadeService, never()).recalcularContadores(any());
    }

    @Test
    void deveDefinirNivelComoIniciantePorPadraoQuandoNivelNaoInformado() {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(habilidade.getId(), null);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(false);
        when(usuarioHabilidadeRepository.save(any(UsuarioHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioHabilidadeResponse response = service.adicionar(usuario.getId(), request);

        assertThat(response.nivel()).isEqualTo(NivelHabilidade.INICIANTE);
        verify(habilidadeService).recalcularContadores(habilidade.getId());
    }

    @Test
    void deveManterNivelInformadoNaRequisicaoAoAdicionar() {
        AdicionarHabilidadeUsuarioRequest request =
                new AdicionarHabilidadeUsuarioRequest(habilidade.getId(), NivelHabilidade.AVANCADO);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(false);
        when(usuarioHabilidadeRepository.save(any(UsuarioHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioHabilidadeResponse response = service.adicionar(usuario.getId(), request);

        assertThat(response.nivel()).isEqualTo(NivelHabilidade.AVANCADO);
    }

    @Test
    void deveDispararRecalculoDeContadoresAoAdicionarComSucesso() {
        AdicionarHabilidadeUsuarioRequest request = new AdicionarHabilidadeUsuarioRequest(habilidade.getId(), null);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(habilidadeService.obterEntidade(habilidade.getId())).thenReturn(habilidade);
        when(usuarioHabilidadeRepository.existsByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(false);
        when(usuarioHabilidadeRepository.save(any(UsuarioHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        service.adicionar(usuario.getId(), request);

        verify(usuarioHabilidadeRepository).flush();
        verify(habilidadeService).recalcularContadores(habilidade.getId());
    }

    // ============================================
    // atualizarNivel()
    // ============================================

    @Test
    void deveLancarExcecaoAoAtualizarNivelDeVinculoInexistente() {
        when(usuarioHabilidadeRepository.findByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizarNivel(usuario.getId(), habilidade.getId(), NivelHabilidade.EXPERT))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("não está no seu perfil");

        verify(usuarioHabilidadeRepository, never()).save(any());
    }

    @Test
    void deveAtualizarNivelDoVinculoExistente() {
        UsuarioHabilidade vinculo = usuarioHabilidade(usuario, habilidade, NivelHabilidade.INICIANTE);
        when(usuarioHabilidadeRepository.findByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(Optional.of(vinculo));
        when(usuarioHabilidadeRepository.save(any(UsuarioHabilidade.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioHabilidadeResponse response =
                service.atualizarNivel(usuario.getId(), habilidade.getId(), NivelHabilidade.EXPERT);

        assertThat(response.nivel()).isEqualTo(NivelHabilidade.EXPERT);
        ArgumentCaptor<UsuarioHabilidade> captor = ArgumentCaptor.forClass(UsuarioHabilidade.class);
        verify(usuarioHabilidadeRepository).save(captor.capture());
        assertThat(captor.getValue().getNivel()).isEqualTo(NivelHabilidade.EXPERT);
    }

    // ============================================
    // remover()
    // ============================================

    @Test
    void deveLancarExcecaoAoRemoverVinculoInexistente() {
        when(usuarioHabilidadeRepository.findByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(usuario.getId(), habilidade.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("não está no seu perfil");

        verify(usuarioHabilidadeRepository, never()).delete(any());
        verify(habilidadeService, never()).recalcularContadores(any());
    }

    @Test
    void deveRemoverVinculoExistenteEDispararRecalculoDeContadores() {
        UsuarioHabilidade vinculo = usuarioHabilidade(usuario, habilidade, NivelHabilidade.INICIANTE);
        when(usuarioHabilidadeRepository.findByUsuario_IdAndHabilidade_Id(usuario.getId(), habilidade.getId()))
                .thenReturn(Optional.of(vinculo));

        service.remover(usuario.getId(), habilidade.getId());

        verify(usuarioHabilidadeRepository).delete(vinculo);
        verify(usuarioHabilidadeRepository).flush();
        verify(habilidadeService).recalcularContadores(habilidade.getId());
    }
}
