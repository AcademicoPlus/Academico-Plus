package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoMembro;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.membro.AdicionarMembroRequest;
import com.unifapi.academico.dto.membro.AtualizarFuncaoMembroRequest;
import com.unifapi.academico.dto.membro.ProjetoMembroResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.unifapi.academico.support.EntityFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (Mockito puro, sem contexto Spring) para {@link ProjetoMembroService}.
 *
 * Cobrem as duas portas de entrada de membros (adicionar via dono + adicionarMembro,
 * núcleo compartilhado com CandidaturaService.aceitar), a atualização de função e a
 * remoção — com atenção especial às assimetrias de regra de negócio entre os métodos:
 * apenas adicionarMembro() bloqueia por status do projeto; atualizarFuncao() e remover()
 * não têm essa restrição.
 */
@ExtendWith(MockitoExtension.class)
class ProjetoMembroServiceTest {

    @Mock
    private ProjetoMembroRepository projetoMembroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoService projetoService;

    @InjectMocks
    private ProjetoMembroService service;

    private NivelAcesso nivelAcesso;
    private Usuario criador;
    private Usuario membro;
    private Usuario outroUsuario;
    private Projeto projeto;
    private UUID projetoId;

    @BeforeEach
    void setUp() {
        nivelAcesso = nivelAcesso("ALUNO");
        criador = usuario("criador@teste.com", nivelAcesso);
        membro = usuario("membro@teste.com", nivelAcesso);
        outroUsuario = usuario("outro@teste.com", nivelAcesso);
        projeto = projeto(criador, 5);
        projetoId = projeto.getId();
    }

    // ============================================
    // adicionar() — porta de entrada exclusiva do dono
    // ============================================

