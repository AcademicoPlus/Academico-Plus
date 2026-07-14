package com.unifapi.academico.repository;

import com.unifapi.academico.domain.model.Habilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HabilidadeRepository extends JpaRepository<Habilidade, Long> {
    Optional<Habilidade> findByNome(String nome);
}