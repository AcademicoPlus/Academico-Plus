package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.RefreshToken;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.RefreshTokenRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // Padrão: 7 dias em ms
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public RefreshToken createRefreshToken(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Busca o token existente do usuário ou instancia um novo se não existir
        RefreshToken refreshToken = refreshTokenRepository.findByUsuario(usuario)
                .orElseGet(() -> RefreshToken.builder()
                        .usuario(usuario)
                        .build());

        // Atualiza o valor do UUID e renova o prazo de expiração
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh Token expirado. Faça login novamente.");
        }
        return token;
    }

    @Transactional
    public int deleteByUsuarioId(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return refreshTokenRepository.deleteByUsuario(usuario);
    }
}