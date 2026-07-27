package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.UsuarioHabilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioHabilidadeRepository extends JpaRepository<UsuarioHabilidade, UUID> {
    
    // Encontrar por usuário e habilidade
    Optional<UsuarioHabilidade> findByUsuarioIdAndHabilidadeId(UUID usuarioId, UUID habilidadeId);
    
    // Listar todas as habilidades de um usuário
    List<UsuarioHabilidade> findByUsuarioId(UUID usuarioId);
    
    // Listar todos os usuários de uma habilidade
    List<UsuarioHabilidade> findByHabilidadeId(UUID habilidadeId);
}