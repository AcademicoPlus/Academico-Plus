package com.unifapi.academico.service;

import com.unifapi.academico.domain.models.Curso;
import com.unifapi.academico.domain.models.NivelAcesso;
import com.unifapi.academico.domain.models.TokenRevogado;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.auth.MensagemResponse;
import com.unifapi.academico.dto.auth.UsuarioResumoResponse;
import com.unifapi.academico.dto.usuario.ExcluirContaRequest;
import com.unifapi.academico.dto.usuario.UsuarioPerfilResponse;
import com.unifapi.academico.dto.usuario.UsuarioUpdatePerfilRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaRequest;
import com.unifapi.academico.dto.usuario.UsuarioUpdateSenhaResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CursoRepository;
import com.unifapi.academico.repository.NivelAcessoRepository;
import com.unifapi.academico.repository.TokenRevogadoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.security.JwtTokenProvider;
import com.unifapi.academico.support.EntityFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private NivelAcessoRepository nivelAcessoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenRevogadoRepository tokenRevogadoRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private FotoStorageClient fotoStorageClient;

    @InjectMocks
    private UsuarioService usuarioService;

    private NivelAcesso nivelAluno;
    private Curso curso;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        nivelAluno = EntityFixtures.nivelAcesso("ALUNO");
        curso = EntityFixtures.curso("Ciência da Computação");
        usuario = EntityFixtures.usuario("aluno@teste.com", "Aluno Teste", nivelAluno, curso);
    }

    // ============================================
    // alterarNivelAcesso
    // ============================================

    @Test
    void alterarNivelAcesso_deveLancarExcecao_quandoUsuarioNaoEncontrado() {
        UUID idUsuario = UUID.randomUUID();
        UUID idNovoNivel = UUID.randomUUID();
        when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarNivelAcesso(idUsuario, idNovoNivel))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(nivelAcessoRepository, never()).findById(any());
    }

    @Test
    void alterarNivelAcesso_deveLancarExcecao_quandoNivelAcessoNaoEncontrado() {
        UUID idNovoNivel = UUID.randomUUID();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(nivelAcessoRepository.findById(idNovoNivel)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarNivelAcesso(usuario.getId(), idNovoNivel))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Nível de acesso não encontrado");
    }

    @Test
    void alterarNivelAcesso_deveAtualizarNivelDoUsuario_quandoAmbosExistem() {
        NivelAcesso nivelAdmin = EntityFixtures.nivelAcesso("ADMIN");
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(nivelAcessoRepository.findById(nivelAdmin.getId())).thenReturn(Optional.of(nivelAdmin));

        UsuarioPerfilResponse response = usuarioService.alterarNivelAcesso(usuario.getId(), nivelAdmin.getId());

        assertThat(usuario.getNivelAcesso()).isEqualTo(nivelAdmin);
        assertThat(response.nivelAcesso().nome()).isEqualTo("ADMIN");
        // A entidade é gerenciada pelo JPA: a alteração de estado é suficiente
        // (dirty checking), não há chamada explícita a usuarioRepository.save(...).
        verify(usuarioRepository, never()).save(any());
    }

    // ============================================
    // obterPorId
    // ============================================

    @Test
    void obterPorId_deveLancarExcecao_quandoUsuarioNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obterPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void obterPorId_deveRetornarPerfil_quandoUsuarioExiste() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        UsuarioPerfilResponse response = usuarioService.obterPorId(usuario.getId());

        assertThat(response.id()).isEqualTo(usuario.getId());
        assertThat(response.email()).isEqualTo(usuario.getEmail());
    }

    // ============================================
    // listarTodos (@PreAuthorize não é testável via Mockito puro;
    // aqui testamos apenas a lógica do método em si)
    // ============================================

    @Test
    void listarTodos_deveRetornarListaDeResumos() {
        Usuario outroUsuario = EntityFixtures.usuario("outro@teste.com", nivelAluno);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario, outroUsuario));

        List<UsuarioResumoResponse> resultado = usuarioService.listarTodos();

        assertThat(resultado).hasSize(2)
                .extracting(UsuarioResumoResponse::nome)
                .containsExactlyInAnyOrder(usuario.getNome(), outroUsuario.getNome());
    }

    @Test
    void listarTodos_deveRetornarListaVazia_quandoNaoHaUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        List<UsuarioResumoResponse> resultado = usuarioService.listarTodos();

        assertThat(resultado).isEmpty();
    }

    // ============================================
    // buscarPerfisPublicos
    // ============================================

    @Test
    @SuppressWarnings("unchecked")
    void buscarPerfisPublicos_deveDelegarParaRepositorioEMapearParaDto() {
        String busca = "joao";
        UUID idCurso = UUID.randomUUID();
        Pageable pageable = mock(Pageable.class);
        Page<Usuario> paginaDeEntidades = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(paginaDeEntidades);

        Page<UsuarioResumoResponse> resultado = usuarioService.buscarPerfisPublicos(busca, idCurso, null, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).nome()).isEqualTo(usuario.getNome());
        verify(usuarioRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPerfisPublicos_deveFiltrarPorHabilidadeQuandoInformada() {
        UUID idHabilidade = UUID.randomUUID();
        Pageable pageable = mock(Pageable.class);
        Page<Usuario> paginaDeEntidades = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(paginaDeEntidades);

        Page<UsuarioResumoResponse> resultado = usuarioService.buscarPerfisPublicos(null, null, idHabilidade, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        verify(usuarioRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ============================================
    // logout
    // ============================================

    @Test
    void logout_deveRetornarSucesso_eNaoSalvarToken_quandoHeaderNulo() {
        MensagemResponse response = usuarioService.logout(null);

        assertThat(response.sucesso()).isTrue();
        verify(tokenRevogadoRepository, never()).save(any());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void logout_deveRetornarSucesso_eNaoSalvarToken_quandoHeaderNaoComecaComBearer() {
        MensagemResponse response = usuarioService.logout("Basic abc123");

        assertThat(response.sucesso()).isTrue();
        verify(tokenRevogadoRepository, never()).save(any());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void logout_naoDeveSalvarNovamente_quandoTokenJaEstaRevogado() {
        String token = "token-ja-revogado";
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(true);

        MensagemResponse response = usuarioService.logout("Bearer " + token);

        assertThat(response.sucesso()).isTrue();
        verify(tokenRevogadoRepository, never()).save(any());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void logout_deveSalvarNovoTokenRevogado_quandoAindaNaoRevogado() {
        String token = "token-valido-ainda-nao-revogado";
        LocalDateTime expiracao = LocalDateTime.now().plusHours(1);
        when(tokenRevogadoRepository.existsByToken(token)).thenReturn(false);
        when(jwtTokenProvider.obterDataExpiracaoDoToken(token)).thenReturn(expiracao);

        MensagemResponse response = usuarioService.logout("Bearer " + token);

        assertThat(response.sucesso()).isTrue();

        ArgumentCaptor<TokenRevogado> captor = ArgumentCaptor.forClass(TokenRevogado.class);
        verify(tokenRevogadoRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(token);
        assertThat(captor.getValue().getDataExpiracao()).isEqualTo(expiracao);
    }

    // ============================================
    // atualizarPerfil
    // ============================================

    @Test
    void atualizarPerfil_deveLancarExcecao_quandoIdCursoInvalido() {
        UUID idCursoInvalido = UUID.randomUUID();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(cursoRepository.findById(idCursoInvalido)).thenReturn(Optional.empty());

        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                null, idCursoInvalido, null, null, null, null, null);

        assertThatThrownBy(() -> usuarioService.atualizarPerfil(usuario.getId(), request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Curso não encontrado");
    }

    @Test
    void atualizarPerfil_naoDeveAlterarNome_quandoNomeInformadoEmBranco() {
        String nomeOriginal = usuario.getNome();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                "   ", null, null, null, null, null, null);

        usuarioService.atualizarPerfil(usuario.getId(), request);

        assertThat(usuario.getNome()).isEqualTo(nomeOriginal);
    }

    @Test
    void atualizarPerfil_deveAplicarNomeTrimado_quandoNomeValido() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                "  Novo Nome  ", null, null, null, null, null, null);

        usuarioService.atualizarPerfil(usuario.getId(), request);

        assertThat(usuario.getNome()).isEqualTo("Novo Nome");
    }

    @Test
    void atualizarPerfil_deveAtualizarCurso_quandoIdCursoValido() {
        Curso novoCurso = EntityFixtures.curso("Engenharia de Software");
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(cursoRepository.findById(novoCurso.getId())).thenReturn(Optional.of(novoCurso));

        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                null, novoCurso.getId(), null, null, null, null, null);

        usuarioService.atualizarPerfil(usuario.getId(), request);

        assertThat(usuario.getCurso()).isEqualTo(novoCurso);
    }

    @Test
    void atualizarPerfil_deveSobrescreverBioComStringVazia_masNaoQuandoBioForNula() {
        usuario.setBio("Bio antiga");
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        // bio="" é aplicada (apenas null é ignorado)
        UsuarioUpdatePerfilRequest requestComBioVazia = new UsuarioUpdatePerfilRequest(
                null, null, null, "", null, null, null);
        usuarioService.atualizarPerfil(usuario.getId(), requestComBioVazia);
        assertThat(usuario.getBio()).isEqualTo("");

        // bio=null não é aplicada, mantém o valor anterior
        usuario.setBio("Bio atual");
        UsuarioUpdatePerfilRequest requestComBioNula = new UsuarioUpdatePerfilRequest(
                null, null, null, null, null, null, null);
        usuarioService.atualizarPerfil(usuario.getId(), requestComBioNula);
        assertThat(usuario.getBio()).isEqualTo("Bio atual");
    }

    @Test
    void atualizarPerfil_deveAplicarTodosOsCamposOpcionais_quandoInformados() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        UsuarioUpdatePerfilRequest request = new UsuarioUpdatePerfilRequest(
                null, null, 5, "Nova bio", "http://foto.com/nova.png",
                "http://linkedin.com/in/teste", "http://github.com/teste");

        usuarioService.atualizarPerfil(usuario.getId(), request);

        assertThat(usuario.getPeriodo()).isEqualTo(5);
        assertThat(usuario.getBio()).isEqualTo("Nova bio");
        assertThat(usuario.getFotoUrl()).isEqualTo("http://foto.com/nova.png");
        assertThat(usuario.getLinkedinUrl()).isEqualTo("http://linkedin.com/in/teste");
        assertThat(usuario.getGithubUrl()).isEqualTo("http://github.com/teste");
    }

    // ============================================
    // atualizarSenha
    // ============================================

    @Test
    void atualizarSenha_deveLancarExcecao_quandoSenhaAtualNaoConfere() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenhaHash())).thenReturn(false);

        UsuarioUpdateSenhaRequest request = new UsuarioUpdateSenhaRequest("senhaErrada", "novaSenha123");

        assertThatThrownBy(() -> usuarioService.atualizarSenha(usuario.getId(), request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Senha atual incorreta.");

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void atualizarSenha_deveLancarExcecao_quandoNovaSenhaIgualASenhaAtual() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual123", usuario.getSenhaHash())).thenReturn(true);

        UsuarioUpdateSenhaRequest request = new UsuarioUpdateSenhaRequest("senhaAtual123", "senhaAtual123");

        assertThatThrownBy(() -> usuarioService.atualizarSenha(usuario.getId(), request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("deve ser diferente");

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void atualizarSenha_deveCodificarEAtualizarHash_quandoDadosValidos() {
        String hashAntigo = usuario.getSenhaHash();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaAtual123", hashAntigo)).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", hashAntigo)).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("$2a$10$novoHash");

        UsuarioUpdateSenhaRequest request = new UsuarioUpdateSenhaRequest("senhaAtual123", "novaSenha123");

        UsuarioUpdateSenhaResponse response = usuarioService.atualizarSenha(usuario.getId(), request);

        assertThat(response.mensagem()).isEqualTo("Senha alterada com sucesso.");
        assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$novoHash");
        verify(passwordEncoder).encode("novaSenha123");
    }

    // ============================================
    // atualizarFoto
    // ============================================

    @Test
    void atualizarFoto_deveSalvarNovaFoto_eNaoTentarDeletarAntiga_quandoNaoHaviaFotoAnterior() {
        usuario.setFotoUrl(null);
        MultipartFile arquivo = mock(MultipartFile.class);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(fotoStorageClient.salvar(arquivo)).thenReturn("http://storage/nova-foto.png");

        UsuarioPerfilResponse response = usuarioService.atualizarFoto(usuario.getId(), arquivo);

        assertThat(usuario.getFotoUrl()).isEqualTo("http://storage/nova-foto.png");
        assertThat(response.fotoUrl()).isEqualTo("http://storage/nova-foto.png");
        verify(fotoStorageClient).salvar(arquivo);
        verify(fotoStorageClient, never()).deletar(anyString());
    }

    @Test
    void atualizarFoto_deveSalvarNovaFoto_eDeletarFotoAntiga_quandoJaHaviaFotoAnterior() {
        usuario.setFotoUrl("http://storage/foto-antiga.png");
        MultipartFile arquivo = mock(MultipartFile.class);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(fotoStorageClient.salvar(arquivo)).thenReturn("http://storage/nova-foto.png");

        usuarioService.atualizarFoto(usuario.getId(), arquivo);

        assertThat(usuario.getFotoUrl()).isEqualTo("http://storage/nova-foto.png");
        verify(fotoStorageClient).salvar(arquivo);
        verify(fotoStorageClient).deletar("http://storage/foto-antiga.png");
    }

    @Test
    void atualizarFoto_naoDeveDeletarFotoAntiga_quandoSalvarNovaFotoFalhar() {
        usuario.setFotoUrl("http://storage/foto-antiga.png");
        MultipartFile arquivo = mock(MultipartFile.class);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(fotoStorageClient.salvar(arquivo)).thenThrow(new RegraDeNegocioException("Falha ao salvar imagem."));

        assertThatThrownBy(() -> usuarioService.atualizarFoto(usuario.getId(), arquivo))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(usuario.getFotoUrl()).isEqualTo("http://storage/foto-antiga.png");
        verify(fotoStorageClient, never()).deletar(anyString());
    }

    // ============================================
    // deletarUsuario
    // ============================================

    @Test
    void deletarUsuario_deveLancarExcecao_quandoUsuarioTentaInativarAPropriaConta() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> usuarioService.deletarUsuario(usuario.getId(), usuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pode inativar sua própria conta");

        assertThat(usuario.getAtivo()).isTrue();
    }

    @Test
    void deletarUsuario_deveLancarExcecao_quandoAlvoForAdministrador() {
        NivelAcesso nivelAdmin = EntityFixtures.nivelAcesso("ADMIN");
        Usuario admin = EntityFixtures.usuario("admin@teste.com", "Admin", nivelAdmin, null);
        UUID idUsuarioLogado = UUID.randomUUID();
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.deletarUsuario(admin.getId(), idUsuarioLogado))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Não é permitido inativar administradores");

        assertThat(admin.getAtivo()).isTrue();
    }

    @Test
    void deletarUsuario_deveInativarUsuario_masNaoPreencherDeletadoEm() {
        UUID idUsuarioLogado = UUID.randomUUID();
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        usuarioService.deletarUsuario(usuario.getId(), idUsuarioLogado);

        assertThat(usuario.getAtivo()).isFalse();
        assertThat(usuario.getDeletadoEm()).isNull();
    }

    // ============================================
    // excluirMinhaConta
    // ============================================

    @Test
    void excluirMinhaConta_deveLancarExcecao_quandoSenhaIncorreta() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", usuario.getSenhaHash())).thenReturn(false);

        ExcluirContaRequest request = new ExcluirContaRequest("senhaErrada");

        assertThatThrownBy(() -> usuarioService.excluirMinhaConta(usuario.getId(), request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Senha incorreta.");

        assertThat(usuario.getAtivo()).isTrue();
        assertThat(usuario.getDeletadoEm()).isNull();
    }

    @Test
    void excluirMinhaConta_deveInativarEMarcarDataDeExclusao_quandoSenhaCorreta() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", usuario.getSenhaHash())).thenReturn(true);

        ExcluirContaRequest request = new ExcluirContaRequest("senhaCorreta");

        usuarioService.excluirMinhaConta(usuario.getId(), request);

        assertThat(usuario.getAtivo()).isFalse();
        assertThat(usuario.getDeletadoEm()).isNotNull();
    }
}
