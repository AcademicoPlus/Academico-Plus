package com.unifapi.academico.service;

import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Login - Autenticar usuário
     */
    public Map<String, Object> login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new RuntimeException("Senha incorreta");
        }

        // Gerar token com claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", usuario.getTipo().toString());
        claims.put("id", usuario.getId());
        
        String token = jwtUtil.generateToken(email, claims);

        return Map.of(
            "id", usuario.getId(),
            "email", usuario.getEmail(),
            "nome", usuario.getNome(),
            "tipo", usuario.getTipo(),
            "token", token,
            "expiresIn", 86400000L
        );
    }

    /**
     * Signup - Registrar novo usuário
     */
    public Map<String, Object> signup(String email, String nome, String curso, String senha) {
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
            .tipo(TipoUsuario.ALUNO) // Default
            .ativo(true)
            .build();

        usuario = usuarioRepository.save(usuario);

        String token = gerarToken(usuario);

        return Map.of(
            "id", usuario.getId(),
            "email", usuario.getEmail(),
            "nome", usuario.getNome(),
            "tipo", usuario.getTipo(),
            "token", token,
            "expiresIn", 86400000L
        );
    }

    /**
     * Validar e decodificar token JWT
     */
    public Map<String, Object> me(String token) {
        // Validar e extrair email
        jwtUtil.validateToken(token);
        String email = jwtUtil.extractEmail(token);
        
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return Map.of(
            "id", usuario.getId(),
            "email", usuario.getEmail(),
            "nome", usuario.getNome(),
            "tipo", usuario.getTipo(),
            "curso", usuario.getCurso()
        );
    }

    /**
     * Gerar token JWT
     */
    private String gerarToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", usuario.getTipo().toString());
        claims.put("email", usuario.getEmail());
        return jwtUtil.generateToken(usuario.getEmail(), claims);
    }
}