    @Test
    void adicionar_devePermitirQuandoUsuarioLogadoEhCriador_eDelegarParaAdicionarMembro() {
        AdicionarMembroRequest request = new AdicionarMembroRequest(membro.getId(), "Desenvolvedor");
        ProjetoMembro salvo = projetoMembro(projeto, membro, "Desenvolvedor");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(usuarioRepository.findById(membro.getId())).thenReturn(Optional.of(membro));
        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projetoId)).thenReturn(0L);
        when(projetoMembroRepository.save(any(ProjetoMembro.class))).thenReturn(salvo);

        ProjetoMembroResponse response = service.adicionar(projetoId, request, criador.getId());

        assertThat(response).isNotNull();
        assertThat(response.funcao()).isEqualTo("Desenvolvedor");
        verify(projetoMembroRepository).save(any(ProjetoMembro.class));
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    @Test
    void adicionar_deveLancarRegraDeNegocio_quandoUsuarioLogadoNaoEhCriador() {
        AdicionarMembroRequest request = new AdicionarMembroRequest(membro.getId(), null);

        when(projetoService.obterEntidadeComoDono(projetoId, outroUsuario.getId()))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> service.adicionar(projetoId, request, outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você não tem permissão para alterar este projeto.");

        verify(usuarioRepository, never()).findById(any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void adicionar_deveLancarRecursoNaoEncontrado_quandoUsuarioAlvoNaoExiste() {
        UUID idInexistente = UUID.randomUUID();
        AdicionarMembroRequest request = new AdicionarMembroRequest(idInexistente, "Dev");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adicionar(projetoId, request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário não encontrado com o ID informado.");

        verify(projetoMembroRepository, never()).save(any());
        verify(projetoService, never()).sincronizarVagasPreenchidas(any());
    }

    // ============================================
    // adicionarMembro() — núcleo compartilhado com CandidaturaService.aceitar()
    // ============================================

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoUsuarioNulo() {
        assertThatThrownBy(() -> service.adicionarMembro(projeto, null, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Usuário inválido para inclusão no projeto.");

        verifyNoInteractions(projetoMembroRepository);
        verifyNoInteractions(projetoService);
    }

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoProjetoConcluido() {
        projeto.setStatus(StatusProjeto.CONCLUIDO);

        assertThatThrownBy(() -> service.adicionarMembro(projeto, membro, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Não é possível incluir membros em um projeto concluído.");

        verify(projetoService, never()).ehCriador(any(), any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoProjetoCancelado() {
        projeto.setStatus(StatusProjeto.CANCELADO);

        assertThatThrownBy(() -> service.adicionarMembro(projeto, membro, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Não é possível incluir membros em um projeto cancelado.");

        verify(projetoService, never()).ehCriador(any(), any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void adicionarMembro_devePermitirQuandoProjetoEmAndamento() {
        projeto.setStatus(StatusProjeto.EM_ANDAMENTO);
        ProjetoMembro salvo = projetoMembro(projeto, membro, "Dev");

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        when(projetoMembroRepository.save(any(ProjetoMembro.class))).thenReturn(salvo);

        ProjetoMembro resultado = service.adicionarMembro(projeto, membro, "Dev");

        assertThat(resultado).isNotNull();
        verify(projetoMembroRepository).flush();
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoUsuarioEhCriadorDoProprioProjeto() {
        when(projetoService.ehCriador(projeto, criador.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.adicionarMembro(projeto, criador, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O criador já é responsável pelo projeto e não ocupa vaga como membro.");

        verify(projetoMembroRepository, never()).existsByProjeto_IdAndUsuario_Id(any(), any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoUsuarioJaEhMembro() {
        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.adicionarMembro(projeto, membro, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Este usuário já é membro do projeto.");

        verify(projetoMembroRepository, never()).countByProjeto_Id(any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void adicionarMembro_deveLancarRegraDeNegocio_quandoVagasEsgotadas() {
        projeto.setVagas(2);

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(2L);

        assertThatThrownBy(() -> service.adicionarMembro(projeto, membro, "Dev"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O projeto já preencheu todas as 2 vaga(s) disponíveis.");

        verify(projetoMembroRepository, never()).save(any());
        verify(projetoService, never()).sincronizarVagasPreenchidas(any());
    }

    @Test
    void adicionarMembro_deveIgnorarLimiteDeVagas_quandoVagasNulas() {
        projeto.setVagas(null);
        ProjetoMembro salvo = projetoMembro(projeto, membro, "Dev");

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        // Mesmo com uma contagem "alta" de ocupadas, sem vagas definidas não há limite a verificar.
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(999L);
        when(projetoMembroRepository.save(any(ProjetoMembro.class))).thenReturn(salvo);

        ProjetoMembro resultado = service.adicionarMembro(projeto, membro, "Dev");

        assertThat(resultado).isNotNull();
        verify(projetoMembroRepository).save(any(ProjetoMembro.class));
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    @Test
    void adicionarMembro_deveApararEspacosDaFuncao_aoSalvarMembro() {
        ArgumentCaptor<ProjetoMembro> captor = ArgumentCaptor.forClass(ProjetoMembro.class);

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        when(projetoMembroRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.adicionarMembro(projeto, membro, "  Dev Backend  ");

        ProjetoMembro salvo = captor.getValue();
        assertThat(salvo.getFuncao()).isEqualTo("Dev Backend");
        assertThat(salvo.getUsuario()).isEqualTo(membro);
        assertThat(salvo.getProjeto()).isEqualTo(projeto);
        assertThat(salvo.getDataAdesao()).isNotNull();
    }

    @Test
    void adicionarMembro_deveConverterFuncaoEmBranco_paraNulo() {
        ArgumentCaptor<ProjetoMembro> captor = ArgumentCaptor.forClass(ProjetoMembro.class);

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        when(projetoMembroRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.adicionarMembro(projeto, membro, "   ");

        assertThat(captor.getValue().getFuncao()).isNull();
    }

    @Test
    void adicionarMembro_deveAceitarFuncaoNula_semErro() {
        ArgumentCaptor<ProjetoMembro> captor = ArgumentCaptor.forClass(ProjetoMembro.class);

        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), membro.getId())).thenReturn(false);
        when(projetoMembroRepository.countByProjeto_Id(projeto.getId())).thenReturn(0L);
        when(projetoMembroRepository.save(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.adicionarMembro(projeto, membro, null);

        assertThat(captor.getValue().getFuncao()).isNull();
    }

    // ============================================
    // atualizarFuncao() — dono apenas, sem restrição de status
    // ============================================

    @Test
    void atualizarFuncao_devePermitirQuandoUsuarioLogadoEhCriador() {
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Antigo");
        UUID membroId = membroEntity.getId();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("  Novo Cargo  ");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoMembroRepository.save(membroEntity)).thenReturn(membroEntity);

        ProjetoMembroResponse response = service.atualizarFuncao(projetoId, membroId, request, criador.getId());

        assertThat(membroEntity.getFuncao()).isEqualTo("Novo Cargo");
        assertThat(response.funcao()).isEqualTo("Novo Cargo");
        verify(projetoMembroRepository).save(membroEntity);
    }

    @Test
    void atualizarFuncao_deveLancarRegraDeNegocio_quandoUsuarioLogadoNaoEhCriador() {
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Novo Cargo");
        UUID membroId = UUID.randomUUID();

        when(projetoService.obterEntidadeComoDono(projetoId, outroUsuario.getId()))
                .thenThrow(new RegraDeNegocioException("Você não tem permissão para alterar este projeto."));

        assertThatThrownBy(() -> service.atualizarFuncao(projetoId, membroId, request, outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você não tem permissão para alterar este projeto.");

        verify(projetoMembroRepository, never()).findById(any());
        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void atualizarFuncao_deveLancarRecursoNaoEncontrado_quandoMembroNaoExiste() {
        UUID membroId = UUID.randomUUID();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Novo Cargo");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizarFuncao(projetoId, membroId, request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Membro não encontrado com o ID informado.");

        verify(projetoMembroRepository, never()).save(any());
    }

    @Test
    void atualizarFuncao_deveLancarRecursoNaoEncontrado_quandoMembroPertenceAOutroProjeto() {
        Projeto outroProjeto = projeto(criador, 3);
        ProjetoMembro membroDeOutroProjeto = projetoMembro(outroProjeto, membro, "Dev");
        UUID membroId = membroDeOutroProjeto.getId();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Novo Cargo");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroDeOutroProjeto));

        assertThatThrownBy(() -> service.atualizarFuncao(projetoId, membroId, request, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Este membro não pertence ao projeto informado.");

        verify(projetoMembroRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = {"CONCLUIDO", "CANCELADO"})
    void atualizarFuncao_devePermitirAtualizarFuncao_independenteDoStatusDoProjeto(StatusProjeto status) {
        projeto.setStatus(status);
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Antigo");
        UUID membroId = membroEntity.getId();
        AtualizarFuncaoMembroRequest request = new AtualizarFuncaoMembroRequest("Novo Cargo");

        when(projetoService.obterEntidadeComoDono(projetoId, criador.getId())).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoMembroRepository.save(membroEntity)).thenReturn(membroEntity);

        ProjetoMembroResponse response = service.atualizarFuncao(projetoId, membroId, request, criador.getId());

        assertThat(response.funcao()).isEqualTo("Novo Cargo");
        verify(projetoMembroRepository).save(membroEntity);
    }

    // ============================================
    // remover() — dono OU o próprio membro; sem restrição de status
    // ============================================

    @Test
    void remover_devePermitirQuandoCriadorRemoveOutroMembro_eSincronizarVagas() {
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Dev");
        UUID membroId = membroEntity.getId();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoService.ehCriador(projeto, criador.getId())).thenReturn(true);

        service.remover(projetoId, membroId, criador.getId());

        verify(projetoMembroRepository).delete(membroEntity);
        verify(projetoMembroRepository).flush();
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    @Test
    void remover_devePermitirQuandoProprioMembroSaiDoProjeto() {
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Dev");
        UUID membroId = membroEntity.getId();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoService.ehCriador(projeto, membro.getId())).thenReturn(false);

        service.remover(projetoId, membroId, membro.getId());

        verify(projetoMembroRepository).delete(membroEntity);
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    @Test
    void remover_deveLancarRegraDeNegocio_quandoUsuarioLogadoNaoEhDonoNemOProprioMembro() {
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Dev");
        UUID membroId = membroEntity.getId();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoService.ehCriador(projeto, outroUsuario.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.remover(projetoId, membroId, outroUsuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Você não tem permissão para remover este membro do projeto.");

        verify(projetoMembroRepository, never()).delete(any());
        verify(projetoMembroRepository, never()).flush();
        verify(projetoService, never()).sincronizarVagasPreenchidas(any());
    }

    @Test
    void remover_deveLancarRecursoNaoEncontrado_quandoMembroNaoExiste() {
        UUID membroId = UUID.randomUUID();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(projetoId, membroId, criador.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Membro não encontrado com o ID informado.");

        verify(projetoMembroRepository, never()).delete(any());
        verify(projetoService, never()).sincronizarVagasPreenchidas(any());
    }

    @Test
    void remover_deveLancarRecursoNaoEncontrado_quandoMembroPertenceAOutroProjeto_semExporPermissao() {
        Projeto outroProjeto = projeto(criador, 3);
        ProjetoMembro membroDeOutroProjeto = projetoMembro(outroProjeto, membro, "Dev");
        UUID membroId = membroDeOutroProjeto.getId();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroDeOutroProjeto));

        // IDOR-safe: mesmo um usuário sem qualquer relação com o projeto recebe 404
        // (não 403) ao tentar adivinhar um membroId de outro projeto.
        assertThatThrownBy(() -> service.remover(projetoId, membroId, outroUsuario.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Este membro não pertence ao projeto informado.");

        verify(projetoService, never()).ehCriador(any(), any());
        verify(projetoMembroRepository, never()).delete(any());
        verify(projetoService, never()).sincronizarVagasPreenchidas(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusProjeto.class, names = {"CONCLUIDO", "CANCELADO"})
    void remover_devePermitirRemocao_independenteDoStatusDoProjeto(StatusProjeto status) {
        projeto.setStatus(status);
        ProjetoMembro membroEntity = projetoMembro(projeto, membro, "Dev");
        UUID membroId = membroEntity.getId();

        when(projetoService.obterEntidadeAtiva(projetoId)).thenReturn(projeto);
        when(projetoMembroRepository.findById(membroId)).thenReturn(Optional.of(membroEntity));
        when(projetoService.ehCriador(projeto, criador.getId())).thenReturn(true);

        service.remover(projetoId, membroId, criador.getId());

        verify(projetoMembroRepository).delete(membroEntity);
        verify(projetoService).sincronizarVagasPreenchidas(projeto);
    }

    // ============================================
    // ehMembro()
    // ============================================

    @Test
    void ehMembro_deveRetornarTrue_quandoExisteRegistroDeVinculo() {
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, membro.getId())).thenReturn(true);

        boolean resultado = service.ehMembro(projetoId, membro.getId());

        assertThat(resultado).isTrue();
        verify(projetoMembroRepository, times(1))
                .existsByProjeto_IdAndUsuario_Id(projetoId, membro.getId());
    }

    @Test
    void ehMembro_deveRetornarFalse_quandoNaoExisteRegistroDeVinculo() {
        when(projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, outroUsuario.getId())).thenReturn(false);

        boolean resultado = service.ehMembro(projetoId, outroUsuario.getId());

        assertThat(resultado).isFalse();
    }
}
