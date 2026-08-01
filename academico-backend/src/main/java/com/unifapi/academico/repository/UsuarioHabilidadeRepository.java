package com.unifapi.academico.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unifapi.academico.domain.models.UsuarioHabilidade;

public interface UsuarioHabilidadeRepository extends JpaRepository<UsuarioHabilidade, UUID> {

}
