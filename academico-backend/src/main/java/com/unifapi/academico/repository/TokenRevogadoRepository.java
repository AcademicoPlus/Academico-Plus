package com.unifapi.academico.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.unifapi.academico.domain.models.TokenRevogado;

import jakarta.transaction.Transactional;

public interface TokenRevogadoRepository extends JpaRepository<TokenRevogado, String> {
    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRevogado t WHERE t.dataExpiracao <= :agora")
    void deletarExpiradosAte(LocalDateTime agora);
}
