package com.unifapi.academico.repository;

import com.unifapi.academico.domain.models.NivelAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NivelAcessoRepository extends JpaRepository<NivelAcesso, UUID> {
    
    Optional<NivelAcesso> findByNome(String nome);
    
    Optional<NivelAcesso> findById(UUID idNivel);
}