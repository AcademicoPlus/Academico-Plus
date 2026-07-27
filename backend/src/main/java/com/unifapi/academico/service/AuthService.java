package com.unifapi.academico.service;

import com.unifapi.academico.api.dto.response.AuthResponseDTO;
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.domain.model.RefreshToken;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.RefreshTokenRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Login - Autenticar usuário
     */
    public AuthResponseDTO login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new RuntimeException("Senha incorreta");
        }

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RuntimeException("Usuário inativo");
        }

        String accessToken = gerarToken(usuario);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario.getId());

        return AuthResponseDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .tipo(usuario.getTipo())
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(86400000L) // 15 minutos em milissegundos
                .build();
    }

    @Transactional
    public AuthResponseDTO refreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUsuario)
                .map(usuario -> {
                    String newAccessToken = gerarToken(usuario);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(usuario.getId());

                    return AuthResponseDTO.builder()
                            .id(usuario.getId())
                            .email(usuario.getEmail())
                            .nome(usuario.getNome())
                            .tipo(usuario.getTipo())
                            .token(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .expiresIn(86400000L)
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh Token não encontrado no banco."));
    }

    // Método de Logout (Revoga o Refresh Token)
    @Transactional
    public void logout(UUID usuarioId) {
        refreshTokenService.deleteByUsuarioId(usuarioId);
    }

    /**
     * Signup - Registrar novo usuário
     */
    public AuthResponseDTO signup(String email, String nome, String curso, String senha) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("Email já registrado");
        }

        if (senha.length() < 8) {
            throw new RuntimeException("Senha deve ter no mínimo 8 caracteres");
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .nome(nome)
                .curso(curso)
                .senhaHash(passwordEncoder.encode(senha))
                .tipo(TipoUsuario.ALUNO)
                .ativo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        String token = gerarToken(usuario);

        return AuthResponseDTO.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nome(usuario.getNome())
                .tipo(usuario.getTipo())
                .token(token)
                .expiresIn(86400000L)
                .build();
    }

    /**
     * Obter dados do usuário autenticado a partir do token
     */
    @Transactional(readOnly = true)
    public UserDTO me(String token) {
        jwtUtil.validateToken(token);
        String email = jwtUtil.extractEmail(token);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return UserDTO.from(usuario);
    }

    /**
     * Gerar token JWT com claims padronizadas
     */
    private String gerarToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", usuario.getTipo().toString());
        claims.put("id", usuario.getId().toString()); // Convertido para String para facilitar a leitura no JWT
        return jwtUtil.generateToken(usuario.getEmail(), claims);
    }
}