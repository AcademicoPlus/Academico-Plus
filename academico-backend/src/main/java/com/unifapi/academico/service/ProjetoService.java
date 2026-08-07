package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Habilidade;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.ProjetoCanceladoAviso;
import com.unifapi.academico.domain.models.ProjetoHabilidade;
import com.unifapi.academico.domain.models.ProjetoMembro;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.habilidade.VincularHabilidadeRequest;
import com.unifapi.academico.dto.projeto.AlterarStatusProjetoRequest;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.CandidaturaRepository;
import com.unifapi.academico.repository.HabilidadeRepository;
import com.unifapi.academico.repository.ProjetoCanceladoAvisoRepository;
import com.unifapi.academico.repository.ProjetoHabilidadeRepository;
import com.unifapi.academico.repository.ProjetoMembroRepository;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    /** Limite da coluna projeto_cancelamento_aviso.projeto_nome. */
    private static final int TAMANHO_MAXIMO_NOME_AVISO = 200;

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoMembroRepository projetoMembroRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final ProjetoHabilidadeRepository projetoHabilidadeRepository;
    private final HabilidadeRepository habilidadeRepository;
    private final ProjetoCanceladoAvisoRepository projetoCanceladoAvisoRepository;
    private final HabilidadeService habilidadeService;

    // ============================================
    // ESCRITA
    // ============================================

    @Transactional
    public ProjetoResponse criar(CriarProjetoRequest request, UUID criadorId) {
        Usuario criador = usuarioRepository.findById(criadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário criador não encontrado."));

        Projeto projeto = projetoRepository.save(Projeto.builder()
                .criador(criador)
                .titulo(request.titulo().trim())
                .descricao(request.descricao().trim())
                .vagas(request.vagas())
                .vagasPreenchidas(0)
                .dataFim(request.dataFim())
                .status(StatusProjeto.ABERTO)
                .ativo(true)
                .build());

        // Só após o save o projeto tem ID, necessário para persistir os vínculos.
        vincularHabilidadesIniciais(projeto, request.habilidades());

        return responseDetalhado(projeto.getId());
    }

    @Transactional
    public ProjetoResponse atualizar(UUID id, AtualizarProjetoRequest request, UUID usuarioLogadoId) {
        Projeto projeto = obterEntidadeComoDono(id, usuarioLogadoId);

        if (projeto.getStatus() == StatusProjeto.CONCLUIDO || projeto.getStatus() == StatusProjeto.CANCELADO) {
            throw new RegraDeNegocioException("Projetos com status " + projeto.getStatus().getLabel()
                    + " não podem ser editados.");
        }

        long membrosAtuais = projetoMembroRepository.countByProjeto_Id(id);
        if (request.vagas() < membrosAtuais) {
            throw new RegraDeNegocioException("O número de vagas não pode ser menor do que as vagas já preenchidas ("
                    + membrosAtuais + ").");
        }

        // Aceita manter a data já cadastrada (mesmo que passada), mas não definir uma nova no passado.
        boolean dataMudou = request.dataFim() != null && !request.dataFim().equals(projeto.getDataFim());
        if (dataMudou && request.dataFim().isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("A nova data de fim deve ser uma data futura.");
        }

        projeto.setTitulo(request.titulo().trim());
        projeto.setDescricao(request.descricao().trim());
        projeto.setVagas(request.vagas());
        projeto.setDataFim(request.dataFim());

        projetoRepository.save(projeto);
        return responseDetalhado(id);
    }

    @Transactional
    public ProjetoResponse alterarStatus(UUID id, AlterarStatusProjetoRequest request, UUID usuarioLogadoId) {
        Projeto projeto = obterEntidadeComoDono(id, usuarioLogadoId);
        StatusProjeto novoStatus = request.status();

        validarTransicaoStatus(projeto.getStatus(), novoStatus);

        projeto.setStatus(novoStatus);
        projetoRepository.save(projeto);

        if (novoStatus == StatusProjeto.CANCELADO) {
            registrarAvisosDeCancelamento(projeto);
        }

        return responseDetalhado(id);
    }

    /** Exclusão lógica: mantém o histórico de candidaturas e membros. */
    @Transactional
    public void inativar(UUID id, UUID usuarioLogadoId) {
        Projeto projeto = obterEntidadeComoDono(id, usuarioLogadoId);

        if (projeto.getStatus() != StatusProjeto.CANCELADO && projeto.getStatus() != StatusProjeto.CONCLUIDO) {
            registrarAvisosDeCancelamento(projeto);
            projeto.setStatus(StatusProjeto.CANCELADO);
        }

        projeto.setAtivo(false);
        projetoRepository.save(projeto);
    }

    // ============================================
    // LEITURA
    // ============================================

    @Transactional(readOnly = true)
    public Page<ProjetoResumoResponse> listar(StatusProjeto status, String busca, Pageable pageable) {
        return projetoRepository.buscarAtivos(status, normalizar(busca), pageable)
                .map(ProjetoResumoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProjetoResumoResponse> listarPorCriador(UUID criadorId, Pageable pageable) {
        return projetoRepository.findByCriador_IdAndAtivoTrue(criadorId, pageable)
                .map(ProjetoResumoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProjetoResumoResponse> listarParticipando(UUID usuarioId, Pageable pageable) {
        return projetoRepository.findByMembros_Usuario_IdAndAtivoTrue(usuarioId, pageable)
                .map(ProjetoResumoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscarPorId(UUID id) {
        return responseDetalhado(id);
    }

    // ============================================
    // USO INTERNO (outros services)
    // ============================================

    /** Carrega o projeto ativo sem coleções. Usado nas validações de escrita. */
    public Projeto obterEntidadeAtiva(UUID id) {
        return projetoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado com o ID informado."));
    }

    /** Carrega o projeto ativo garantindo que o usuário logado é o criador. */
    public Projeto obterEntidadeComoDono(UUID id, UUID usuarioLogadoId) {
        Projeto projeto = obterEntidadeAtiva(id);

        if (projeto.getCriador() == null || !projeto.getCriador().getId().equals(usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você não tem permissão para alterar este projeto.");
        }

        return projeto;
    }

    public boolean ehCriador(Projeto projeto, UUID usuarioId) {
        return projeto.getCriador() != null && projeto.getCriador().getId().equals(usuarioId);
    }

    /**
     * Recalcula vagasPreenchidas a partir da contagem real de membros.
     * Único ponto que escreve nesse campo, para não haver drift entre
     * os fluxos de candidatura aceita, adição direta e remoção de membro.
     */
    @Transactional
    public void sincronizarVagasPreenchidas(Projeto projeto) {
        projeto.setVagasPreenchidas((int) projetoMembroRepository.countByProjeto_Id(projeto.getId()));
        projetoRepository.save(projeto);
    }

    // ============================================
    // PRIVADOS
    // ============================================

    private ProjetoResponse responseDetalhado(UUID projetoId) {
        Projeto projeto = projetoRepository.findDetalhePorId(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado com o ID informado."));

        long totalMembros = projetoMembroRepository.countByProjeto_Id(projetoId);
        long pendentes = candidaturaRepository.countByProjeto_IdAndStatus(projetoId, StatusCandidatura.PENDENTE);

        return ProjetoResponse.fromEntity(projeto, totalMembros, pendentes);
    }

    private void vincularHabilidadesIniciais(Projeto projeto, List<VincularHabilidadeRequest> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return;
        }

        // LinkedHashSet descarta IDs repetidos no mesmo payload sem quebrar a unique constraint.
        Set<UUID> habilidadeIds = new LinkedHashSet<>();

        for (VincularHabilidadeRequest pedido : pedidos) {
            if (!habilidadeIds.add(pedido.habilidadeId())) {
                continue;
            }

            Habilidade habilidade = habilidadeRepository.findById(pedido.habilidadeId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Habilidade não encontrada: " + pedido.habilidadeId()));

            ProjetoHabilidade vinculo = projetoHabilidadeRepository.save(ProjetoHabilidade.builder()
                    .projeto(projeto)
                    .habilidade(habilidade)
                    .obrigatoria(Boolean.TRUE.equals(pedido.obrigatoria()))
                    .build());

            // O projeto já está gerenciado nesta transação: sem isso, o findDetalhePorId
            // logo abaixo reaproveita esta mesma instância (1º nível de cache) com a
            // coleção ainda vazia, em vez de refletir o vínculo recém-persistido.
            projeto.getHabilidadesNecessarias().add(vinculo);
        }

        habilidadeIds.forEach(habilidadeService::recalcularContadores);
    }

    private void validarTransicaoStatus(StatusProjeto atual, StatusProjeto novo) {
        if (atual == novo) {
            throw new RegraDeNegocioException("O projeto já está com o status " + novo.getLabel() + ".");
        }

        if (atual == StatusProjeto.CONCLUIDO || atual == StatusProjeto.CANCELADO) {
            throw new RegraDeNegocioException("Projetos com status " + atual.getLabel()
                    + " não podem ter o status alterado.");
        }
    }

    /**
     * Grava um aviso por membro. O AuthService entrega esses avisos no login,
     * então é assim que a equipe descobre que o projeto foi cancelado.
     */
    private void registrarAvisosDeCancelamento(Projeto projeto) {
        List<ProjetoMembro> membros = projetoMembroRepository.findByProjeto_IdOrderByDataAdesaoAsc(projeto.getId());
        if (membros.isEmpty()) {
            return;
        }

        LocalDate dataReferencia = projeto.getDataFim() != null
                ? projeto.getDataFim().toLocalDate()
                : (projeto.getCriadoEm() != null ? projeto.getCriadoEm().toLocalDate() : LocalDate.now());

        List<ProjetoCanceladoAviso> avisos = membros.stream()
                .map(membro -> ProjetoCanceladoAviso.builder()
                        .usuario(membro.getUsuario())
                        .projeto(projeto)
                        .projetoNome(truncar(projeto.getTitulo(), TAMANHO_MAXIMO_NOME_AVISO))
                        .projetoData(dataReferencia)
                        .tipo("cancelamento")
                        .build())
                .toList();

        projetoCanceladoAvisoRepository.saveAll(avisos);
    }

    private static String truncar(String valor, int tamanhoMaximo) {
        if (valor == null || valor.length() <= tamanhoMaximo) {
            return valor;
        }
        return valor.substring(0, tamanhoMaximo);
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
