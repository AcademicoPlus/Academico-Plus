package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.UsuarioHabilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioHabilidadeRepository extends JpaRepository<UsuarioHabilidade, Long> {
    
    // Encontrar por usuário e habilidade
    Optional<UsuarioHabilidade> findByUsuarioIdAndHabilidadeId(Long usuarioId, Long habilidadeId);
    
    // Listar todas as habilidades de um usuário
    List<UsuarioHabilidade> findByUsuarioId(Long usuarioId);
    
    // Listar todos os usuários de uma habilidade
    List<UsuarioHabilidade> findByHabilidadeId(Long habilidadeId);
}