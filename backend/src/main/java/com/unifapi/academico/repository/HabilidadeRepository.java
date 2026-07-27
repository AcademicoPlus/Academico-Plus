package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Habilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabilidadeRepository extends JpaRepository<Habilidade, UUID> {
    Optional<Habilidade> findByNome(String nome);
}