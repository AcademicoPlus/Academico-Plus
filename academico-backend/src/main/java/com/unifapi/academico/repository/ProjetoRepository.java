package com.unifapi.academico.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unifapi.academico.domain.models.Projeto;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

}
