package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.models.Projeto;
import com.unifapi.academico.domain.models.Usuario;
import com.unifapi.academico.dto.projeto.AtualizarProjetoRequest;
import com.unifapi.academico.dto.projeto.CriarProjetoRequest;
import com.unifapi.academico.dto.projeto.ProjetoResponse;
import com.unifapi.academico.exception.RecursoNaoEncontradoException;
import com.unifapi.academico.exception.RegraDeNegocioException;
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ProjetoResponse criar(CriarProjetoRequest request, UUID criadorId) {
        Usuario criador = usuarioRepository.findById(criadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário criador não encontrado."));

        Projeto projeto = Projeto.builder()
                .criador(criador)
                .titulo(request.titulo().trim())
                .descricao(request.descricao().trim())
                .vagas(request.vagas())
                .dataFim(request.dataFim())
                .status(StatusProjeto.ABERTO)
                .ativo(true)
                .build();

        projeto = projetoRepository.save(projeto);
        return ProjetoResponse.fromEntity(projeto);
    }

    @Transactional(readOnly = true)
    public Page<ProjetoResponse> listar(StatusProjeto status, Pageable pageable) {
        Page<Projeto> projetos = (status != null)
                ? projetoRepository.findByAtivoTrueAndStatus(status, pageable)
                : projetoRepository.findByAtivoTrue(pageable);

        return projetos.map(ProjetoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProjetoResponse> listarPorCriador(UUID criadorId, Pageable pageable) {
        return projetoRepository.findByCriador_IdAndAtivoTrue(criadorId, pageable)
                .map(ProjetoResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscarPorId(UUID id) {
        Projeto projeto = projetoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado com o ID informado."));
        return ProjetoResponse.fromEntity(projeto);
    }

    @Transactional
    public ProjetoResponse atualizar(UUID id, AtualizarProjetoRequest request, UUID usuarioLogadoId) {
        Projeto projeto = obterProjetoEValidarDono(id, usuarioLogadoId);

        if (request.vagas() < projeto.getVagasPreenchidas()) {
            throw new RegraDeNegocioException("O número de vagas não pode ser menor do que as vagas já preenchidas (" 
                    + projeto.getVagasPreenchidas() + ").");
        }

        projeto.setTitulo(request.titulo().trim());
        projeto.setDescricao(request.descricao().trim());
        projeto.setVagas(request.vagas());
        projeto.setDataFim(request.dataFim());

        projeto = projetoRepository.save(projeto);
        return ProjetoResponse.fromEntity(projeto);
    }

    @Transactional
    public ProjetoResponse alterarStatus(UUID id, StatusProjeto novoStatus, UUID usuarioLogadoId) {
        Projeto projeto = obterProjetoEValidarDono(id, usuarioLogadoId);
        projeto.setStatus(novoStatus);
        
        projeto = projetoRepository.save(projeto);
        return ProjetoResponse.fromEntity(projeto);
    }

    @Transactional
    public void inativar(UUID id, UUID usuarioLogadoId) {
        Projeto projeto = obterProjetoEValidarDono(id, usuarioLogadoId);
        projeto.setAtivo(false);
        projetoRepository.save(projeto);
    }

    private Projeto obterProjetoEValidarDono(UUID id, UUID usuarioLogadoId) {
        Projeto projeto = projetoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado."));

        if (!projeto.getCriador().getId().equals(usuarioLogadoId)) {
            throw new RegraDeNegocioException("Você não tem permissão para alterar este projeto.");
        }

        return projeto;
    }
}