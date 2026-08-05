package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Equipe do projeto. Este service é o único dono do vínculo Projeto↔Usuário,
 * portanto toda entrada/saída de membro passa por aqui — inclusive o aceite
 * de candidatura, que delega em adicionarMembro().
 *
 * Convenção: o criador do projeto NÃO ocupa vaga. vagasPreenchidas equivale
 * sempre à quantidade de registros em projeto_membros.
 */
@Service
@RequiredArgsConstructor
public class ProjetoMembroService {

    private final ProjetoMembroRepository projetoMembroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoService projetoService;

    // ============================================
    // LEITURA
    // ============================================

    @Transactional(readOnly = true)
    public List<ProjetoMembroResponse> listarPorProjeto(UUID projetoId) {
        projetoService.obterEntidadeAtiva(projetoId);

        return projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projetoId).stream()
                .map(ProjetoMembroResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetoMembroResponse buscarPorId(UUID projetoId, UUID membroId) {
        return ProjetoMembroResponse.fromEntity(obterMembroDoProjeto(projetoId, membroId));
    }

    @Transactional(readOnly = true)
    public boolean ehMembro(UUID projetoId, UUID usuarioId) {
        return projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projetoId, usuarioId);
    }

    // ============================================
    // ESCRITA
    // ============================================

    @Transactional
    public ProjetoMembroResponse adicionar(UUID projetoId, AdicionarMembroRequest request, UUID usuarioLogadoId) {
        Projeto projeto = projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID informado."));

        ProjetoMembro membro = adicionarMembro(projeto, usuario, request.funcao());
        return ProjetoMembroResponse.fromEntity(membro);
    }

    /**
     * Núcleo compartilhado com CandidaturaService.aceitar().
     * Valida vagas e duplicidade e mantém vagasPreenchidas sincronizado.
     */
    @Transactional
    public ProjetoMembro adicionarMembro(Projeto projeto, Usuario usuario, String funcao) {
        if (usuario == null) {
            throw new RegraDeNegocioException("Usuário inválido para inclusão no projeto.");
        }

        if (projeto.getStatus() == StatusProjeto.CONCLUIDO || projeto.getStatus() == StatusProjeto.CANCELADO) {
            throw new RegraDeNegocioException("Não é possível incluir membros em um projeto "
                    + projeto.getStatus().getLabel().toLowerCase() + ".");
        }

        if (projetoService.ehCriador(projeto, usuario.getId())) {
            throw new RegraDeNegocioException(
                    "O criador já é responsável pelo projeto e não ocupa vaga como membro.");
        }

        if (projetoMembroRepository.existsByProjeto_IdAndUsuario_Id(projeto.getId(), usuario.getId())) {
            throw new RegraDeNegocioException("Este usuário já é membro do projeto.");
        }

        long ocupadas = projetoMembroRepository.countByProjeto_Id(projeto.getId());
        if (projeto.getVagas() != null && ocupadas >= projeto.getVagas()) {
            throw new RegraDeNegocioException("O projeto já preencheu todas as "
                    + projeto.getVagas() + " vaga(s) disponíveis.");
        }

        ProjetoMembro membro = projetoMembroRepository.save(ProjetoMembro.builder()
                .projeto(projeto)
                .usuario(usuario)
                .funcao(normalizar(funcao))
                .dataAdesao(LocalDateTime.now())
                .build());

        projetoMembroRepository.flush();
        projetoService.sincronizarVagasPreenchidas(projeto);

        return membro;
    }

    @Transactional
    public ProjetoMembroResponse atualizarFuncao(UUID projetoId,
                                                 UUID membroId,
                                                 AtualizarFuncaoMembroRequest request,
                                                 UUID usuarioLogadoId) {
        projetoService.obterEntidadeComoDono(projetoId, usuarioLogadoId);

        ProjetoMembro membro = obterMembroDoProjeto(projetoId, membroId);
        membro.setFuncao(request.funcao().trim());

        return ProjetoMembroResponse.fromEntity(projetoMembroRepository.save(membro));
    }

    /**
     * Remove o membro e libera a vaga. Permitido ao criador do projeto
     * (remover alguém) e ao próprio membro (sair do projeto).
     */
    @Transactional
    public void remover(UUID projetoId, UUID membroId, UUID usuarioLogadoId) {
        Projeto projeto = projetoService.obterEntidadeAtiva(projetoId);
        ProjetoMembro membro = obterMembroDoProjeto(projetoId, membroId);

        boolean ehDono = projetoService.ehCriador(projeto, usuarioLogadoId);
        boolean ehProprioMembro = membro.getUsuario() != null
                && membro.getUsuario().getId().equals(usuarioLogadoId);

        if (!ehDono && !ehProprioMembro) {
            throw new RegraDeNegocioException("Você não tem permissão para remover este membro do projeto.");
        }

        projetoMembroRepository.delete(membro);
        // Garante que o count abaixo já enxergue a remoção.
        projetoMembroRepository.flush();

        projetoService.sincronizarVagasPreenchidas(projeto);
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private ProjetoMembro obterMembroDoProjeto(UUID projetoId, UUID membroId) {
        ProjetoMembro membro = projetoMembroRepository.findById(membroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado com o ID informado."));

        if (membro.getProjeto() == null || !membro.getProjeto().getId().equals(projetoId)) {
            throw new RecursoNaoEncontradoException("Este membro não pertence ao projeto informado.");
        }

        return membro;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
